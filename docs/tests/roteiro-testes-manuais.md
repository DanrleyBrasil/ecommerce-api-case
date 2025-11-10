# 🧪 ROTEIRO DE TESTES MANUAIS - POSTMAN

## 📋 PRÉ-REQUISITOS

1. Importar collection: `postman/ecommerce-api-tests.json`
2. Docker rodando: `docker-compose ps` (ambos `healthy`)
3. Swagger acessível: http://localhost:8080/swagger-ui.html

---

## 🔐 FASE 1: AUTENTICAÇÃO

### 1.1 Login ADMIN

**Endpoint**: `POST /api/auth/login`  
**Token**: Nenhum  
**Body**:
```json
{
  "email": "admin@ecommerce.com",
  "password": "Admin@123"
}
```

**Resultado Esperado**:
- Status: `200 OK`
- Response contém `token` (JWT)
- Response contém `roles: ["ADMIN"]`
- Token salvo automaticamente em `{{admin_token}}`

---

### 1.2 Login USER

**Endpoint**: `POST /api/auth/login`  
**Token**: Nenhum  
**Body**:
```json
{
  "email": "user1@test.com",
  "password": "User@123"
}
```

**Resultado Esperado**:
- Status: `200 OK`
- Response contém `token` (JWT)
- Response contém `roles: ["USER"]`
- Token salvo automaticamente em `{{user_token}}`

---

## 🛍️ FASE 2: PRODUTOS (ADMIN)

### 2.1 Listar Produtos

**Endpoint**: `GET /api/products`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Array com 12+ produtos
- Cada produto tem: `id`, `name`, `price`, `stockQuantity`, `categoryName`

---

### 2.2 Buscar Produto por ID

**Endpoint**: `GET /api/products/1`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Produto `id: 1` retornado
- Nome: "Mouse Gamer Logitech G203"
- Preço: `149.90`

---

### 2.3 Criar Produto

**Endpoint**: `POST /api/products`  
**Token**: `{{admin_token}}`  
**Body**:
```json
{
  "name": "Produto Teste Avaliação",
  "description": "Criado durante testes de validação",
  "price": 299.90,
  "stockQuantity": 100,
  "categoryId": 1,
  "supplierId": 1,
  "sku": "TEST-AVAL-001"
}
```

**Resultado Esperado**:
- Status: `201 Created`
- Response contém produto criado com `id`
- Salvar `id` em `{{created_product_id}}`

---

### 2.4 Atualizar Produto

**Endpoint**: `PUT /api/products/{{created_product_id}}`  
**Token**: `{{admin_token}}`  
**Body**:
```json
{
  "name": "Produto Teste Avaliação - ATUALIZADO",
  "description": "Atualizado via PUT",
  "price": 399.90,
  "stockQuantity": 150,
  "categoryId": 1,
  "supplierId": 1,
  "sku": "TEST-AVAL-001"
}
```

**Resultado Esperado**:
- Status: `200 OK`
- Preço atualizado: `399.90`
- Estoque atualizado: `150`

---

### 2.5 Deletar Produto (Soft Delete)

**Endpoint**: `DELETE /api/products/{{created_product_id}}`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `204 No Content`
- Produto não aparece mais em `GET /api/products` (active=false)

---

## 🔒 FASE 3: PRODUTOS (USER) - Validação de Autorização

### 3.1 USER Lista Produtos (Permitido)

