#!/bin/bash
echo "🚀 Iniciando ambiente de desenvolvimento..."
docker-compose up -d mysql
echo "⏳ Aguardando MySQL inicializar..."
sleep 10
echo "✅ Banco pronto! Iniciando aplicação..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev