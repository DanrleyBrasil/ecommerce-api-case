.PHONY: help

.DEFAULT_GOAL := help

help: ## Mostra comandos disponíveis
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Docker Commands
up: ## Sobe MySQL via Docker Compose
	docker-compose up -d

down: ## Para e remove containers
	docker-compose down

logs: ## Mostra logs do MySQL
	docker-compose logs -f mysql

restart: ## Reinicia MySQL
	docker-compose restart mysql

clean-docker: ## Remove containers e volumes
	docker-compose down -v

# Maven Commands
clean: ## Limpa build
	./mvnw clean

compile: ## Compila projeto
	./mvnw compile

test: ## Roda testes unitários
	./mvnw test

test-coverage: ## Gera relatório de cobertura
	./mvnw clean test jacoco:report
	@echo "📊 Relatório em: target/site/jacoco/index.html"

package: ## Gera JAR
	./mvnw clean package -DskipTests

install: ## Instala dependências
	./mvnw clean install

# Application Commands
run: ## Roda aplicação (profile dev)
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

run-test: ## Roda aplicação (profile test)
	./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# Database Commands
db-reset: ## Reseta banco de dados
	docker-compose down -v
	docker-compose up -d
	@echo "⏳ Aguardando MySQL iniciar..."
	@sleep 10
	@echo "✅ Banco resetado com dump.sql"

# Combined Commands
setup: up ## Setup inicial (sobe MySQL)
	@echo "⏳ Aguardando MySQL iniciar..."
	@sleep 10
	@echo "✅ Setup concluído. Execute: make run"

dev: up run ## Setup + Run (ambiente completo)

# Quality Commands
format: ## Formata código
	./mvnw spring-javaformat:apply

verify: ## Verifica código
	./mvnw verify

# Info Commands
info: ## Mostra informações do projeto
	@echo "📦 Projeto: E-Commerce API"
	@echo "🔧 Java Version: 17"
	@echo "🚀 Spring Boot: 3.5.7"
	@echo "📊 Profile ativo: dev (padrão)"
	@echo ""
	@echo "Endpoints úteis:"
	@echo "  🌐 Aplicação: http://localhost:8080"
	@echo "  📖 Swagger: http://localhost:8080/swagger-ui.html"
	@echo "  ❤️  Health: http://localhost:8080/actuator/health"