**Endpoint**: `GET /api/products`  
**Token**: `{{user_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Array com produtos retornado normalmente

---

### 3.2 USER Tenta Criar Produto (Negado)

**Endpoint**: `POST /api/products`  
**Token**: `{{user_token}}`  
**Body**: (qualquer)

**Resultado Esperado**:
- Status: `403 Forbidden`
- Mensagem: "Access Denied" ou similar

---

## 🛒 FASE 4: PEDIDOS (USER)

### 4.1 Criar Pedido

**Endpoint**: `POST /api/orders`  
**Token**: `{{user_token}}`  
**Body**:
```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ]
}
```

**Resultado Esperado**:
- Status: `201 Created`
- `status: "PENDENTE"`
- `reservedUntil` preenchido (now + 10min)
- Response contém `items` array com 2 itens
- Salvar `id` em `{{created_order_id}}`

---

### 4.2 Listar Meus Pedidos

**Endpoint**: `GET /api/orders`  
**Token**: `{{user_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Array com pedidos do user1 APENAS
- NÃO aparece pedidos de outros usuários

---

### 4.3 Buscar Meu Pedido por ID

**Endpoint**: `GET /api/orders/{{created_order_id}}`  
**Token**: `{{user_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Pedido retornado com `status: "PENDENTE"`
- Contém `items` completos

---

### 4.4 Processar Pagamento

**Endpoint**: `POST /api/orders/{{created_order_id}}/payment`  
**Token**: `{{user_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- `status` mudou para `"APROVADO"`
- `paymentDate` preenchido
- `reservedUntil` = `null` (reserva liberada)

---

### 4.5 Criar Novo Pedido (Para Cancelar)

**Endpoint**: `POST /api/orders`  
**Token**: `{{user_token}}`  
**Body**:
```json
{
  "items": [
    {
      "productId": 4,
      "quantity": 1
    }
  ]
}
```

**Resultado Esperado**:
- Status: `201 Created`
- Salvar `id` em `{{order_to_cancel_id}}`

---

### 4.6 Cancelar Pedido PENDENTE

**Endpoint**: `DELETE /api/orders/{{order_to_cancel_id}}`  
**Token**: `{{user_token}}`

**Resultado Esperado**:
- Status: `204 No Content`
- Pedido não aparece mais em `GET /api/orders` (ou status=CANCELADO)

---

## 👨‍💼 FASE 5: PEDIDOS (ADMIN)

### 5.1 Listar TODOS os Pedidos

**Endpoint**: `GET /api/orders`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Array com pedidos de TODOS os usuários
- Deve conter pedidos de user1, user2, user3, etc.

---

### 5.2 Cancelar Pedido de Qualquer Usuário

**Endpoint**: `DELETE /api/orders/{id_de_pedido_de_outro_user}`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `204 No Content`
- Pedido cancelado mesmo sendo de outro user

---

## 📊 FASE 6: RELATÓRIOS (ADMIN)

### 6.1 Top 5 Usuários com Mais Compras

**Endpoint**: `GET /api/reports/top-users`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Array com até 5 usuários
- Cada item contém: `userId`, `userName`, `totalOrders`, `totalSpent`
- Ordenado por `totalOrders` DESC

---

### 6.2 Faturamento em Período

**Endpoint**: `GET /api/reports/revenue?startDate=2025-11-01&endDate=2025-11-30`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Response contém `totalRevenue` (valor numérico)
- Response contém `orderCount`

---

### 6.3 Ticket Médio por Usuário

**Endpoint**: `GET /api/reports/avg-ticket?startDate=2025-11-01&endDate=2025-11-30`  
**Token**: `{{admin_token}}`

**Resultado Esperado**:
- Status: `200 OK`
- Array com usuários e seus tickets médios
- Cada item: `userId`, `userName`, `totalOrders`, `averageTicket`

---

## ✅ CHECKLIST DE VALIDAÇÃO COMPLETA

Execute todos os testes na ordem e marque:

```
☐ 1.1 Login ADMIN (200)
☐ 1.2 Login USER (200)
☐ 2.1 Listar Produtos ADMIN (200)
☐ 2.2 Buscar Produto por ID (200)
☐ 2.3 Criar Produto (201)
☐ 2.4 Atualizar Produto (200)
☐ 2.5 Deletar Produto (204)
☐ 3.1 USER Lista Produtos (200)
☐ 3.2 USER Tenta Criar Produto (403)
☐ 4.1 Criar Pedido USER (201)
☐ 4.2 Listar Meus Pedidos (200)
☐ 4.3 Buscar Meu Pedido (200)
☐ 4.4 Processar Pagamento (200)
☐ 4.5 Criar Pedido para Cancelar (201)
☐ 4.6 Cancelar Pedido PENDENTE (204)
☐ 5.1 Listar TODOS Pedidos ADMIN (200)
☐ 5.2 Cancelar Pedido de Outro User (204)
☐ 6.1 Top 5 Usuários (200)
☐ 6.2 Faturamento Período (200)
☐ 6.3 Ticket Médio (200)
```

**Total**: 20 testes

---

## 🎯 CRITÉRIOS DE SUCESSO

- ✅ Todos os status codes corretos
- ✅ Autenticação JWT funcionando (401 sem token)
- ✅ Autorização RBAC funcionando (403 USER em endpoints ADMIN)
- ✅ Isolamento de dados (USER só vê seus pedidos)
- ✅ Lógica de negócio (reserva, pagamento, cancelamento)
- ✅ Relatórios retornam dados consistentes

---

## 📤 EXPORTAR COLLECTION

Após validar tudo:
```
Postman → Collection → ... → Export
Salvar em: postman/ecommerce-api-tests.json
```

---

**Tempo estimado de execução**: 10-15 minutos  
**Pré-requisito**: Docker rodando, tokens gerados