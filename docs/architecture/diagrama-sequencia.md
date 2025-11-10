# Diagrama de Sequência - E-Commerce API

## 📋 Visão Geral

Este documento apresenta os fluxos sequenciais críticos da aplicação, demonstrando a estratégia híbrida de controle de estoque implementada: **Reserva Temporária + Lock Pessimista**.

## 🎯 Fluxos Documentados

1. **Autenticação JWT** - Login de usuário
2. **Criação de Pedido** - Reserva temporária de estoque (TTL 10min)
3. **Processamento de Pagamento** - Lock pessimista + baixa definitiva
4. **Expiração Automática** - Job scheduled que libera reservas

---

## 🔐 Fluxo 1: Autenticação JWT

### Cenário: Login de usuário

```mermaid
%%{init: {'theme':'dark'}}%%
sequenceDiagram
    actor User as 👤 Usuário
    participant Controller as AuthController
    participant Service as AuthService
    participant Repository as UserRepository
    participant Security as JwtService
    participant DB as 🗄️ Database

    User->>Controller: POST /auth/login<br/>{email, password}
    
    Controller->>Service: authenticate(LoginRequest)
    
    Service->>Repository: findByEmail(email)
    Repository->>DB: SELECT * FROM users<br/>WHERE email = ?
    DB-->>Repository: User entity
    Repository-->>Service: Optional<User>
    
    alt Usuário não encontrado
        Service-->>Controller: throw UserNotFoundException
        Controller-->>User: 404 Not Found
    end
    
    Service->>Service: validatePassword(rawPassword, encodedPassword)
    
    alt Senha inválida
        Service-->>Controller: throw InvalidCredentialsException
        Controller-->>User: 401 Unauthorized
    end
    
    Service->>Security: generateToken(user)
    Security->>Security: createJwtToken(claims, expiration)
    Security-->>Service: JWT Token (String)
    
    Service-->>Controller: AuthResponse(token, user)
    Controller-->>User: 200 OK<br/>{token, user}
    
    Note over User,DB: Token válido por 24h<br/>Renovação requer novo login
```

### Pontos-Chave
- ✅ Validação em duas etapas (usuário existe + senha correta)
- ✅ Token JWT com expiração de 24h
- ✅ Tratamento de erros específicos (404 vs 401)

---

## 🛒 Fluxo 2: Criação de Pedido com Reserva Temporária

### Cenário: Usuário cria pedido e sistema reserva estoque por 10 minutos

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'actorTextColor':'#000000', 'labelTextColor':'#000000', 'loopTextColor':'#000000', 'noteTextColor':'#000000', 'activationBorderColor':'#000000', 'signalColor':'#000000', 'signalTextColor':'#000000', 'labelBoxBkgColor':'#ffffff', 'labelBoxBorderColor':'#000000', 'sequenceNumberColor':'#000000'}}}%%
sequenceDiagram
    actor User as 👤 USER
    participant Controller as OrderController
    participant Service as OrderService
    participant ProductRepo as ProductRepository
    participant OrderRepo as OrderRepository
    participant DB as 🗄️ Database

    User->>Controller: POST /orders<br/>{items: [{productId, quantity}]}
    Note over User,Controller: Header: Authorization: Bearer <token>
    
    Controller->>Controller: @PreAuthorize("hasRole('USER')")
    Controller->>Service: createOrder(userId, CreateOrderRequest)
    activate Service
    
    rect rgb(255, 248, 220)
        Note over Service,DB: FASE 1: VALIDAÇÃO DE ESTOQUE
        
        loop Para cada item do pedido
            Service->>ProductRepo: findByIdWithLock(productId)
            activate ProductRepo
            ProductRepo->>DB: SELECT * FROM products<br/>WHERE id = ?<br/>FOR UPDATE
            activate DB
            DB-->>ProductRepo: Product entity (LOCKED)
            deactivate DB
            ProductRepo-->>Service: Product
            deactivate ProductRepo
            
            alt Produto não encontrado
                Service-->>Controller: throw ProductNotFoundException
                Controller-->>User: 404 Not Found
            end
            
            Service->>Service: validateAvailableStock()<br/>(stockQuantity - reservedQuantity)
            
            alt Estoque disponível insuficiente
                Service-->>Controller: throw InsufficientStockException
                Controller-->>User: 400 Bad Request<br/>"Estoque insuficiente"
            end
            
            Service->>Service: product.reservedQuantity += quantity
            Service->>ProductRepo: save(product)
            ProductRepo->>DB: UPDATE products<br/>SET reserved_quantity = ?
            DB-->>ProductRepo: COMMIT
        end
    end
    
    rect rgb(220, 255, 220)
        Note over Service,DB: FASE 2: CRIAÇÃO DO PEDIDO
        
        Service->>Service: calculateTotalAmount(items)
        Service->>Service: setExpirationTime(now + 10 minutes)
        
        Service->>OrderRepo: save(Order)<br/>status = PENDENTE<br/>expiresAt = now + 10min
        activate OrderRepo
        OrderRepo->>DB: INSERT INTO orders<br/>INSERT INTO order_items
        activate DB
        DB-->>OrderRepo: Saved entities
        deactivate DB
        OrderRepo-->>Service: Order with items
        deactivate OrderRepo
    end
    
    Service-->>Controller: OrderResponse<br/>(id, status=PENDENTE, expiresAt)
    deactivate Service
    Controller-->>User: 201 Created<br/>{orderId, status, total, expiresAt}
    
    Note over User,DB: ⏰ Cliente tem 10 minutos para pagar<br/>🔒 Estoque reservado (não baixado ainda)<br/>📊 reservedQuantity incrementado
