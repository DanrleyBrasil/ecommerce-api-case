# ADR-004: Auditoria Seletiva - Histórico de Preços

## 📋 Metadata

| Campo | Valor |
|-------|-------|
| **Status** | ✅ Aceito |
| **Data** | 06/11/2025 |
| **Decisores** | Danrley Brasil dos Santos |
| **Contexto** | Case Técnico - E-Commerce API |

---

## 🎯 Contexto

Ao modelar o banco de dados, identificamos a necessidade de rastreabilidade de mudanças em entidades críticas. Durante o planejamento, consideramos implementar histórico de auditoria para três domínios principais:

1. **Produtos** - Mudanças de preço
2. **Pedidos** - Mudanças de status
3. **Estoque** - Movimentações de quantidade

A questão central foi: **"Onde o histórico agrega valor real vs onde seria over-engineering?"**

---

## 🔍 Alternativas Consideradas

### Alternativa 1: Auditoria Completa (3 Tabelas de Histórico)

**Descrição**: Implementar histórico para todos os domínios identificados.

**Tabelas**:
- `product_price_history` - Mudanças de preço
- `order_status_history` - Mudanças de status (PENDENTE → APROVADO → CANCELADO)
- `product_stock_history` - Movimentações de estoque

**Prós**:
- ✅ Rastreabilidade total
- ✅ Compliance máximo
- ✅ Auditoria completa de operações

**Contras**:
- ❌ **Volume altíssimo**: Black Friday geraria milhões de registros em `product_stock_history`
- ❌ **Redundância**: `order_items` já rastreia vendas (principal movimentação de estoque)
- ❌ **Over-engineering**: Mudanças de status são sistêmicas, não decisões humanas relevantes
- ❌ Tempo de desenvolvimento adicional (+1h)

**Decisão**: ❌ **Rejeitado** - Custo-benefício desfavorável

---

### Alternativa 2: Nenhum Histórico (Apenas Timestamps)

**Descrição**: Confiar apenas nos campos de auditoria padrão (`updated_at`, `updated_by`).

**Prós**:
- ✅ Simples e direto
- ✅ Zero overhead de desenvolvimento
- ✅ Sem tabelas adicionais

**Contras**:
- ❌ **Perde histórico de preços**: Impossível fazer analytics de precificação
- ❌ **Não demonstra maturidade**: Não mostra conhecimento de patterns enterprise
- ❌ Dificuldade em debugging de mudanças passadas

**Decisão**: ❌ **Rejeitado** - Perde valor analítico importante

---

### Alternativa 3: Auditoria Seletiva (Escolhida) ⭐

**Descrição**: Implementar histórico **APENAS onde há valor real**.

**Implementar**:
- ✅ `product_price_history`

**Não Implementar** (mas documentar possibilidade futura):
- ⚠️ `order_status_history`
- ⚠️ `product_stock_history`

**Prós**:
- ✅ **Analytics de precificação**: Identificar volatilidade, impacto de promoções
- ✅ **Baixo volume**: Mudanças de preço são raras e manuais (~10-20 por mês)
- ✅ **Demonstra pragmatismo**: Escolhe tecnicamente onde aplicar patterns
- ✅ **Factível no prazo**: +25 minutos de desenvolvimento
- ✅ **Compliance leve**: Rastreia decisões comerciais importantes

**Contras** (aceitos no contexto):
- ⚠️ Não rastreia mudanças de status (mitigado: `order_date` e `payment_date` suficientes)
- ⚠️ Não rastreia movimentação de estoque (mitigado: `order_items` já fornece rastreabilidade de vendas)

**Decisão**: ✅ **ACEITO** - Melhor equilíbrio entre valor e complexidade

---

## ✅ Decisão

### Implementado: `product_price_history`

```sql
CREATE TABLE product_price_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    old_price DECIMAL(10, 2) NOT NULL,
    new_price DECIMAL(10, 2) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255) COMMENT 'Motivo: promoção, ajuste margem, etc',
    
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_product_price_product (product_id),
    INDEX idx_product_price_date (changed_at)
);
```

### Justificativa da Escolha

**Por que implementar histórico de preços?**

