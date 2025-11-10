# ADR-002: JWT para Autenticação

## 📋 Metadata

| Campo | Valor |
|-------|-------|
| **Status** | ✅ Aceito |
| **Data** | 04/11/2025 |
| **Contexto** | Case Técnico - E-Commerce API |

---

## 🎯 Contexto

Precisamos de um mecanismo de autenticação para proteger os endpoints da API, diferenciando usuários com perfis distintos (`USER` e `ADMIN`). O sistema deve ser projetado para ser **stateless** (sem estado no servidor), a fim de facilitar a manutenção, o desempenho e a escalabilidade horizontal futura em um ambiente potencialmente distribuído (microserviços).

---

## 🔍 Alternativas Consideradas

### Alternativa 1: Sessões Stateful (ID de Sessão em Cookie)

Neste modelo, o servidor gera um ID de sessão único, armazena os dados do usuário associados a esse ID no backend (em memória, banco de dados ou Redis) e envia apenas o ID para o cliente dentro de um cookie.

**Prós**:
- ✅ **Controle Total:** A sessão pode ser invalidada instantaneamente no servidor a qualquer momento (logout forçado).
- ✅ **Segurança:** O cookie contém apenas um ID sem significado, não expondo dados do usuário.
- ✅ **Simplicidade para Cenários Monolíticos:** É um padrão tradicional e bem compreendido para aplicações únicas.

**Contras**:
- ❌ **Quebra o Requisito Stateless:** Exige um armazenamento compartilhado de sessões, criando um ponto de dependência.
- ❌ **Complexidade em Escala:** Dificulta a escalabilidade horizontal. Cada requisição exige uma consulta ao repositório de sessões.
- ❌ **Inadequado para Microserviços:** Cria acoplamento e gargalos de comunicação entre os serviços.

**Decisão**: ❌ **Rejeitado**. Incompatível com o requisito fundamental de ser um sistema stateless e preparado para escalar.

---

### Alternativa 2: JWT (JSON Web Token) via Header `Authorization`

Neste modelo, o token JWT (contendo os dados do usuário) é gerado, assinado e enviado ao cliente, que o armazena (ex: em `localStorage`) e o envia de volta em cada requisição no cabeçalho `Authorization: Bearer <token>`.

**Prós**:
- ✅ **Stateless:** O servidor não precisa armazenar estado de sessão. Cada token é autossuficiente.
- ✅ **Escalabilidade e Desempenho:** Ideal para escalabilidade horizontal e arquiteturas de microserviços, pois qualquer serviço pode validar o token de forma independente.
- ✅ **Padrão Universal:** Amplamente adotado e compatível com diversos tipos de clientes (web, mobile, outros serviços).

**Contras**:
- ⚠️ **Revogação Complexa:** Um token é válido até sua expiração. A invalidação imediata requer uma camada extra de complexidade (ex: blocklist).
- ⚠️ **Segurança no Cliente:** Se armazenado em `localStorage`, é vulnerável a ataques XSS (Cross-Site Scripting).

**Decisão**: ✅ **ACEITO**. Alinha-se perfeitamente com os requisitos de ser stateless e escalável, sendo o padrão para APIs modernas.

---

### Nota Sobre o Uso de Cookies com JWT

É importante notar que "JWT" e "Cookies" não são mutuamente exclusivos. Uma terceira abordagem, híbrida e muito robusta, seria **armazenar o JWT da Alternativa 2 dentro de um cookie seguro (`HttpOnly`, `SameSite`)**.

Esta abordagem combina o melhor dos dois mundos: a natureza **stateless do JWT** com a **segurança aprimorada dos cookies** (proteção contra XSS).

**Por que não foi escolhida para este projeto?**
Para o escopo atual de uma API pura, que deve ser simples e universal para ser consumida por diferentes tipos de clientes (não apenas navegadores), o padrão `Authorization: Bearer` é mais direto e desacoplado. A implementação com cookies, embora mais segura para frontends web, adiciona considerações de segurança (CSRF) e pode ser menos trivial para clientes non-browser. A simplicidade e universalidade do header `Authorization` foram priorizadas.

---

## ✅ Decisão

Será implementado o uso de **JWT (JSON Web Token)** para autenticação e autorização. O token será transmitido através do cabeçalho HTTP padrão, seguindo o esquema "Bearer".

- **Algoritmo de Assinatura:** `HS256` (HMAC com SHA-256), que requer um segredo compartilhado entre os serviços que geram e validam o token.
- **Tempo de Expiração:** O token terá uma vida útil de **24 horas**.
- **Payload (Claims):** O corpo do token conterá as seguintes informações essenciais:
    - `sub` (Subject): Identificador único do usuário (userId).
    - `email`: E-mail do usuário.
    - `role`: Perfil do usuário (`USER` ou `ADMIN`).
    - `iat` (Issued At): Timestamp de quando o token foi gerado.
    - `exp` (Expiration Time): Timestamp de quando o token irá expirar.
- **Transmissão:** O cliente deve enviar o token em cada requisição para endpoints protegidos no cabeçalho `Authorization`, no formato: `Authorization: Bearer <token>`.

---

## 📊 Consequências

**Positivas**:
- **Independência de Estado:** O servidor não precisa manter registros de sessão, reduzindo o consumo de memória e a complexidade.
- **Pronto para Escalabilidade:** A arquitetura stateless facilita a adição de novas instâncias da aplicação (escalabilidade horizontal) sem a necessidade de sincronização de sessões.
- **Interoperabilidade:** Facilita a integração com diferentes tipos de clientes (web, mobile, CLI, outros serviços) e a documentação/teste com ferramentas como Postman e Swagger UI.
- **Desacoplamento:** Prepara a arquitetura para uma eventual migração para microserviços, onde cada serviço pode validar o token de forma autônoma.

**Negativas (aceitas)**:
- **Impossibilidade de Revogação Imediata:** Uma vez emitido, um token é válido até sua expiração. Se um token for comprometido, ele poderá ser usado até expirar. Esta limitação é considerada aceitável para o escopo do projeto e pode ser mitigada com tempos de expiração mais curtos, se necessário no futuro. O logout no cliente consistirá apenas em descartar o token localmente.

---

## 🔗 ADRs Relacionados

- **ADR-001**: Arquitetura Modular
- **ADR-003**: Locks Pessimistas

---

**Responsável**: Danrley Brasil dos Santos