```

### Pontos-Chave
- ✅ **Lock Pessimista** (`FOR UPDATE`) durante validação para evitar race condition
- ✅ Validação de estoque **disponível**: `stockQuantity - reservedQuantity`
- ✅ **Reserva temporária**: incrementa `reservedQuantity` sem baixar `stockQuantity`
- ✅ **TTL de 10 minutos**: campo `expiresAt` no pedido
- ✅ Status inicial: `PENDENTE`
- ⚠️ Estoque NÃO é baixado ainda (só reservado)

### Por que Reserva Temporária?

| Abordagem | Prós | Contras |
|-----------|------|---------|
| **Baixar na criação** | Simples | Muitos abandonos = estoque travado |
| **Baixar só no pagamento** | Máxima disponibilidade | Race condition severa |
| **Reserva + Lock (escolhida)** ✅ | UX + Consistência | Complexidade adicional |

---

## 💳 Fluxo 3: Processamento de Pagamento (Automático)

### Cenário: Sistema processa pagamento com lock pessimista e baixa definitiva

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'actorTextColor':'#000000', 'labelTextColor':'#000000', 'loopTextColor':'#000000', 'noteTextColor':'#000000', 'activationBorderColor':'#000000', 'signalColor':'#000000', 'signalTextColor':'#000000', 'labelBoxBkgColor':'#ffffff', 'labelBoxBorderColor':'#000000', 'sequenceNumberColor':'#000000'}}}%%
sequenceDiagram
    actor System as 🤖 Sistema/Gateway
    participant Controller as OrderController
    participant PaymentService as PaymentService
    participant OrderRepo as OrderRepository
    participant ProductRepo as ProductRepository
    participant DB as 🗄️ Database

    System->>Controller: POST /orders/{orderId}/payment
    Note over System,Controller: Simulação: pode ser chamado por webhook<br/>de gateway de pagamento (Stripe/MP)
    
    Controller->>PaymentService: processPayment(orderId)
    activate PaymentService
    
    rect rgb(255, 220, 220)
        Note over PaymentService,DB: ⚠️ TRANSAÇÃO ATÔMICA<br/>@Transactional(isolation = SERIALIZABLE)
        
        PaymentService->>OrderRepo: findById(orderId)
        activate OrderRepo
        OrderRepo->>DB: SELECT * FROM orders<br/>WHERE id = ?
        activate DB
        DB-->>OrderRepo: Order entity
        deactivate DB
        OrderRepo-->>PaymentService: Optional<Order>
        deactivate OrderRepo
        
        alt Pedido não encontrado
            PaymentService-->>Controller: throw OrderNotFoundException
            Controller-->>System: 404 Not Found
        end
        
        alt Status diferente de PENDENTE
            PaymentService-->>Controller: throw InvalidOrderStatusException
            Controller-->>System: 400 Bad Request<br/>"Pedido já processado ou expirado"
        end
        
        alt Pedido expirado (> 10min)
            PaymentService-->>Controller: throw OrderExpiredException
            Controller-->>System: 400 Bad Request<br/>"Tempo de pagamento expirado"
        end
        
        Note over PaymentService,DB: 🔒 LOCK PESSIMISTA + BAIXA DE ESTOQUE
        
        loop Para cada item do pedido
            PaymentService->>ProductRepo: findByIdWithLock(productId)
            activate ProductRepo
            ProductRepo->>DB: SELECT * FROM products<br/>WHERE id = ?<br/>FOR UPDATE
            activate DB
            DB-->>ProductRepo: Product entity (LOCKED)
            deactivate DB
            ProductRepo-->>PaymentService: Product
            deactivate ProductRepo
            
            PaymentService->>PaymentService: revalidateStock()<br/>(stockQuantity >= quantity)
            
            alt Estoque insuficiente (edge case)
                PaymentService->>DB: ROLLBACK
                activate DB
                Note over DB: Libera locks e desfaz mudanças
                deactivate DB
                PaymentService-->>Controller: throw InsufficientStockException
                Controller-->>System: 409 Conflict<br/>"Estoque esgotado"
            end
            
            PaymentService->>PaymentService: product.stockQuantity -= quantity<br/>product.reservedQuantity -= quantity
            
            PaymentService->>ProductRepo: save(product)
            activate ProductRepo
            ProductRepo->>DB: UPDATE products<br/>SET stock_quantity = stock_quantity - ?<br/>SET reserved_quantity = reserved_quantity - ?<br/>WHERE id = ?
            activate DB
            deactivate DB
            deactivate ProductRepo
        end
        
        PaymentService->>PaymentService: order.status = APROVADO<br/>order.paymentDate = now()
        
        PaymentService->>OrderRepo: save(order)
        activate OrderRepo
        OrderRepo->>DB: UPDATE orders<br/>SET status = 'APROVADO'<br/>SET payment_date = NOW()
        activate DB
        DB-->>PaymentService: COMMIT
        deactivate DB
        deactivate OrderRepo
    end
    
    PaymentService-->>Controller: OrderResponse(status=APROVADO)
    deactivate PaymentService
    Controller-->>System: 200 OK<br/>{orderId, status=APROVADO}
    
    Note over System,DB: 🔓 Locks liberados após COMMIT<br/>✅ Estoque baixado definitivamente<br/>✅ Reserva liberada
```

