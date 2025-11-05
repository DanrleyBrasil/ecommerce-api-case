# Diagrama de Sequência - E-Commerce API

## 📋 Visão Geral

Este documento apresenta os fluxos sequenciais mais críticos da aplicação, demonstrando a interação entre as camadas e as decisões de negócio implementadas.

## 🎯 Fluxos Documentados

1. **Autenticação JWT** - Login de usuário
   2. **Criação de Pedido** - Validação de estoque e criação (crítico)
   3. **Processamento de Pagamento** - Aprovação com locks pessimistas (crítico)

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

## 🛒 Fluxo 2: Criação de Pedido (Crítico)

### Cenário: Usuário cria pedido com validação de estoque

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
            Service->>ProductRepo: findById(productId)
            activate ProductRepo
            ProductRepo->>DB: SELECT * FROM products<br/>WHERE id = ?
            activate DB
            DB-->>ProductRepo: Product entity
            deactivate DB
            ProductRepo-->>Service: Optional<Product>
            deactivate ProductRepo
            
            alt Produto não encontrado
                Service-->>Controller: throw ProductNotFoundException
                Controller-->>User: 404 Not Found
            end
            
            Service->>Service: validateStock(product, quantity)
            
            alt Estoque insuficiente
                Service-->>Controller: throw InsufficientStockException
                Controller-->>User: 400 Bad Request<br/>"Estoque insuficiente"
            end
        end
    end
    
    rect rgb(220, 255, 220)
        Note over Service,DB: FASE 2: CRIAÇÃO DO PEDIDO
        
        Service->>Service: calculateTotalAmount(items)
        
        Service->>OrderRepo: save(Order)<br/>status = PENDENTE
        activate OrderRepo
        OrderRepo->>DB: INSERT INTO orders<br/>INSERT INTO order_items
        activate DB
        DB-->>OrderRepo: Saved entities
        deactivate DB
        OrderRepo-->>Service: Order with items
        deactivate OrderRepo
    end
    
    Service-->>Controller: OrderResponse<br/>(id, status=PENDENTE, totalAmount)
    deactivate Service
    Controller-->>User: 201 Created<br/>{orderId, status, total}
    
    Note over User,DB: ⚠️ ESTOQUE NÃO É RESERVADO<br/>Baixa acontece apenas no PAGAMENTO
```

### Pontos-Chave
- ✅ Validação de **TODOS** os produtos antes de criar
  - ✅ Cálculo de `totalAmount` na criação
  - ⚠️ Estoque é apenas **verificado**, não reservado
  - ✅ Status inicial: `PENDENTE`

### Por que não reservar estoque na criação?

| Abordagem | Prós | Contras |
|-----------|------|---------|
| **Reservar na criação** | Garante disponibilidade | Muitos pedidos abandonados<br/>Estoque "travado" |
| **Baixar no pagamento** ✅ | Maximiza vendas<br/>Sem estoque parado | Race condition no pagamento |

**Decisão**: Baixar no pagamento + **Lock Pessimista** (ver fluxo 3)

---

## 💳 Fluxo 3: Processamento de Pagamento (Crítico + Concorrência)

### Cenário: Pagamento de pedido com atualização de estoque

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'actorTextColor':'#000000', 'labelTextColor':'#000000', 'loopTextColor':'#000000', 'noteTextColor':'#000000', 'activationBorderColor':'#000000', 'signalColor':'#000000', 'signalTextColor':'#000000', 'labelBoxBkgColor':'#ffffff', 'labelBoxBorderColor':'#000000', 'sequenceNumberColor':'#000000'}}}%%
sequenceDiagram
    actor Admin as 👤 ADMIN
    participant Controller as OrderController
    participant OrderService as OrderService
    participant PaymentService as PaymentService
    participant OrderRepo as OrderRepository
    participant ProductRepo as ProductRepository
    participant DB as 🗄️ Database

    Admin->>Controller: POST /orders/{orderId}/payment
    Note over Admin,Controller: Header: Authorization: Bearer <token>
    
    Controller->>Controller: @PreAuthorize("hasRole('ADMIN')")
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
            Controller-->>Admin: 404 Not Found
        end
        
        alt Status diferente de PENDENTE
            PaymentService-->>Controller: throw InvalidOrderStatusException
            Controller-->>Admin: 400 Bad Request<br/>"Pedido já processado"
        end
        
        Note over PaymentService,DB: 🔒 LOCK PESSIMISTA NOS PRODUTOS
        
        loop Para cada item do pedido
            PaymentService->>ProductRepo: findByIdWithLock(productId)
            activate ProductRepo
            ProductRepo->>DB: SELECT * FROM products<br/>WHERE id = ?<br/>FOR UPDATE
            activate DB
            DB-->>ProductRepo: Product entity (LOCKED)
            deactivate DB
            ProductRepo-->>PaymentService: Product
            deactivate ProductRepo
            
            PaymentService->>PaymentService: revalidateStock(product, quantity)
            
            alt Estoque insuficiente (race condition)
                PaymentService->>DB: ROLLBACK
                activate DB
                deactivate DB
                PaymentService-->>Controller: throw InsufficientStockException
                Controller-->>Admin: 409 Conflict<br/>"Estoque esgotado"
            end
            
            PaymentService->>PaymentService: product.stockQuantity -= quantity
            
            PaymentService->>ProductRepo: save(product)
            activate ProductRepo
            ProductRepo->>DB: UPDATE products<br/>SET stock_quantity = ?<br/>WHERE id = ?
            activate DB
            deactivate DB
            deactivate ProductRepo
        end
        
        PaymentService->>PaymentService: order.status = APROVADO<br/>order.paymentDate = now()
        
        PaymentService->>OrderRepo: save(order)
        activate OrderRepo
        OrderRepo->>DB: UPDATE orders<br/>SET status = 'APROVADO'
        activate DB
        DB-->>PaymentService: COMMIT
        deactivate DB
        deactivate OrderRepo
    end
    
    PaymentService-->>Controller: OrderResponse(status=APROVADO)
    deactivate PaymentService
    Controller-->>Admin: 200 OK<br/>{orderId, status=APROVADO}
    
    Note over Admin,DB: 🔓 Locks liberados após COMMIT
```