1. **Mudança Manual com Impacto Comercial**
    - Administradores decidem conscientemente alterar preços
    - Cada mudança tem um motivo de negócio (promoção, ajuste de margem, competição)
    - Valor para auditoria: "Quem aprovou essa promoção? Quando?"

2. **Analytics Real**
    - "Qual produto teve maior volatilidade nos últimos 30 dias?"
    - "Qual foi o impacto da Black Friday nos preços?"
    - "Produtos em promoção constante (possível problema de margem)"

3. **Baixo Volume de Dados**
    - Mudanças de preço são pontuais (~10-20 alterações/mês)
    - Crescimento linear e previsível
    - Não gera problemas de performance

4. **Compliance Comercial**
    - Rastrear decisões de precificação
    - Auditoria para análise de margem
    - Histórico para negociações com fornecedores

---

## 📊 Consequências

### Positivas ✅

1. **Analytics de Precificação**
    - Query: "Produtos com maior número de mudanças de preço"
   ```sql
   SELECT p.name, COUNT(pph.id) as num_changes
   FROM products p
   LEFT JOIN product_price_history pph ON pph.product_id = p.id
   WHERE pph.changed_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
   GROUP BY p.id
   ORDER BY num_changes DESC;
   ```

2. **Compliance Leve**
    - Rastrear quem/quando mudou preços
    - Motivos documentados no campo `reason`

3. **Debugging Facilitado**
    - "Por que o preço deste produto mudou?"
    - Resposta: Consulta no histórico

4. **Demonstra Maturidade Arquitetural**
    - Aplicar patterns onde fazem sentido (não everywhere)
    - Pragmatismo técnico

### Negativas ⚠️ (Trade-offs Aceitos)

1. **Sem Histórico de Status de Pedidos**

   **Por que não implementar:**
    - Mudanças de status são **automáticas/sistêmicas** (fluxo de pagamento)
    - Não representam decisões humanas significativas
    - Timestamps já fornecem informação suficiente:

   ```sql
   -- Já conseguimos calcular tempo de aprovação:
   SELECT 
     id,
     TIMESTAMPDIFF(MINUTE, order_date, payment_date) as minutes_to_approve
   FROM orders
   WHERE status = 'APROVADO';
   ```

   **Quando adicionar no futuro:**
    - Se implementar múltiplos status (EM_SEPARAÇÃO, ENVIADO, ENTREGUE)
    - Se compliance regulatório exigir auditoria completa
    - Se precisar rastrear SLA de processamento

2. **Sem Histórico de Estoque**

   **Por que não implementar:**
    - **Volume gigantesco**: Black Friday = milhões de registros
    - **Redundância**: `order_items` já rastreia vendas (99% das movimentações)
    - Query para histórico de vendas já funciona:

   ```sql
   -- Histórico de vendas de um produto:
   SELECT 
     oi.product_id,
     p.name,
     oi.quantity,
     o.order_date,
     o.status
   FROM order_items oi
   JOIN orders o ON o.id = oi.order_id
   JOIN products p ON p.id = oi.product_id
   WHERE oi.product_id = 5 AND o.status = 'APROVADO'
   ORDER BY o.order_date DESC;
   ```

   **Quando adicionar no futuro:**
    - Se precisar rastrear **devoluções**
    - Se precisar rastrear **ajustes manuais** de estoque
    - Se precisar rastrear **perdas/quebras**
    - Se compliance de inventário for obrigatório

---

## 🔄 Estratégia de Evolução Futura

Esta decisão **não impede** evolução futura. As possibilidades estão documentadas:

### Adicionar `order_status_history`

**Triggers para implementação:**
- Sistema evoluir para múltiplos status
- Necessidade de rastrear SLA operacional
- Compliance regulatório exigir

**Schema proposto**:
```sql
CREATE TABLE order_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

### Adicionar `product_stock_history`

**Triggers para implementação:**
- Sistema evoluir para gestão de devoluções
- Necessidade de rastrear ajustes manuais
- Auditoria de inventário obrigatória

**Schema proposto**:
```sql
CREATE TABLE product_stock_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    old_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    change_type VARCHAR(20) NOT NULL, -- 'SALE', 'RESTOCK', 'ADJUSTMENT', 'RETURN'
    reference_id BIGINT, -- order_id se venda
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