### Pontos-Chave Críticos

#### 🔒 Lock Pessimista (`FOR UPDATE`)
```sql
SELECT * FROM products 
WHERE id = ? 
FOR UPDATE;
```
- Previne race conditions durante o pagamento
- Outros pagamentos **aguardam** liberação do lock
- Garante consistência absoluta do estoque

#### ⚠️ Re-validação de Estoque

**Por que re-validar se já reservamos?**

Cenário Edge Case:
```
T=0:  Pedido A criado (reserva 5 unidades, expira T+10min)
T=9:  Job de expiração não rodou ainda
T=9:  Pedido B criado (vê 0 disponível, falha) ✅
T=10: Pedido A tenta pagar (já expirou!)
```

**Solução Implementada**:
1. Lock pessimista no pagamento
2. **Re-validação** de estoque dentro da transação
3. Verificação de expiração do pedido
4. Rollback automático se inconsistência detectada

#### 🎯 Isolamento SERIALIZABLE

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
```
- Mais alto nível de isolamento
- Garante consistência total
- Trade-off: performance (aceitável para pagamentos críticos)

---

## ⏰ Fluxo 4: Expiração Automática de Pedidos

### Cenário: Job scheduled libera reservas de pedidos expirados

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'actorTextColor':'#000000', 'labelTextColor':'#000000', 'loopTextColor':'#000000', 'noteTextColor':'#000000', 'activationBorderColor':'#000000', 'signalColor':'#000000', 'signalTextColor':'#000000', 'labelBoxBkgColor':'#ffffff', 'labelBoxBorderColor':'#000000', 'sequenceNumberColor':'#000000'}}}%%
sequenceDiagram
    participant Scheduler as OrderExpirationScheduler
    participant OrderRepo as OrderRepository
    participant ProductRepo as ProductRepository
    participant DB as 🗄️ Database

    Note over Scheduler: ⏰ Executa a cada 1 minuto<br/>@Scheduled(fixedRate = 60000)
    
    activate Scheduler
    Scheduler->>OrderRepo: findExpiredPendingOrders()
    activate OrderRepo
    OrderRepo->>DB: SELECT * FROM orders<br/>WHERE status = 'PENDENTE'<br/>AND expires_at < NOW()
    activate DB
    DB-->>OrderRepo: List<Order>
    deactivate DB
    OrderRepo-->>Scheduler: List de pedidos expirados
    deactivate OrderRepo
    
    alt Sem pedidos expirados
        Scheduler->>Scheduler: Log: "Nenhum pedido expirado"
        Note over Scheduler: Aguarda próxima execução
    end
    
    loop Para cada pedido expirado
        rect rgb(255, 240, 240)
            Note over Scheduler,DB: 🔄 TRANSAÇÃO ATÔMICA POR PEDIDO
            
            Scheduler->>Scheduler: order.status = EXPIRADO
            
            loop Para cada item do pedido
                Scheduler->>ProductRepo: findById(productId)
                activate ProductRepo
                ProductRepo->>DB: SELECT * FROM products<br/>WHERE id = ?
                activate DB
                DB-->>ProductRepo: Product entity
                deactivate DB
                ProductRepo-->>Scheduler: Product
                deactivate ProductRepo
                
                Scheduler->>Scheduler: product.reservedQuantity -= quantity
                
                Scheduler->>ProductRepo: save(product)
                activate ProductRepo
                ProductRepo->>DB: UPDATE products<br/>SET reserved_quantity = reserved_quantity - ?<br/>WHERE id = ?
                activate DB
                deactivate DB
                deactivate ProductRepo
            end
            
            Scheduler->>OrderRepo: save(order)
            activate OrderRepo
            OrderRepo->>DB: UPDATE orders<br/>SET status = 'EXPIRADO'
            activate DB
            DB-->>Scheduler: COMMIT
            deactivate DB
            deactivate OrderRepo
            
            Note over Scheduler: ✅ Reserva liberada<br/>📊 Estoque disponível novamente
        end
    end
    
    Scheduler->>Scheduler: Log: "X pedidos expirados processados"
    deactivate Scheduler
```

