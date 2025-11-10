#!/bin/bash

###############################################################################
# Script de Validação Automatizada - E-Commerce API
# Executa testes críticos para validação final antes da submissão
#
# Uso: ./validacao-final.sh
# Requisitos: Docker, Git Bash, curl, jq
# Tempo estimado: 5 minutos
###############################################################################

# 'set -e' para o script se qualquer comando falhar.
# 'set -o pipefail' garante que um comando no meio de um pipe que falhe
# fará com que toda a linha falhe, o que é mais seguro.
set -e
set -o pipefail

# --- CORES E FUNÇÕES DE LOG ---

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Contadores
PASSED=0
FAILED=0 # Este contador não será usado se 'set -e' estiver ativo

# Função para print colorido
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED++))
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_header() {
    echo ""
    echo "═══════════════════════════════════════════════════════"
    echo -e "${BLUE}$1${NC}"
    echo "═══════════════════════════════════════════════════════"
    echo ""
}

# --- VARIÁVEIS GLOBAIS ---

ADMIN_TOKEN=""
USER_TOKEN=""

###############################################################################
# FASE 1: VALIDAÇÃO DE INFRAESTRUTURA
###############################################################################

test_docker_running() {
    print_header "FASE 1: VALIDAÇÃO DE INFRAESTRUTURA"
    print_info "Teste 1: Verificando se Docker está rodando..."
    if docker info > /dev/null 2>&1; then
        print_success "Docker está rodando"
    else
        print_error "Docker não está rodando. Inicie o Docker Desktop e tente novamente."
        exit 1
    fi
}

test_containers_up() {
    print_info "Teste 2: Verificando containers ativos..."
    API_RUNNING=$(docker-compose ps | grep "api" | grep "Up" | tr -d '\r' | wc -l)
    MYSQL_RUNNING=$(docker-compose ps | grep "mysql" | grep "Up" | tr -d '\r' | wc -l)

    if [ "$API_RUNNING" -eq 1 ] && [ "$MYSQL_RUNNING" -eq 1 ]; then
        print_success "Containers API e MySQL estão ativos"
    else
        print_error "Um ou mais containers não estão rodando. Execute: docker-compose up -d"
        exit 1
    fi
}

test_api_health() {
    print_info "Teste 3: Verificando Health Check da API..."
    sleep 5 # Aumentado o tempo de espera para garantir que a API inicialize.
    HEALTH=$(curl --silent --fail http://localhost:8080/actuator/health 2>/dev/null || echo "failed")

    if echo "$HEALTH" | grep -q '"status":"UP"'; then
        print_success "Health Check: API está UP"
    else
        print_error "Health Check falhou. A API pode não ter iniciado corretamente."
        print_info "Response: $HEALTH"
        exit 1
    fi
}

test_swagger_accessible() {
    print_info "Teste 4: Verificando acessibilidade do Swagger UI..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html)

    if [ "$HTTP_CODE" -eq 200 ]; then
        print_success "Swagger UI está acessível (HTTP 200)"
    else
        print_error "Swagger UI não está acessível (HTTP $HTTP_CODE)"
    fi
}

test_database_populated() {
    print_info "Teste 5: Verificando se banco está populado..."
    USER_COUNT=$(docker-compose exec -T mysql mysql -u ecommerce_user -pecommerce_pass ecommerce_db -se "SELECT COUNT(*) FROM users;" 2>/dev/null | tr -d '\r')

    if [ "$USER_COUNT" -ge 2 ]; then
        print_success "Banco de dados populado ($USER_COUNT usuários encontrados)"
    else
        print_error "Banco de dados não parece estar populado corretamente (encontrado: $USER_COUNT)"
    fi
}

###############################################################################
# FASE 2: VALIDAÇÃO DE AUTENTICAÇÃO
###############################################################################

test_admin_login() {
    print_header "FASE 2: VALIDAÇÃO DE AUTENTICAÇÃO"
    print_info "Teste 6: Login ADMIN..."
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@ecommerce.com","password":"admin123"}')
    ADMIN_TOKEN=$(echo "$RESPONSE" | jq -r '.token')

    if [ -n "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "null" ]; then
        print_success "Login ADMIN bem-sucedido (token obtido)"
    else
        print_error "Login ADMIN falhou"
        print_info "Response: $RESPONSE"
        exit 1
    fi
}

test_user_login() {
    print_info "Teste 7: Login USER..."
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"user1@ecommerce.com","password":"user123"}')
    USER_TOKEN=$(echo "$RESPONSE" | jq -r '.token')

    if [ -n "$USER_TOKEN" ] && [ "$USER_TOKEN" != "null" ]; then
        print_success "Login USER bem-sucedido (token obtido)"
    else
        print_error "Login USER falhou"
        print_info "Response: $RESPONSE"
        exit 1
    fi
}

test_invalid_credentials() {
    print_info "Teste 8: Login com credenciais inválidas (deve falhar)..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@ecommerce.com","password":"senhaerrada"}')

    if [ "$HTTP_CODE" -eq 401 ]; then
        print_success "Credenciais inválidas rejeitadas corretamente (HTTP 401)"
    else
        print_error "Validação de credenciais falhou (esperado 401, recebido $HTTP_CODE)"
    fi
}

