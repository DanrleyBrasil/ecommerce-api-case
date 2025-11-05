# ADR-003: Locks Pessimistas + Reserva Temporária de Estoque

## 📋 Metadata

| Campo | Valor |
|-------|-------|
| **Status** | ✅ Aceito |
| **Data** | 04/11/2025 |
| **Contexto** | Case Técnico - E-Commerce API |

---

## 🎯 Contexto

**Problema**: Race condition + UX ruim em concorrência de compras.

### Cenário Real:
```
Estoque: 5 produtos
Cliente A: adiciona 4 ao carrinho
Cliente B: adiciona 2 ao carrinho  
Cliente C: adiciona 1 ao carrinho

Se C paga primeiro → OK (sobra 4)
Se B paga depois → OK (sobra 2)
Se A tenta pagar → FALHA (só tem 2, queria 4) ❌
```

**Necessidade**:
1. Garantir consistência (sem overselling)
2. Melhorar UX (cliente sabe disponibilidade real no checkout)
3. Permitir ajuste de quantidade antes de pagar

---

## 🔍 Alternativas

### 1. Lock Otimista (@Version)

**Contras**:
- ❌ Cliente descobre indisponibilidade só no pagamento
- ❌ Retry necessário

**Decisão**: ❌ Rejeitado - UX ruim

---

### 2. Lock Pessimista Simples (FOR UPDATE apenas no pagamento)

**Contras**:
- ⚠️ Validação só no pagamento final
- ⚠️ Cliente pode perder tempo no checkout sem garantia

**Decisão**: ⚠️ Parcialmente aceito - complementar com reserva

---

### 3. Reserva Temporária + Lock Pessimista (Escolhido)

**Como funciona**:

**Fluxo**:
1. Cliente inicia checkout → **Reserva temporária** (TTL 10min)
2. Se estoque insuficiente → oferecer quantidade disponível
3. Cliente confirma pagamento → **Lock pessimista** + baixa definitiva
4. Se TTL expira ou cancela → devolve ao estoque

**Prós**:
- ✅ Cliente sabe disponibilidade real no checkout
- ✅ Pode ajustar quantidade antes de pagar
- ✅ Reserva garante produto durante pagamento
- ✅ Expira automaticamente se abandonar

**Contras**:
- ⚠️ Estoque "travado" temporariamente
- ⚠️ Requer gerenciamento de TTL

**Decisão**: ✅ **ACEITO** - Melhor custo-benefício UX vs Complexidade

---

## ✅ Decisão

**Estratégia Híbrida**: Reserva Temporária (checkout) + Lock Pessimista (pagamento)

### Implementação

**1. Iniciar Checkout (Reserva Temporária)**

```java
// Validar e reservar estoque
@Transactional
public ReserveResponse reserveStock(CreateOrderRequest request) {
    for (item : request.items) {
        Product p = productRepo.findByIdWithLock(item.productId);
        
        if (p.stockQuantity < item.quantity) {
            // Oferecer quantidade disponível
            return ReserveResponse.partial(p.id, p.stockQuantity);
        }
        
        // Reservar (não baixa ainda)
        p.reservedQuantity += item.quantity;
    }
    // Criar Order status=RESERVED (TTL 10min)
}
```

**2. Processar Pagamento (Lock Pessimista + Baixa Definitiva)**

```java
@Transactional(isolation = SERIALIZABLE)
public PaymentResponse processPayment(Long orderId) {
    Order order = orderRepo.findById(orderId);
    
    for (item : order.items) {
        Product p = productRepo.findByIdWithLock(item.productId);
        
        // Re-validar (pode ter expirado e outro cliente comprou)
        if (p.stockQuantity < item.quantity) {
            rollback(); // Devolve reservas
            throw InsufficientStockException();
        }
        
        // Baixa definitiva
        p.stockQuantity -= item.quantity;
        p.reservedQuantity -= item.quantity;
    }
    
    order.status = APPROVED;
}
```

**3. Job de Expiração (Scheduled)**

```java
@Scheduled(fixedRate = 60000) // cada 1min
public void expireReservations() {
    List<Order> expired = orderRepo.findExpiredReservations();
    
    for (order : expired) {
        // Devolver ao estoque
        for (item : order.items) {
            Product p = productRepo.findById(item.productId);
            p.reservedQuantity -= item.quantity;
        }
        order.status = EXPIRED;
    }
}
```

### Tratamento de Indisponibilidade Parcial

```java
if (disponivel < solicitado) {
    return ReserveResponse.builder()
        .status("PARTIAL")
        .disponivel(disponivel)
        .mensagem("Só temos " + disponivel + " disponíveis. Continuar?")
        .build();
}
```

---

## 📊 Consequências

### Positivas ✅

- UX excelente (cliente sabe disponibilidade real)
- Permite ajuste de quantidade no checkout
- Consistência garantida (lock pessimista final)
- Reservas expiram automaticamente

### Negativas ⚠️ (Aceitas no contexto)

- Complexidade adicional (job de expiração)
- Estoque temporariamente "travado" (max 10min)
- **Mitigação**: TTL curto + job eficiente

---

## 🔄 Possível Evolução Futura

Se necessário expandir:
- Fila de espera para produtos esgotados
- Notificação quando voltar ao estoque
- Sistema de prioridade (cliente premium)

---

**Status**: ✅ Aceito  
**Responsável**: Danrley Brasil dos Santos