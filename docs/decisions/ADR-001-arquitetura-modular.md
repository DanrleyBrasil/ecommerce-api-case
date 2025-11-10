# ADR-001: Arquitetura Modular Monolítica

## 📋 Metadata

| Campo | Valor |
|-------|-------|
| **Status** | ✅ Aceito |
| **Data** | 04/11/2025 |
| **Decisores** | Danrley Brasil dos Santos |
| **Contexto** | Case Técnico - E-Commerce API |

---

## 🎯 Contexto

Ao iniciar o desenvolvimento da API de e-commerce, foi necessário definir a arquitetura base do sistema. A aplicação precisa gerenciar quatro domínios principais:

1. **Autenticação** (`auth`) - Gerenciamento de usuários e JWT
2. **Produtos** (`products`) - CRUD de produtos com categorias
3. **Pedidos** (`orders`) - Gestão de pedidos e itens
4. **Relatórios** (`reports`) - Consultas SQL otimizadas

### Requisitos Técnicos

- ✅ Prazo de entrega: 6 dias
- ✅ Demonstrar capacidade técnica pleno/sênior
- ✅ Código limpo e organizado
- ✅ Facilitar manutenção e evolução futura
- ✅ Permitir teste e avaliação rápida

### Restrições

- ⚠️ Equipe: 1 desenvolvedor
- ⚠️ Tempo limitado para desenvolvimento
- ⚠️ Necessidade de documentação clara
- ⚠️ Avaliadores precisam entender rapidamente

---

## 🔍 Alternativas Consideradas

### Alternativa 1: Microserviços desde o início

**Descrição**: Criar 4 microserviços independentes (auth-service, product-service, order-service, report-service).

**Prós**:
- ✅ Escalabilidade independente por domínio
- ✅ Deploy independente
- ✅ Tecnologias diferentes por serviço
- ✅ Isolamento total de falhas

**Contras**:
- ❌ Complexidade operacional (Docker Compose com 4+ containers)
- ❌ Overhead de comunicação (REST/gRPC entre serviços)
- ❌ Transações distribuídas (Saga pattern necessário)
- ❌ Tempo de desenvolvimento muito maior
- ❌ Dificuldade para testes locais
- ❌ Configuração complexa para avaliadores

**Decisão**: ❌ **Rejeitado** - Over-engineering para o escopo atual

---

### Alternativa 2: Arquitetura em Camadas Tradicional

**Descrição**: Estrutura clássica `controller/service/repository` sem separação por domínio.

```
src/
├── controller/
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── OrderController.java
│   └── ReportController.java
├── service/
│   ├── AuthService.java
│   ├── ProductService.java
│   └── ...
└── repository/
    ├── UserRepository.java
    └── ...
```

**Prós**:
- ✅ Estrutura familiar para maioria dos devs
- ✅ Simples de implementar
- ✅ Baixa curva de aprendizado

**Contras**:
- ❌ Acoplamento entre domínios
- ❌ Dificulta evolução para microserviços
- ❌ Crescimento desordenado em projetos grandes
- ❌ Violação do Single Responsibility Principle em nível de módulo
- ❌ Testes menos isolados

**Decisão**: ❌ **Rejeitado** - Não demonstra maturidade arquitetural

---

### Alternativa 3: Arquitetura Modular Monolítica (Escolhida)

**Descrição**: Monolito organizado por domínios com estrutura interna consistente.

```
src/main/java/com/ecommerce/
├── auth/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── security/
├── products/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── orders/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── reports/
│   ├── controller/
│   ├── service/
│   └── dto/
└── shared/
    ├── entity/
    ├── enums/
    └── exception/
```

**Prós**:
- ✅ Coesão por domínio (alta modularidade)
- ✅ Facilita entendimento do código
- ✅ Preparação natural para microserviços
- ✅ Deploy único simplificado
- ✅ Transações ACID nativas
- ✅ Baixa latência (in-process)
- ✅ Facilita testes e debugging
- ✅ Rápido para desenvolver

**Contras**:
- ⚠️ Escalabilidade limitada ao monolito
- ⚠️ Deploy único (não independente)
- ⚠️ Requer disciplina para evitar acoplamento

**Decisão**: ✅ **ACEITO** - Melhor custo-benefício para o contexto atual

---

## ✅ Decisão

