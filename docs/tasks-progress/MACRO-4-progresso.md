# MACRO 4 - Banco de Dados - Progresso Completo

## 📊 Status: ✅ 100% CONCLUÍDO

**Data de Conclusão**: 07/11/2025  
**Tempo Total**: ~2.5 horas  
**Responsável**: Danrley Brasil dos Santos

---

## 🎯 Objetivo do MACRO 4

Criar dump do banco de dados com estrutura completa e dados de teste realistas, preparando a base para desenvolvimento da API.

---

## ✅ Entregas Realizadas

### 📄 **Arquivos Criados**

1. **database/dump.sql** (22 KB) - **ATUALIZADO v1.1**
    - Estrutura completa de 10 tabelas
    - Dados de teste (11 usuários, 12 produtos, 8 pedidos)
    - Queries de validação integradas
    - Histórico de preços (5 registros)
    - **NOVO**: Campos de controle de reserva (`reserved_quantity`, `reserved_until`)

2. **database/README.md** (11 KB)
    - Instruções de importação (Docker + manual)
    - Credenciais de teste formatadas
    - Queries úteis para validação e testes
    - Troubleshooting completo

3. **docs/decisions/ADR-004-auditoria-seletiva.md** (13 KB)
    - Decisão sobre auditoria seletiva
    - Justificativa: apenas `product_price_history`
    - **NOVO**: Decisão complementar sobre escopo de CRUD
    - Documentação de evolução futura

4. **docs/architecture/diagrama-ER-database.md** (13 KB) - **ATUALIZADO v1.1**
    - Diagrama Mermaid ER completo
    - Descrição detalhada dos 7 relacionamentos
    - Índices e constraints documentados
    - Análise de normalização (3NF)
    - **NOVO**: Documentação de controle de reserva temporária

---

## 🗄️ Estrutura do Banco de Dados

### **10 Tabelas Criadas**

#### **Domínio: Autenticação (RBAC)**
- `users` - 11 registros (1 ADMIN + 10 USERS)
- `roles` - 2 registros (ADMIN, USER)
- `user_roles` - 11 associações (N:N)

#### **Domínio: Catálogo**
- `categories` - 5 registros (PERIFERICOS, COMPONENTES, MONITORES, ARMAZENAMENTO, ACESSORIOS)
- `suppliers` - 5 registros (Logitech, AMD, Corsair, Kingston, LG)
- `products` - 12 registros com metadata JSON
    - **NOVO**: Campo `reserved_quantity` para controle de reserva

#### **Domínio: Pedidos**
- `orders` - 8 registros (5 APROVADOS + 3 PENDENTES)
    - **NOVO**: Campo `reserved_until` para TTL de reserva
    - **NOVO**: Status `EXPIRED` para pedidos com reserva expirada
- `order_items` - ~15 registros

#### **Domínio: Auditoria**
- `product_price_history` - 5 registros (mudanças últimos 30 dias)

---

## 🔑 Credenciais de Teste

### **Admin**
- **Email**: admin@ecommerce.com
- **Senha**: Admin@123
- **Role**: ADMIN

### **Usuários (10)**
| Email | Senha | Nome |
|-------|-------|------|
| user1@test.com | User@123 | João Silva |
| user2@test.com | User@123 | Maria Santos |
| user3@test.com | User@123 | Pedro Oliveira |
| user4@test.com | User@123 | Ana Costa |
| user5@test.com | User@123 | Lucas Almeida |
| user6@test.com | User@123 | Carla Ferreira |
| user7@test.com | User@123 | Rafael Souza |
| user8@test.com | User@123 | Fernanda Lima |
| user9@test.com | User@123 | Gustavo Rocha |
| user10@test.com | User@123 | Juliana Martins |

---

## 🎯 Decisões Arquiteturais Importantes

### **1. Auditoria Seletiva**

**Implementado**:
- ✅ `product_price_history` - Histórico de mudanças de preço

**NÃO Implementado** (mas documentado):
- ⚠️ `order_status_history` - Mudanças sistêmicas, timestamps suficientes
- ⚠️ `product_stock_history` - Alto volume, order_items já rastreia vendas

**Justificativa**: Pragmatismo. Implementar apenas onde há valor analítico real sem over-engineering.

**Documento**: ADR-004

---

### **2. Escopo de CRUD**

**Implementar AGORA**:
- ✅ CRUD completo de **Produtos** (requisito obrigatório)
- ✅ GET /categories (listagem read-only)

**NÃO Implementar** (evolução futura):
- ⚠️ CRUD de Suppliers
- ⚠️ CRUD completo de Categories

**Justificativa**:
- `supplier_id` é **NULLABLE** em produtos (opcional)
- Categories são 5 fixas (dados estáticos)
- Economiza ~3.5h de desenvolvimento
- Demonstra normalização sem over-engineering