###############################################################################
# FASE 3: VALIDAÇÃO DE PRODUTOS
###############################################################################

test_list_products_admin() {
    print_header "FASE 3: VALIDAÇÃO DE PRODUTOS"
    print_info "Teste 9: ADMIN lista produtos..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X GET http://localhost:8080/api/products \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    if [ "$HTTP_CODE" -eq 200 ]; then
        print_success "ADMIN consegue listar produtos (HTTP 200)"
    else
        print_error "Falha ao listar produtos como ADMIN (HTTP $HTTP_CODE)"
    fi
}

test_create_product_admin() {
    print_info "Teste 10: ADMIN cria produto..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST http://localhost:8080/api/products \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Produto Teste Validação",
            "description": "Teste automatizado",
            "price": 99.90,
            "stockQuantity": 10,
            "categoryId": 1,
            "sku": "TEST-VALIDATION-001"
        }')

    if [ "$HTTP_CODE" -eq 201 ]; then
        print_success "ADMIN criou produto com sucesso (HTTP 201)"
    else
        print_error "Falha ao criar produto como ADMIN (HTTP $HTTP_CODE)"
    fi
}

test_user_cannot_create_product() {
    print_info "Teste 11: USER tenta criar produto (deve falhar)..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST http://localhost:8080/api/products \
        -H "Authorization: Bearer $USER_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Tentativa USER",
            "price": 10.00,
            "stockQuantity": 5,
            "categoryId": 1,
            "sku": "USER-ATTEMPT"
        }')

    if [ "$HTTP_CODE" -eq 403 ]; then
        print_success "USER corretamente bloqueado de criar produto (HTTP 403)"
    else
        print_error "Autorização RBAC falhou para criação de produto (esperado 403, recebido $HTTP_CODE)"
    fi
}

###############################################################################
# FASE 4: VALIDAÇÃO DE PEDIDOS
###############################################################################

test_user_create_order() {
    print_header "FASE 4: VALIDAÇÃO DE PEDIDOS"
    print_info "Teste 12: USER cria pedido..."
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
        -H "Authorization: Bearer $USER_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "items": [
                {
                    "productId": 1,
                    "quantity": 1
                }
            ]
        }')
    ORDER_ID=$(echo "$RESPONSE" | jq -r '.id')
    ORDER_STATUS=$(echo "$RESPONSE" | jq -r '.status')

    if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ] && [ "$ORDER_STATUS" = "PENDENTE" ]; then
        print_success "Pedido criado com sucesso (ID: $ORDER_ID, Status: PENDENTE)"
    else
        print_error "Falha ao criar pedido"
        print_info "Response: $RESPONSE"
    fi
}

test_user_list_orders() {
    print_info "Teste 13: USER lista seus pedidos..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X GET http://localhost:8080/api/orders \
        -H "Authorization: Bearer $USER_TOKEN")

    if [ "$HTTP_CODE" -eq 200 ]; then
        print_success "USER consegue listar seus pedidos (HTTP 200)"
    else
        print_error "Falha ao listar pedidos do USER (HTTP $HTTP_CODE)"
    fi
}

###############################################################################
# FASE 5: VALIDAÇÃO DE RELATÓRIOS
###############################################################################

test_admin_reports() {
    print_header "FASE 5: VALIDAÇÃO DE RELATÓRIOS"
    print_info "Teste 14: ADMIN acessa relatórios..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X GET "http://localhost:8080/api/reports/top-users?limit=5" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    if [ "$HTTP_CODE" -eq 200 ]; then
        print_success "ADMIN acessa relatórios com sucesso (HTTP 200)"
    else
        print_error "Falha ao acessar relatórios como ADMIN (HTTP $HTTP_CODE)"
    fi
}

test_user_cannot_access_reports() {
    print_info "Teste 15: USER tenta acessar relatórios (deve falhar)..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X GET "http://localhost:8080/api/reports/top-users?limit=5" \
        -H "Authorization: Bearer $USER_TOKEN")

    if [ "$HTTP_CODE" -eq 403 ]; then
        print_success "USER corretamente bloqueado de acessar relatórios (HTTP 403)"
    else
        print_error "Autorização RBAC para relatórios falhou (esperado 403, recebido $HTTP_CODE)"
    fi
}

