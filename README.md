# E-Commerce API

API RESTful construída com Spring Boot para gerenciamento completo de e-commerce, incluindo autenticação JWT, catálogo de produtos e processamento de pedidos.

## 🚀 Stack Tecnológica

**Core:**
- Java 17
- Spring Boot 3.5.7
- Spring Security 6.x (com JWT)
- Spring Data JPA
- MySQL 8.0

**Ferramentas:**
- Maven
- Docker & Docker Compose
- Swagger/OpenAPI 3.0

## ⚡ Início Rápido

```bash
git clone https://github.com/DanrleyBrasil/ecommerce-api-case.git
cd ecommerce-api-case
docker-compose up -d
```

**Pronto!** A aplicação estará rodando em `http://localhost:8080`

Acesse a documentação interativa: `http://localhost:8080/swagger-ui.html`

> 💡 Para mais detalhes sobre configuração e troubleshooting, consulte [QUICKSTART.md](QUICKSTART.md)

## 📐 Arquitetura

O projeto segue uma **arquitetura modular monolítica**, organizando o código por domínios de negócio para facilitar manutenção e permitir evolução futura para microserviços quando necessário.

### Estrutura de Módulos

```
src/main/java/com/danrley/ecommerce/
├── shared/              # Componentes transversais
│   ├── entity/         # BaseEntity com auditoria automática
│   ├── enums/          # Enums compartilhados (OrderStatus, PaymentStatus, etc)
│   └── exception/      # Tratamento global de exceções
│
├── auth/               # Autenticação e Autorização
│   ├── controller/    # Endpoints de login/registro
│   ├── service/       # Lógica de autenticação e geração de JWT
│   ├── security/      # Filtros, providers e configurações Spring Security
│   ├── entity/        # User, Role
│   └── dto/           # LoginRequest, AuthResponse
│
├── products/          # Gestão de Catálogo
│   ├── controller/   # CRUD de produtos (com soft delete)
│   ├── service/      # Regras de negócio e validações
│   ├── repository/   # Consultas JPA customizadas
│   ├── entity/       # Product, Category, Supplier
│   └── dto/          # ProductRequest, ProductResponse
│
├── orders/           # Processamento de Pedidos
│   ├── controller/  # Criação e consulta de pedidos
│   ├── service/     # Lógica de pedidos e controle de estoque
│   ├── repository/  # Queries otimizadas com locks pessimistas
│   ├── entity/      # Order, OrderItem, Payment
│   └── dto/         # OrderRequest, OrderResponse
│
└── reports/         # Relatórios Gerenciais
    ├── controller/ # Endpoints administrativos
    ├── service/    # Agregações e queries SQL otimizadas
    └── dto/        # DTOs especializados para relatórios
```

### Decisões Arquiteturais (ADRs)

A documentação completa das decisões técnicas está disponível nos Architecture Decision Records:

- **[ADR-001](./docs/decisions/ADR-001-arquitetura-modular.md)** - Escolha da Arquitetura Modular Monolítica
- **[ADR-002](./docs/decisions/ADR-002-jwt-autenticacao.md)** - Implementação de JWT para Autenticação
- **[ADR-003](./docs/decisions/ADR-003-locks-pessimistas.md)** - Locks Pessimistas para Controle de Estoque
- **[ADR-004](./docs/decisions/ADR-004-auditoria-seletiva.md)** - Estratégia de Auditoria Seletiva

### Diagramas Técnicos

- **[Diagrama de Classes](./docs/architecture/diagrama-classes.md)** - Modelo de domínio e relacionamentos
- **[Diagrama ER](./docs/architecture/diagrama-ER-database.md)** - Estrutura do banco de dados
- **[Diagrama de Sequência](./docs/architecture/diagrama-sequencia.md)** - Fluxo de criação de pedidos

## 🔐 Autenticação com Spring Security

A API utiliza **JWT (JSON Web Token)** com Spring Security para controle de acesso baseado em roles.