**Documento**: ADR-004 (seção complementar)

---

### **3. Normalização**

**Nível**: 3NF (Terceira Forma Normal)

**Características**:
- ✅ Suppliers separado de Products
- ✅ Categories separado de Products
- ✅ Roles separado de Users (N:N via user_roles)
- ✅ Metadata JSON para especificações flexíveis

**Exceção**: `products.metadata` (JSON) - violação intencional da 1NF para flexibilidade

---

### **4. Relacionamento N:N (RBAC)**

**Decisão**: Usar tabela associativa `user_roles`

**Alternativa Rejeitada**: Enum simples na tabela users

**Justificativa**:
- Permite múltiplas roles por usuário (ex: ADMIN + USER)
- Demonstra conhecimento de @ManyToMany no JPA
- Preparado para sistema de permissões granulares

---

### **5. Controle de Estoque com Reserva Temporária** ⭐ NOVO

**Implementado**:
- ✅ Campo `reserved_quantity` INT NOT NULL DEFAULT 0 em products
- ✅ Campo `reserved_until` TIMESTAMP NULL em orders
- ✅ Status `EXPIRED` adicionado ao CHECK constraint de orders
- ✅ Índice `idx_orders_reserved_until` para job de expiração

**Justificativa**: Implementação da estratégia híbrida definida na **ADR-003**

**Estratégia em 3 Fases**:

1. **Checkout (Reserva Temporária)**
    - Incrementa `product.reserved_quantity`
    - Define `order.reserved_until = NOW() + 10 MINUTE`
    - Status do pedido: `PENDENTE`
    - Valida disponibilidade: `stock_quantity - reserved_quantity >= requested`

2. **Pagamento (Lock Pessimista + Baixa Definitiva)**
    - `SELECT FOR UPDATE` nos produtos (lock pessimista)
    - Re-valida estoque (previne race condition)
    - Decrementa `product.stock_quantity`
    - Decrementa `product.reserved_quantity`
    - Muda status para `APROVADO`
    - Define `order.payment_date`

3. **Job Scheduled (Expiração Automática)**
    - Executa a cada 1 minuto
    - Busca orders onde `status = PENDENTE AND reserved_until < NOW()`
    - Para cada pedido expirado:
        - Devolve estoque: `reserved_quantity -= quantity`
        - Muda status para `EXPIRED`

**Cálculo de Disponibilidade**:
```sql
-- Estoque disponível = estoque real - estoque reservado
disponivel = stock_quantity - reserved_quantity
```

**Exemplo Prático**:
```
Estado Inicial:
  stock_quantity = 10
  reserved_quantity = 0
  disponível = 10

Cliente A inicia checkout (3 unidades):
  reserved_quantity = 3
  disponível = 7

Cliente B inicia checkout (4 unidades):
  reserved_quantity = 7
  disponível = 3

Cliente A paga (aprovado):
  stock_quantity = 7
  reserved_quantity = 4
  disponível = 3

Cliente B desiste (TTL expira após 10min):
  reserved_quantity = 0
  disponível = 7
```

**Vantagens**:
- ✅ Cliente sabe disponibilidade real no checkout
- ✅ Pode ajustar quantidade antes de pagar
- ✅ Reserva garante produto durante pagamento (TTL 10min)
- ✅ Expira automaticamente se abandonar (sem estoque "travado")
- ✅ Lock pessimista garante consistência na hora do pagamento
- ✅ Previne race conditions e overselling

**Trade-offs Aceitos**:
- ⚠️ Complexidade adicional (job de expiração)
- ⚠️ Estoque temporariamente "travado" (máximo 10min)
- **Mitigação**: TTL curto + job eficiente a cada 1min

**Documento**: ADR-003 - Locks Pessimistas + Reserva Temporária

---

## 🐳 Processo de Importação

### **Configuração Docker Compose**

```yaml
services:
  mysql:
    image: mysql:8.0
    volumes:
      - ./database/dump.sql:/docker-entrypoint-initdb.d/01-init.sql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: ecommerce
    ports:
      - "3306:3306"
```

### **Comandos Executados**

```powershell
# 1. Limpar ambiente
docker-compose down -v

# 2. Subir containers (importação automática)
docker-compose up -d

# 3. Aguardar inicialização
# (30-60 segundos)

# 4. Validar importação
docker-compose exec mysql mysql -uroot -proot123 -e "USE ecommerce; SHOW TABLES;"
```

---

## ✅ Validações Realizadas

### **1. Tabelas Criadas**
```sql
SHOW TABLES;
```
**Resultado**: ✅ 10 tabelas

---

### **2. Registros Inseridos**
```sql
SELECT COUNT(*) FROM users;      -- 11 ✅
SELECT COUNT(*) FROM products;   -- 12 ✅
SELECT COUNT(*) FROM orders;     -- 8 ✅
```

---

### **3. Consistência de Valores**
```sql
SELECT 
    o.id,
    o.total_amount as declared,
    SUM(oi.subtotal) as calculated,
    CASE 
        WHEN ABS(o.total_amount - SUM(oi.subtotal)) < 0.01 THEN 'OK'
        ELSE 'ERRO'
    END as status
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id;
```
**Resultado**: ✅ Todos OK (diferenças < 0.01)

---

### **4. Credencial Admin**
```sql
SELECT id, name, email FROM users WHERE email = 'admin@ecommerce.com';
```
**Resultado**: ✅ id=1, name=Administrador

---

### **5. Validação de Controle de Reserva** ⭐ NOVO

```sql
-- Verificar campos de reserva em products
SELECT 
    id, 
    name, 
    stock_quantity, 
    reserved_quantity,
    (stock_quantity - reserved_quantity) as disponivel
FROM products 
LIMIT 3;

-- Verificar pedidos pendentes com TTL
SELECT 
    id,
    user_id,
    status,
    reserved_until,
    TIMESTAMPDIFF(MINUTE, NOW(), reserved_until) as minutos_restantes
FROM orders 
WHERE status = 'PENDENTE';
```
**Resultado**: ✅ Campos criados corretamente, pedidos PENDENTES com TTL de 10min

---

## 📊 Estatísticas Finais

| Métrica | Valor |
|---------|-------|
| **Tabelas** | 10 |
| **Usuários** | 11 (1 ADMIN + 10 USERS) |
| **Roles** | 2 (ADMIN, USER) |
| **Categorias** | 5 |
| **Fornecedores** | 5 |
| **Produtos** | 12 (com metadata JSON + controle reserva) |
| **Pedidos** | 8 (5 aprovados + 3 pendentes com TTL) |
| **Order Items** | ~15 |
| **Histórico Preços** | 5 |
| **Total Registros** | ~130 |
| **Linhas de SQL** | ~520 |
| **Tamanho dump.sql** | 22 KB |

---

## 🔍 Destaques Técnicos

### **1. BCrypt nas Senhas**
- ✅ Todas as senhas armazenadas com hash BCrypt
- ✅ Compatível com Spring Security
- ✅ Senhas em texto claro apenas na documentação

### **2. Metadata JSON**
```json
{
  "brand": "Logitech",
  "model": "G203",
  "dpi": 8000,
  "warranty_months": 12
}
```
- ✅ Especificações flexíveis por produto
- ✅ MySQL 8.0 suporta queries JSON
- ✅ Evita schema rígido (EAV)

### **3. Datas Realistas**
- ✅ Pedidos aprovados: últimos 30 dias
- ✅ Pedidos pendentes: hoje com TTL de 10min
- ✅ Histórico de preços: últimos 30 dias
- ✅ Uso de `DATE_SUB(NOW(), INTERVAL X DAY)` e `DATE_ADD(NOW(), INTERVAL X MINUTE)`

### **4. Índices Otimizados**
```sql
-- Relatórios
INDEX idx_orders_user_id (user_id)
INDEX idx_orders_status (status)
INDEX idx_orders_date (order_date)
INDEX idx_orders_reserved_until (reserved_until) ← NOVO

-- Busca de produtos
INDEX idx_products_category (category_id)
INDEX idx_products_sku (sku)
```

### **5. Controle de Reserva** ⭐ NOVO
- ✅ Campo `reserved_quantity` em products (DEFAULT 0)
- ✅ Campo `reserved_until` em orders (NULL ou timestamp)
- ✅ Status `EXPIRED` no CHECK constraint
- ✅ Índice para job de expiração
- ✅ Pedidos PENDENTES inicializados com TTL de 10min

---

## 🎓 Aprendizados e Decisões

### **O que funcionou bem** ✅

1. **Planejamento antes da execução**
    - Definir estrutura completa antes de criar dump
    - Evitou retrabalho

2. **Auditoria seletiva**
    - Implementar apenas `product_price_history`
    - Demonstrou pragmatismo técnico

3. **Suppliers opcional**
    - Normalização sem over-engineering
    - Economizou ~2h de desenvolvimento

4. **Dados realistas**
    - Produtos inspirados em Kabum/Terabyte
    - Facilitará testes futuros

5. **Controle de reserva desde o banco** ⭐ NOVO
    - Campos preparados para implementação da ADR-003
    - Pedidos PENDENTES já criados com TTL realista
    - Facilita desenvolvimento do MACRO 8

### **Desafios enfrentados** ⚠️