**Adotada Arquitetura Modular Monolítica** com os seguintes princípios:

### 1. **Separação por Bounded Context (DDD Light)**

Cada módulo representa um bounded context:
- `auth` - Contexto de identidade e acesso
- `products` - Contexto de catálogo
- `orders` - Contexto de vendas
- `reports` - Contexto de analytics

### 2. **Estrutura Interna Consistente**

Cada módulo segue a mesma organização:
```
module/
├── controller/    # REST endpoints
├── service/       # Lógica de negócio
├── repository/    # Acesso a dados
├── entity/        # Entidades JPA
├── dto/           # Request/Response
└── mapper/        # Conversões (quando necessário)
```

### 3. **Camada Shared para Elementos Transversais**

```
shared/
├── entity/BaseEntity.java        # Auditoria
├── enums/                         # Enums reutilizáveis
├── exception/                     # Exceções globais
└── config/                        # Configurações compartilhadas
```

### 4. **Regras de Dependência**

- ✅ Módulos podem depender de `shared`
- ✅ Módulos **NÃO** devem depender diretamente de outros módulos
- ✅ Comunicação entre módulos via Services injetados (baixo acoplamento)
- ✅ Sem imports cruzados de `entity` entre módulos

---

## 📊 Consequências

### Positivas ✅

1. **Desenvolvimento Ágil**
    - Setup único (1 projeto Spring Boot)
    - Debugging simples (1 processo)
    - Testes rápidos (em memória)

2. **Manutenibilidade**
    - Código organizado por domínio
    - Fácil localizar funcionalidades
    - Mudanças isoladas por módulo

3. **Performance**
    - Zero overhead de rede entre módulos
    - Transações ACID nativas
    - Queries otimizadas em um único banco

4. **Evolução Futura**
    - Preparado para extração de microserviços
    - Módulos já possuem fronteiras claras
    - Migração incremental possível

5. **Avaliação Técnica**
    - Demonstra conhecimento de DDD
    - Mostra visão arquitetural madura
    - Código limpo e profissional

### Negativas ⚠️ (Trade-offs aceitos no contexto atual)

1. **Escalabilidade**
    - Escala vertical (mais CPU/RAM)
    - Não permite escalar módulos independentemente
    - **Contexto**: Suficiente para o escopo atual (6 dias úteis); evolução para microserviços quando justificado

2. **Deploy**
    - Deploy único para todo sistema
    - **Contexto**: Simplifica entrega e validação do case técnico; CI/CD robusto mitiga riscos em produção

3. **Disciplina de Código**
    - Requer atenção para evitar acoplamento entre módulos
    - **Contexto**: Aceitável para desenvolvedor com experiência; validação via code review quando em equipe

4. **Tamanho do Artefato**
    - Um único JAR com todas funcionalidades
    - **Contexto**: Aceitável para aplicações médias; não é limitante no escopo atual

---

## 🔄 Estratégia de Evolução (Possível Caminho Futuro)

Esta arquitetura **permite** evolução incremental para microserviços **se necessário**. Não é uma roadmap obrigatória, mas uma possibilidade caso o sistema atinja escala que justifique a complexidade adicional:
```
FASE 1 (ATUAL): Modular Monolith
└── 1 aplicação, 4 módulos internos

FASE 2 (SE NECESSÁRIO): Microserviços Híbridos  
├── Auth Service (extraído)
├── Monolito (products + orders + reports)
└── Comunicação via REST

FASE 3 (SE NECESSÁRIO): Full Microservices
├── Auth Service
├── Product Service  
├── Order Service
└── Report Service
```

**Ver documento**: `docs/architecture/evolucao-microservices.md`

---

## 📚 Referências

- [Modular Monoliths - Simon Brown](https://www.youtube.com/watch?v=5OjqD-ow8GE)
- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
- [Monolith First - Martin Fowler](https://martinfowler.com/bliki/MonolithFirst.html)
- [Building Microservices - Sam Newman](https://samnewman.io/books/building_microservices/)

---

## 🔗 ADRs Relacionados

- **ADR-002**: Escolha de JWT para Autenticação *(planejado)*
- **ADR-003**: Locks Pessimistas para Controle de Estoque *(planejado)*

---

**Status**: ✅ Aceito  
**Última Atualização**: 04/11/2025  
**Responsável**: Danrley Brasil dos Santos