### Pontos-Chave
- ✅ Execução **automática** a cada 1 minuto
- ✅ Busca pedidos com `status = PENDENTE` e `expiresAt < NOW()`
- ✅ **Libera reservas**: decrementa `reservedQuantity`
- ✅ Atualiza status para `EXPIRADO`
- ✅ Transação atômica por pedido (se um falhar, outros continuam)
- ⚠️ Não usa lock pessimista (performance > consistência neste caso)

### Por que Job Scheduled?

| Alternativa | Contras |
|-------------|---------|
| **Manual** | Requer ação humana |
| **Trigger SQL** | Complexidade de debug |
| **Job Scheduled** ✅ | Simples, testável, monitorável |

---

## 🔄 Fluxo Completo End-to-End

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'primaryColor':'#e1f5ff', 'primaryTextColor':'#000000', 'primaryBorderColor':'#000000', 'lineColor':'#000000', 'secondaryColor':'#ffe1e1', 'tertiaryColor':'#e1ffe1', 'edgeLabelBackground':'#ffffff', 'nodeTextColor':'#000000', 'textColor':'#000000', 'mainBkg':'#ffffff'}}}%%
graph TD
    A[👤 Usuário navega] --> B[🛒 Adiciona produtos ao carrinho]
    B --> C[✅ Cria pedido - POST /orders]
    C --> D{Estoque<br/>disponível?}
    D -->|❌ Não| E[400 Bad Request]
    D -->|✅ Sim| F[201 Created<br/>Status: PENDENTE<br/>🔒 Reserva por 10min]
    F --> G{Usuário<br/>paga em<br/>10min?}
    G -->|❌ Não| H[⏰ Job expira pedido<br/>🔓 Libera reserva<br/>Status: EXPIRADO]
    G -->|✅ Sim| I[💳 POST /orders/id/payment]
    I --> J{Re-valida<br/>estoque<br/>com LOCK}
    J -->|❌ Não| K[409 Conflict<br/>ROLLBACK]
    J -->|✅ Sim| L[✅ Baixa estoque<br/>🔓 Libera reserva<br/>Status: APROVADO<br/>COMMIT]
    
    style C fill:#e1f5ff
    style F fill:#fff4e1
    style I fill:#ffe1e1
    style L fill:#e1ffe1
    style H fill:#ffeeee