1. **PowerShell e redirecionamento**
    - Problema: `<` não funciona no PowerShell
    - Solução: `Get-Content | docker-compose exec`

2. **Escopo de CRUD**
    - Debate: Implementar CRUD de suppliers/categories?
    - Decisão: Apenas produtos (requisito) + categories read-only

3. **Volume de auditoria**
    - Problema: `product_stock_history` seria milhões de registros
    - Decisão: Não implementar, `order_items` já rastreia vendas

4. **Controle de estoque complexo** ⭐ NOVO
    - Debate: Lock simples vs Reserva temporária
    - Decisão: Estratégia híbrida (ADR-003) para melhor UX

---

## 🔄 Próximos Passos (MACRO 5)

**MACRO 5: Desenvolvimento - Camada de Domínio**

Entregas planejadas:
- [ ] Criar `BaseEntity` (auditoria)
- [ ] Criar enums (UserRole, OrderStatus com EXPIRED, ProductCategory)
- [ ] Criar entidade `User` + `UserRepository`
- [ ] Criar entidade `Product` + `ProductRepository` (com `reserved_quantity`)
- [ ] Criar entidade `Order` + `OrderRepository` (com `reserved_until`)
- [ ] Criar entidade `OrderItem` + `OrderItemRepository`
- [ ] Criar entidades auxiliares (Category, Supplier, Role)
- [ ] Testes de persistência básicos

**Tempo estimado**: 3-4 horas (ajustado pela complexidade adicional)

---

## 📚 Documentação Relacionada

- **ADR-001**: Arquitetura Modular Monolítica
- **ADR-002**: JWT para Autenticação
- **ADR-003**: Locks Pessimistas + Reserva Temporária ⭐
- **ADR-004**: Auditoria Seletiva + Escopo de CRUD
- **diagrama-classes.md**: Modelo de domínio
- **diagrama-sequencia.md**: Fluxos críticos
- **diagrama-ER-database.md**: Modelo físico do banco (v1.1)

---

## ✅ Checklist Final MACRO 4

```
MACRO 4: Banco de Dados
═══════════════════════════════════════════════════════════

FASE 0: DESIGN
☑ Modelo de dados definido e validado
☑ Normalização 3NF aplicada
☑ Decisões arquiteturais documentadas (ADR-003, ADR-004)

FASE 1: ESTRUTURA
☑ Schema de 10 tabelas criado
☑ Constraints e validações (CHECK, UNIQUE, FK)
☑ Índices otimizados para relatórios
☑ supplier_id NULLABLE (opcional)
☑ reserved_quantity em products ⭐ NOVO
☑ reserved_until em orders ⭐ NOVO
☑ Status EXPIRED em orders ⭐ NOVO

FASE 2: DADOS
☑ 11 usuários com BCrypt hash
☑ 5 categorias
☑ 5 fornecedores
☑ 12 produtos com metadata JSON (reserved_quantity = 0)
☑ 8 pedidos (5 aprovados + 3 pendentes com TTL)
☑ 5 registros de histórico de preços

FASE 3: DOCUMENTAÇÃO
☑ database/dump.sql completo v1.1 (22 KB)
☑ database/README.md (instruções)
☑ ADR-004 (auditoria + escopo)
☑ diagrama-ER-database.md v1.1 (Mermaid + controle reserva)

FASE 4: VALIDAÇÃO
☑ Importação no Docker bem-sucedida
☑ 10 tabelas criadas
☑ Todos os registros inseridos
☑ Consistência de valores validada
☑ Credencial admin testada
☑ Campos de reserva validados ⭐ NOVO

STATUS: ✅ 100% COMPLETO!
═══════════════════════════════════════════════════════════
```

---

## 🎯 Critérios de Sucesso Atingidos

- ✅ Banco estruturado e normalizado (3NF)
- ✅ Dados de teste realistas e completos
- ✅ Documentação profissional e detalhada
- ✅ Decisões arquiteturais justificadas (ADRs)
- ✅ Importação automatizada via Docker
- ✅ Validações executadas com sucesso
- ✅ Controle de reserva implementado (ADR-003) ⭐
- ✅ Tempo dentro do estimado (2.5h vs 2h planejado)
- ✅ Demonstra conhecimento sênior

---

**MACRO 4 Concluído com Excelência!** 🎉

**Data**: 07/11/2025  
**Responsável**: Danrley Brasil dos Santos  
**Próximo**: MACRO 5 - Camada de Domínio

---

## 📝 Notas de Versão

**v1.1 (07/11/2025)**:
- Adicionado controle de reserva temporária (ADR-003)
- Campos `reserved_quantity` em products
- Campos `reserved_until` em orders
- Status `EXPIRED` em orders
- Índice `idx_orders_reserved_until`
- Documentação atualizada (diagrama ER)