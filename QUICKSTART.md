# 🚀 Quick Start Guide

Rode a aplicação completa em **2 minutos**.

## ⚡ Passo Único
```bash
git clone https://github.com/DanrleyBrasil/ecommerce-api-case.git
cd ecommerce-api-case
docker-compose up -d
```

**Pronto!** Aguarde ~30 segundos para inicialização.

## 🌐 Acessar a Aplicação

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Base:** http://localhost:8080
- **Health Check:** http://localhost:8080/actuator/health

## 🔐 Credenciais de Teste

A aplicação vem com usuários pré-cadastrados:

| Email | Senha | Perfil |
|-------|-------|--------|
| admin@ecommerce.com | admin123 | ADMIN |
| user1@ecommerce.com | user123 | USER |
| user2@ecommerce.com | user123 | USER |

## 🧪 Testar Rapidamente

### 1. Obter token (via Swagger)

Acesse http://localhost:8080/swagger-ui.html → `/api/auth/login`

**Body:**
```json
{
  "email": "admin@ecommerce.com",
  "password": "admin123"
}
```

### 2. Usar o token

Clique em **"Authorize"** no topo do Swagger e cole o token retornado.

**Agora você pode testar todos os endpoints autenticado!**

## 🛑 Parar a Aplicação
```bash
# Parar containers
docker-compose down

# Parar e limpar banco de dados
docker-compose down -v
```

## ⚙️ Configuração Avançada (Opcional)

Para customizar portas, senhas ou outros parâmetros:
```bash
cp .env.example .env
# Edite o .env conforme necessário
docker-compose restart
```

## ❓ Problemas?

### Porta 8080 em uso
```bash
# No .env, altere:
SERVER_PORT=8081
```

### Banco não sobe
```bash
docker-compose logs mysql
docker-compose down -v && docker-compose up -d
```

### Permissão negada (Linux)
```bash
sudo usermod -aG docker $USER
# Logout e login novamente
```

## 📚 Documentação Completa

Para arquitetura, decisões técnicas e detalhes: [README.md](README.md)