```

---

## 📊 Comparação de Abordagens

### Lock Pessimista vs Lock Otimista

| Aspecto | Pessimista ✅ | Otimista |
|---------|--------------|----------|
| **Quando usar** | Alta concorrência<br/>Dados críticos | Baixa concorrência<br/>Leitura > Escrita |
| **Mecanismo** | `FOR UPDATE` | `@Version` |
| **Performance** | Mais lento | Mais rápido |
| **Consistência** | Garantida | Retry necessário |
| **Nosso caso** | **ESCOLHIDO**<br/>Estoque é crítico | Não adequado |

### Por que Lock Pessimista para Estoque?

✅ **Argumentos a Favor**:
- Estoque é um recurso finito e crítico
- Race conditions causam vendas impossíveis (overselling)
- UX: melhor travar 2s que vender sem estoque

❌ **Argumentos Contra** (mitigados):
- Performance: poucos pagamentos simultâneos no mesmo produto (aceitável)
- Deadlocks: locks curtos e ordenados por productId

---

## 🎓 Lições Aprendidas e Decisões

### 1. Por que Reserva Temporária + Lock Pessimista?

**Problema sem reserva**:
```
Cliente A: adiciona 5 produtos ao carrinho
Cliente B: adiciona 3 produtos ao carrinho
Estoque: 5 unidades

Se B paga primeiro → OK (sobra 2)
Se A tenta pagar → FALHA ❌ (frustrante!)
```

**Solução com reserva**:
- Cliente sabe disponibilidade real no checkout
- Pode ajustar quantidade antes de pagar
  - ⚠️ Seria interessante criar uma rotina de verificação do estoque dos itens do pedido talvez em rotinas como "Calculo de Entrega", ou algum botão/rotina para atualizar a quantidade disponível dentro do carrinho.
- Reserva expira se não pagar (estoque volta)

### 2. Por que NÃO usar Event Sourcing?

- ✅ Complexidade desnecessária para o escopo
- ✅ MySQL + Transações ACID são suficientes
- ⚠️ Evolução futura: considerar para auditoria completa

### 3. Por que Processamento Automático de Pagamento?

**Implementação atual**: Endpoint simulado `/orders/{id}/payment`

**Produção real**:
- Integração com gateway (Stripe/Mercado Pago/PagSeguro)
- Webhook recebe confirmação de pagamento
- Sistema processa automaticamente
- ⚠️ Ver ADR-002 para detalhes de integração futura

### 4. Por que NÃO usar filas (RabbitMQ)?

- ✅ Síncrono é mais simples e adequado ao case
- ✅ Filas para casos de uso assíncronos (email, notificações)

---

## 📈 Métricas de Complexidade

| Fluxo | Atores | Camadas | Validações | Locks | Transações |
|-------|--------|---------|-----------|-------|------------|
| Login | 1 | 4 | 2 | 0 | 0 |
| Criar Pedido | 1 | 4 | N×2 | N | 1 |
| Processar Pagamento | 1 | 4 | N×2 | N | 1 |
| Expirar Pedidos | 0 (scheduled) | 3 | 0 | 0 | N |

**N** = número de itens no pedido

---

## 🔗 Referências

- [ADR-003: Locks Pessimistas + Reserva Temporária](../decisions/ADR-003-locks-pessimistas.md)

---

**Última Atualização**: 10/11/2025  
**Versão**: 2.0  
**Responsável**: Danrley Brasil dos Santos