### Obtendo um Token

**Endpoint:** `POST /api/auth/login`

```json
{
  "email": "admin@ecommerce.com",
  "password": "senha123"
}
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "name": "Administrador",
  "email": "admin@ecommerce.com",
  "roles": ["ADMIN"]
}
```

### Usando o Token

Inclua o token JWT no header de todas as requisições protegidas:

```bash
Authorization: Bearer {seu-token-jwt}
```

**Exemplo:**
```bash
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### Usuários Pré-configurados

| Email | Senha | Perfil | Permissões |
|-------|-------|--------|-----------|
| admin@ecommerce.com | senha123 | ADMIN | Acesso total + relatórios |
| user1@ecommerce.com | senha123 | USER | Consulta e compra |

> O banco de dados já vem populado com 12 produtos e 8 pedidos de exemplo para facilitar os testes.

## 📚 Documentação da API

A documentação completa da API está disponível via **Swagger UI** com suporte para autenticação JWT integrada:

```
http://localhost:8080/swagger-ui.html
```

## 🔧 Configuração e Build

### Variáveis de Ambiente

**Para este case técnico**, as configurações já vêm com valores padrão funcionais em `application.yml` e `docker-compose.yml`, permitindo execução imediata sem setup adicional.

**⚠️ IMPORTANTE:** Em ambiente de produção, SEMPRE usar variáveis de ambiente e secrets managers (AWS Secrets Manager, HashiCorp Vault, etc). Nunca comitar credenciais no código.

Exemplo de customização (opcional):
```bash
# .env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ecommerce
DB_USER=seu_usuario
DB_PASSWORD=sua_senha_segura
JWT_SECRET=sua_chave_secreta_min_256bits
```

### Build Local

Se necessário gerar o JAR manualmente:

```bash
./mvnw clean package
```

O artefato será gerado em: `target/ecommerce-api-0.0.1-SNAPSHOT.jar`

### Executando o JAR
**Observação:** Garanta que o banco de dados está rodando ao executar diretamente o arquivo .jar

```bash
java -jar target/ecommerce-api-0.0.1-SNAPSHOT.jar
```

## 🛑 Gerenciamento de Containers

```bash
# Parar aplicação
docker-compose down

# Parar e limpar volumes (reseta banco de dados)
docker-compose down -v

# Ver logs da aplicação
docker-compose logs -f app