---

## 🎯 Casos de Uso Implementados

### 1. Relatório de Volatilidade de Preços

```sql
-- Top 5 produtos com maior número de mudanças nos últimos 30 dias
SELECT 
    p.name,
    COUNT(pph.id) as num_changes,
    MIN(pph.new_price) as lowest_price,
    MAX(pph.new_price) as highest_price,
    AVG(pph.new_price) as avg_price
FROM products p
LEFT JOIN product_price_history pph ON pph.product_id = p.id
WHERE pph.changed_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY p.id
ORDER BY num_changes DESC
LIMIT 5;
```

**Utilidade**: Identificar produtos em promoção constante ou com gestão de preço errática.

### 2. Auditoria de Mudança de Preço

```sql
-- Quem mudou o preço do produto X e por quê?
SELECT 
    pph.changed_at,
    pph.changed_by,
    pph.old_price,
    pph.new_price,
    (pph.new_price - pph.old_price) as price_diff,
    ROUND(((pph.new_price - pph.old_price) / pph.old_price) * 100, 2) as percent_change,
    pph.reason
FROM product_price_history pph
WHERE pph.product_id = 5
ORDER BY pph.changed_at DESC;
```

**Utilidade**: Compliance, debugging, análise de impacto de decisões comerciais.

### 3. Histórico de Preços para Gráfico

```sql
-- Evolução do preço ao longo do tempo (para gráfico)
SELECT 
    pph.changed_at,
    pph.new_price
FROM product_price_history pph
WHERE pph.product_id = 5
ORDER BY pph.changed_at ASC;
```

**Utilidade**: Visualização de tendências de preço.

---

---

## 📚 Referências

- [Audit Trail Patterns - Martin Fowler](https://martinfowler.com/eaaDev/AuditLog.html)

---

## 📦 Decisão Complementar: Escopo de CRUD

Durante a modelagem, incluímos tabelas `suppliers` e `categories` para demonstrar normalização profissional. Porém, **o requisito do case técnico pede apenas CRUD de produtos**.

### Decisão de Escopo

**Implementar AGORA**:
- ✅ **CRUD completo de Produtos** (requisito obrigatório)
- ✅ **GET /categories** - Listagem read-only (5 categorias fixas)

**NÃO implementar agora** (evolução futura):
- ⚠️ CRUD de Suppliers
- ⚠️ CRUD completo de Categories

### Justificativa

**Por que suppliers é opcional?**
1. **Escopo**: Requisito não pede gestão de fornecedores
2. **Normalização**: Tabela existe para demonstrar modelagem profissional
3. **Pragmatismo**: `supplier_id NULL` é válido (produto sem fornecedor cadastrado)
4. **Tempo**: Economiza ~2h de desenvolvimento sem perder qualidade

**Por que categories é read-only?**
1. **Dados fixos**: 5 categorias estáticas pré-populadas
2. **Suficiente**: Endpoint GET atende necessidade do CRUD de produtos
3. **Simplicidade**: Não requer interface de administração completa

### Na Prática

```java
// ProductRequest.java
public class ProductRequest {
    @NotBlank
    private String name;
    
    @NotNull
    private Long categoryId;  // OBRIGATÓRIO
    
    private Long supplierId;  // OPCIONAL ✅
}
```

**Criar produto sem fornecedor**:
```json
POST /products
{
  "name": "Mouse Genérico",
  "categoryId": 1,
  "supplierId": null  // ✅ Válido!
}
```

### Evolução Futura

**Quando implementar CRUD de Suppliers**:
- Sistema evoluir para gestão de compras
- Necessidade de rastrear fornecedores por produto
- Módulo de purchase orders

**Quando implementar CRUD de Categories**:
- Negócio precisar criar categorias dinamicamente
- Implementar hierarquia (sub-categorias)

---

## 🔗 ADRs Relacionados

- **ADR-001**: Arquitetura Modular Monolítica
- **ADR-002**: JWT para Autenticação
- **ADR-003**: Locks Pessimistas + Reserva Temporária

---

**Status**: ✅ Aceito  
**Última Atualização**: 06/11/2025  
**Responsável**: Danrley Brasil dos Santos