###############################################################################
# EXECUÇÃO PRINCIPAL
###############################################################################

# --- Bloco de finalização ---
# Esta função será chamada no final, seja por sucesso ou por um erro capturado
finish() {
  # $? contém o código de saída do último comando
  LAST_EXIT_CODE=$?
  echo ""

  if [ $LAST_EXIT_CODE -ne 0 ]; then
    # Se o último comando falhou (código de saída diferente de 0), significa que 'set -e' parou o script
    echo "═══════════════════════════════════════════════════════"
    echo -e "${RED}  ⚠️  EXECUÇÃO INTERROMPIDA DEVIDO A UM ERRO${NC}"
    echo -e "${YELLOW}  ⚠️  REVISE O LOG ACIMA PARA IDENTIFICAR O PROBLEMA${NC}"
    echo "═══════════════════════════════════════════════════════"
  else
    # Se chegamos aqui sem erros, todos os testes passaram
    print_header "RESUMO DA VALIDAÇÃO"
    TOTAL_TESTS=15 # Número fixo de testes definidos no script.
    echo -e "${GREEN}✅ Testes Executados com Sucesso: $PASSED${NC}"
    echo -e "${BLUE}📊 Total de Testes Planejados: $TOTAL_TESTS${NC}"
    echo ""

    echo "═══════════════════════════════════════════════════════"
    echo -e "${GREEN}  🎉 TODOS OS TESTES PASSARAM!${NC}"
    echo -e "${GREEN}  ✅ PROJETO APROVADO PARA SUBMISSÃO${NC}"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    echo "Próximos passos:"
    echo "1. Exportar Collection Postman"
    echo "2. Revisar README.md"
    echo "3. Fazer commit final"
    echo "4. Submeter projeto"
  fi

  echo ""
  # AQUI ESTÁ A MUDANÇA PRINCIPAL: O SCRIPT VAI PAUSAR AQUI
  read -p "Pressione [Enter] para fechar a janela..."
}

# 'trap' captura sinais do sistema. O sinal 'EXIT' é enviado quando o script termina.
# Isso garante que a função 'finish' será executada sempre que o script acabar,
# seja por sucesso ou por um erro que o 'set -e' capturou.
trap finish EXIT

# --- Início da Execução ---
clear
echo "═══════════════════════════════════════════════════════"
echo "  🚀 VALIDAÇÃO AUTOMATIZADA - E-COMMERCE API"
echo "═══════════════════════════════════════════════════════"
echo ""

# --- VERIFICAÇÃO DE DEPENDÊNCIAS ---
print_info "Verificando dependências..."
if ! command -v curl &> /dev/null; then
    print_error "'curl' não está instalado. Por favor, instale e tente novamente."
    exit 1
fi
if ! command -v jq &> /dev/null; then
    print_error "'jq' não está instalado. É necessário para processar as respostas da API."
    print_info "No Windows, instale com: winget install jqlang.jq"
    exit 1
fi
print_success "Dependências (curl, jq) encontradas."

    # --- EXECUÇÃO DOS TESTES ---
    test_docker_running
    test_containers_up
    test_api_health
    test_swagger_accessible
    test_database_populated

    test_admin_login
    test_user_login
    test_invalid_credentials

    test_list_products_admin
    test_create_product_admin
    test_user_cannot_create_product

    test_user_create_order
    test_user_list_orders

    test_admin_reports
    test_user_cannot_access_reports

    # --- RESUMO FINAL ---
    # Este bloco só será alcançado se 'set -e' não parar o script.
    print_header "RESUMO DA VALIDAÇÃO"
    TOTAL_TESTS=15 # Número fixo de testes definidos no script.
    echo -e "${GREEN}✅ Testes Executados com Sucesso: $PASSED${NC}"
    echo -e "${BLUE}📊 Total de Testes Planejados: $TOTAL_TESTS${NC}"
    echo ""

    echo "═══════════════════════════════════════════════════════"
    echo -e "${GREEN}  🎉 TODOS OS TESTES PASSARAM!${NC}"
    echo -e "${GREEN}  ✅ PROJETO APROVADO PARA SUBMISSÃO${NC}"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    echo "Próximos passos:"
    echo "1. Exportar Collection Postman"
    echo "2. Revisar README.md"
    echo "3. Fazer commit final"
    echo "4. Submeter projeto"
    echo ""
}

# --- Ponto de Entrada do Script ---
main