# Reconstruir imagens após mudanças no código
docker-compose up -d --build
```

## 🔒 Considerações de Segurança

### Implementado neste Case

✅ **Autenticação JWT** com Spring Security  
✅ **BCrypt** para hash de senhas  
✅ **Autorização baseada em Roles** (ADMIN/USER)  
✅ **Validação de entrada** com Bean Validation  
✅ **Proteção contra SQL Injection** via JPA/Hibernate

### Para Produção

Em um ambiente real, considerar adicionar:

- **Rate Limiting** para prevenir abuso de API
- **HTTPS obrigatório** com certificados válidos
- **CORS** configurado restritivamente
- **Auditoria completa** de ações sensíveis
- **Rotação de JWT secrets** periodicamente
- **Tokens de refresh** para melhor UX sem comprometer segurança

> 📖 Detalhes sobre decisões de segurança em [ADR-002](./docs/decisions/ADR-002-jwt-autenticacao.md)

## 🚀 Melhorias futuras

Com mais tempo disponível, existem várias melhorias que considero essenciais para elevar este projeto a um nível production-ready. Priorizo sempre entregar funcionalidades sólidas dentro do prazo, mas reconheço onde investiria esforços adicionais:

### Testes Automatizados

**Por que não implementei agora:**
- Foco em entregar funcionalidades completas e bem documentadas no prazo do case
- Priorizei testes manuais sistemáticos via Postman com validações de todos os cenários
- Validação completa de regras de negócio através de testes exploratórios

**O que implementaria:**
- **Testes Unitários** com JUnit 5 e Mockito para camada de serviço
- **Testes de Integração** com `@SpringBootTest` para validar fluxos completos
- **Testes de Segurança** validando autenticação, autorização e cenários de acesso negado
- **Cobertura mínima de 80%** com relatórios via JaCoCo

### Arquitetura e Escalabilidade

**Microsserviços:**
- Migração incremental para microserviços quando justificado por volume
- Separação em serviços: Auth, Products, Orders, Payments, Reports
- Event-driven architecture com mensageria (RabbitMQ/Kafka)
- CQRS para otimizar leitura/escrita em contextos críticos

**Infraestrutura:**
- Orquestração com **Kubernetes** (deployments, HPA, health checks)
- **Service Mesh** (Istio/Linkerd) para resiliência e observabilidade
- **API Gateway** centralizado (Kong/AWS API Gateway)
- **Cache distribuído** com Redis para queries frequentes

**Gerais**
- Ajuste no tipo de ID para UUID autogerado em campos PK

### Observabilidade

**Logging e Monitoramento:**
- **Spring Boot Actuator** com métricas customizadas
- **Prometheus + Grafana** para dashboards em tempo real
- **ELK Stack** (Elasticsearch, Logstash, Kibana) para logging centralizado
- **Distributed Tracing** com Jaeger/Zipkin para rastreamento entre serviços
- **Alertas proativos** baseados em SLOs (Service Level Objectives)

### Segurança Avançada

**Proteções Adicionais:**
- **Rate Limiting** por IP/usuário com Redis/Bucket4j
- **Refresh Tokens** para melhor UX sem comprometer segurança
- **OAuth 2.0** para integração com provedores externos
- **Auditoria completa** de ações sensíveis (criar/atualizar/deletar)
- **Rotating secrets** com gerenciadores (AWS Secrets Manager, Vault)
- **WAF** (Web Application Firewall) para proteção contra ataques comuns

### Performance e Otimização

**Banco de Dados:**
- **Read replicas** para distribuir carga de leitura
- **Particionamento** de tabelas grandes (orders, order_items)
- **Índices compostos** adicionais baseados em análise de queries
- **Connection pooling** otimizado (HikariCP tuning)

**Aplicação:**
- **Cache de segundo nível** do Hibernate para entidades frequentes
- **Lazy loading** otimizado para evitar N+1 queries
- **Async processing** com `@Async` para operações não-críticas
- **Batch processing** para importações e relatórios pesados

### CI/CD e DevOps

**Pipeline Completo:**
- **GitHub Actions** ou GitLab CI com stages: build → test → security scan → deploy
- **Análise estática** com SonarQube (qualidade, vulnerabilidades, code smells)
- **Testes de performance** automatizados (JMeter/Gatling)
- **Blue-Green deployment** ou Canary releases para deploys sem downtime
- **Rollback automático** em caso de falhas

### Documentação

**Aprimoramentos:**
- **Postman Collections** exportadas e versionadas no repositório
- **Guia de contribuição** para novos desenvolvedores
- **Runbooks** para troubleshooting de cenários comuns
- **Architecture Decision Log** contínuo para novas decisões
- **API versioning** com estratégia clara de deprecação

---

> 💡 **Filosofia de desenvolvimento:** Prefiro entregar funcionalidades completas e bem testadas manualmente dentro do prazo do que código com falhas e testes superficiais. A arquitetura atual já está preparada para todas essas evoluções, com módulos bem definidos e baixo acoplamento ([ADR-001](./docs/decisions/ADR-001-arquitetura-modular.md)).
## 📊 Status do Projeto

✅ **Completo** - Case técnico desenvolvido para processo seletivo

**Autor:** Danrley Brasil dos Santos  
**Objetivo:** Demonstração de habilidades em desenvolvimento backend com Spring Boot

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).

---

**Desenvolvido com Spring Boot** ☕ | [Documentação](http://localhost:8080/swagger-ui.html) | [Issues](https://github.com/DanrleyBrasil/ecommerce-api-case/issues)