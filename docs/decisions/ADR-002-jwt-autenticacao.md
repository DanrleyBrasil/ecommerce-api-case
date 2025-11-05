# ADR-002: JWT para Autenticação

## 📋 Metadata

| Campo | Valor |
|-------|-------|
| **Status** | ✅ Aceito |
| **Data** | 04/11/2025 |
| **Contexto** | Case Técnico - E-Commerce API |

---

## 🎯 Contexto

Precisamos autenticar usuários (USER e ADMIN) e proteger endpoints da API. Sistema precisa ser stateless para facilitar escalabilidade futura.

---

## 🔍 Alternativas Consideradas

### Alternativa 1: Sessões no Servidor (Cookies)

**Prós**:
- ✅ Invalidação imediata de sessões
- ✅ Controle total no backend
- ✅ Mais simples de implementar

**Contras**:
- ❌ Estado no servidor (memória/Redis necessário)
- ❌ Dificulta escalar horizontalmente
- ❌ Não funciona bem em arquiteturas distribuídas

**Decisão**: ❌ Rejeitado - Não é stateless

---

### Alternativa 2: JWT (JSON Web Token)

**Prós**:
- ✅ Stateless (sem estado no servidor)
- ✅ Escala horizontalmente
- ✅ Padrão de mercado
- ✅ Funciona em microserviços

**Contras**:
- ⚠️ Não pode invalidar antes da expiração
- ⚠️ Token pode crescer se muitos claims

**Decisão**: ✅ **ACEITO** - Melhor para o contexto

---

## ✅ Decisão

Usar **JWT** com:
- Expiração de 24h
- Algoritmo HS256
- Claims: userId, email, role
- Header: `Authorization: Bearer <token>`

---

## 📊 Consequências

**Positivas**:
- Zero overhead de estado
- Fácil testar (Postman/Swagger)
- Preparado para microserviços

**Negativas (aceitas)**:
- Logout não invalida token imediatamente (mitigado com TTL curto)

---

## 🔗 ADRs Relacionados

- **ADR-001**: Arquitetura Modular
- **ADR-003**: Locks Pessimistas

---

**Responsável**: Danrley Brasil dos Santos