### Pontos-Chave Críticos

#### 🔒 Lock Pessimista (`FOR UPDATE`)
```sql
SELECT * FROM products 
WHERE id = ? 
FOR UPDATE;
```
- Previne race conditions
  - Outros pagamentos **aguardam** liberação do lock
  - Garante consistência do estoque

#### ⚠️ Tratamento de Race Condition

**Cenário Problemático**:
```
T=0: Pedido A criado (valida estoque = 5 OK)
T=1: Pedido B criado (valida estoque = 5 OK)
T=2: Pagamento A processa (estoque -= 5 = 0)
T=3: Pagamento B tenta processar (estoque = 0)
```

**Solução Implementada**:
1. Lock pessimista no pagamento
   2. **Re-validação** de estoque dentro da transação
   3. Rollback automático se inconsistência detectada

#### 🎯 Isolamento SERIALIZABLE

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
```
- Mais alto nível de isolamento
  - Garante consistência total
  - Trade-off: performance (aceito para pagamentos)

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
  - Race conditions causam vendas impossíveis
  - UX: melhor travar 2s que vender sem estoque

❌ **Argumentos Contra** (mitigados):
- Performance: poucos pagamentos simultâneos (aceitável)
  - Deadlocks: locks curtos e ordenados por productId

---

## 🔄 Fluxo Completo End-to-End

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'primaryColor':'#e1f5ff', 'primaryTextColor':'#000000', 'primaryBorderColor':'#000000', 'lineColor':'#000000', 'secondaryColor':'#ffe1e1', 'tertiaryColor':'#e1ffe1', 'edgeLabelBackground':'#ffffff', 'nodeTextColor':'#000000', 'textColor':'#000000', 'mainBkg':'#ffffff'}}}%%
graph TD
    A[👤 Usuário navega] --> B[🛒 Adiciona produtos ao carrinho]
    B --> C[✅ Cria pedido - POST /orders]
    C --> D{Estoque<br/>disponível?}
    D -->|❌ Não| E[400 Bad Request]
    D -->|✅ Sim| F[201 Created<br/>Status: PENDENTE]
    F --> G[💳 Usuário paga<br/>fora do sistema]
    G --> H[👨‍💼 Admin aprova<br/>POST /orders/id/payment]
    H --> I{Re-valida<br/>estoque<br/>com LOCK}
    I -->|❌ Não| J[409 Conflict<br/>ROLLBACK]
    I -->|✅ Sim| K[✅ Atualiza estoque<br/>Status: APROVADO<br/>COMMIT]
    
    style C fill:#e1f5ff
    style H fill:#ffe1e1
    style I fill:#fff4e1
    style K fill:#e1ffe1
```

---

## 🎓 Lições Aprendidas e Decisões

### 1. **Por que não usar Event Sourcing?**
- ✅ Complexidade desnecessária para o escopo
  - ✅ MySQL + Transações ACID são suficientes
  - ⚠️ Evolução futura: considerar para auditoria

### 2. **Por que ADMIN aprova pagamento?**
- ✅ Simulação simplificada (sem gateway de pagamento)
  - ✅ Permite testar fluxo completo
  - ⚠️ Produção: integrar com Stripe/Mercado Pago

### 3. **Por que não usar filas (RabbitMQ)?**
- ✅ Síncrono é mais simples e adequado ao case
  - ✅ Filas para casos de uso assíncronos (email, notificações)
  - ⚠️ Evolução: ver `evolucao-microservices.md`

---

## 📈 Métricas de Complexidade

| Fluxo | Atores | Camadas | Validações | Locks | Transações |
|-------|--------|---------|------------|-------|------------|
| Login | 1 | 4 | 2 | 0 | 0 |
| Criar Pedido | 1 | 4 | N×2 | 0 | 1 |
| Processar Pagamento | 1 | 5 | N×2 | N | 1 |

**N** = número de itens no pedido

---

## 🔗 Referências

- [Pessimistic Locking - Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.locking)
  - [Transaction Isolation Levels](https://en.wikipedia.org/wiki/Isolation_(database_systems))
  - [Patterns of Enterprise Application Architecture - Martin Fowler](https://martinfowler.com/eaaCatalog/)

---

**Última Atualização**: 04/11/2025  
**Versão**: 1.0  
**Responsável**: Danrley Brasil dos Santos