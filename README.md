# E-Commerce API

API RESTful para gerenciamento de e-commerce com autenticação JWT, CRUD de produtos e fluxo completo de pedidos com processamento de pagamentos.

## 🚀 Tecnologias

- Java 17
- Spring Boot 3.5.7
- Spring Security + JWT
- Spring Data JPA
- MySQL 8.0
- Docker & Docker Compose
- Maven
- Swagger/OpenAPI 3.0

## 📋 Pré-requisitos

- Docker 20.10+
- Docker Compose 2.0+
- Git

## ⚡ Início Rápido
```bash
git clone https://github.com/DanrleyBrasil/ecommerce-api-case.git
cd ecommerce-api-case
docker-compose up -d
```

**Pronto!** Acesse http://localhost:8080/swagger-ui.html

Para guia detalhado, veja [QUICKSTART.md](QUICKSTART.md)

## 📚 Documentação da API

Acesse a documentação interativa Swagger:
```
http://localhost:8080/swagger-ui.html
```

## 🔐 Autenticação

A API utiliza **JWT (JSON Web Token)** para autenticação.

### Obter Token

**Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "email": "user@master.com",
  "password": "senha123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJyb2x...",
  "type": "Bearer",
  "userId": 12,
  "name": "User Teste",
  "email": "user@master.com",
  "roles": [
    "USER"
  ]
}
```

### Usar Token nas Requisições

Inclua o token no header `Authorization`:
```
Authorization: Bearer {seu-token-jwt}
```

**Exemplo com curl:**
```bash
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Usuários de Teste

| Email | Senha    | Perfil |
|-------|----------|--------|
| admin@ecommerce.com | senha123 | ADMIN |
| user1@ecommerce.com | senha123  | USER |

Banco de dados vem pré-populado com 14 produtos e 7 pedidos de exemplo.

## 🔒 Decisões Técnicas

### Configuração de Ambiente

**Para este case técnico/demonstração:**
- ✅ Valores default configurados (funciona out-of-the-box)
- ✅ Senhas e secrets hardcoded em `application.yml` e `docker-compose.yml`
- ✅ Foco em facilitar avaliação e testes

**⚠️ Em ambiente de produção:**
- ❌ NUNCA usar valores default em produção
- ✅ Variáveis de ambiente obrigatórias via `.env` ou secrets manager
- ✅ Secrets gerenciados (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault)
- ✅ Diferentes configurações por ambiente (dev/staging/prod)
- ✅ Rotação automática de secrets
- ✅ Auditoria de acesso a credenciais

Para customização local (opcional), veja `.env.example`

### Autenticação via Header Authorization

Este projeto utiliza o padrão **Authorization Bearer** para transmissão de tokens JWT.

**Alternativa considerada:** Uso de cookies `httpOnly` + proteção CSRF seria mais seguro contra ataques XSS, pois o JavaScript não teria acesso ao token. Essa abordagem é recomendada para ambientes de produção, especialmente em aplicações com frontend integrado.

Para este case técnico, optou-se pelo padrão `Authorization Header` por:
- Melhor compatibilidade com ferramentas de teste (Swagger UI, Postman)
- Simplicidade de implementação no prazo do case
- Padrão amplamente adotado em APIs RESTful

## 🧪 Executar Testes
```bash
docker-compose exec api mvn test
```

## 📦 Build Manual

Se necessário, para gerar o `.jar`:
```bash
mvn clean package
```

O arquivo será gerado em `target/ecommerce-api-0.0.1-SNAPSHOT.jar`

## 🛑 Parar a Aplicação
```bash
# Parar containers
docker-compose down

# Parar e limpar volumes (banco de dados)
docker-compose down -v
```

## 🚀 Evoluções Futuras

Pensando em cenários de produção e escalabilidade, as seguintes evoluções são recomendadas:

### Arquitetura
- Migração para **arquitetura de microserviços**
    - Separação em serviços: Auth, Products, Orders, Payments, Reports
    - Comunicação assíncrona via mensageria (RabbitMQ/Kafka)
    - Event-driven architecture

### Orquestração e Escalabilidade
- **Kubernetes** para orquestração de containers
    - Deployments com ReplicaSets
    - Horizontal Pod Autoscaler (HPA)
    - Load balancing automático
    - Health checks e self-healing

### Infraestrutura
- **Service Mesh** (Istio/Linkerd) para controle de tráfego
- **API Gateway** (Kong/AWS API Gateway) como ponto de entrada único
- **Cache distribuído** (Redis) para melhor performance
- **CDN** para assets estáticos

### Observabilidade
- Distributed tracing (Jaeger/Zipkin)
- Centralized logging (ELK Stack)
- Advanced monitoring (Prometheus + Grafana)

### Segurança
- Secrets management (Vault/AWS Secrets Manager)
- Certificate management automatizado
- Rate limiting por usuário/IP
- WAF (Web Application Firewall)

## 📊 Status do Projeto

🚧 **Em desenvolvimento** - Case técnico para vaga de Desenvolvedor Backend Pleno

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).
