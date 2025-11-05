.PHONY: help
help: ## Mostra comandos disponíveis
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST)

run-local: ## Roda aplicação local (sem Docker)
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

run-docker: ## Sobe ambiente completo com Docker
	docker-compose up -d

test: ## Roda todos os testes
	mvn clean test

test-integration: ## Roda testes de integração com TestContainers
	mvn test -Dtest="**/*IntegrationTest"

clean: ## Limpa containers e volumes
	docker-compose down -v

db-shell: ## Acessa MySQL via shell
	docker exec -it ecommerce_mysql mysql -u root -proot123

swagger: ## Abre Swagger UI
	open http://localhost:8080/swagger-ui.html