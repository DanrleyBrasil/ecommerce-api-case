-- =========================================
-- E-COMMERCE API - DATABASE DUMP
-- =========================================
-- Autor: Danrley Brasil dos Santos
-- Data: 06/11/2025
-- Versão: 1.1 (Adicionado controle de reserva)
-- Descrição: Estrutura completa + dados de teste
-- =========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Dropar tabelas existentes (ordem inversa das FKs)
DROP TABLE IF EXISTS product_price_history;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

-- =========================================
-- DOMÍNIO: AUTENTICAÇÃO (RBAC)
-- =========================================

CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL COMMENT 'BCrypt hash',
                       active BOOLEAN NOT NULL DEFAULT true,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       created_by VARCHAR(100),
                       updated_by VARCHAR(100),

                       INDEX idx_users_email (email),
                       INDEX idx_users_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Usuários do sistema';

CREATE TABLE roles (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       description VARCHAR(255),
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Perfis de acesso';

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,

                            INDEX idx_user_roles_user (user_id),
                            INDEX idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Associação N:N entre usuários e roles';

-- =========================================
-- DOMÍNIO: CATÁLOGO (Normalizado)
-- =========================================

CREATE TABLE categories (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            active BOOLEAN NOT NULL DEFAULT true,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            INDEX idx_categories_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Categorias de produtos';

CREATE TABLE suppliers (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           name VARCHAR(100) NOT NULL,
                           cnpj VARCHAR(18),
                           email VARCHAR(100),
                           phone VARCHAR(20),
                           active BOOLEAN NOT NULL DEFAULT true,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                           INDEX idx_suppliers_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fornecedores de produtos';

CREATE TABLE products (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          name VARCHAR(200) NOT NULL,
                          description TEXT,
                          price DECIMAL(10, 2) NOT NULL,
                          stock_quantity INT NOT NULL DEFAULT 0,
                          reserved_quantity INT NOT NULL DEFAULT 0 COMMENT 'Estoque reservado temporariamente (ADR-003)',
                          category_id BIGINT NOT NULL COMMENT 'Categoria obrigatória',
                          supplier_id BIGINT NULL COMMENT 'Fornecedor opcional - NULL permitido',
                          sku VARCHAR(50) UNIQUE,
                          active BOOLEAN NOT NULL DEFAULT true,
                          metadata JSON COMMENT 'Metadados flexíveis do produto (marca, modelo, specs)',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          created_by VARCHAR(100),
                          updated_by VARCHAR(100),

                          FOREIGN KEY (category_id) REFERENCES categories(id),
                          FOREIGN KEY (supplier_id) REFERENCES suppliers(id),

                          INDEX idx_products_category (category_id),
                          INDEX idx_products_supplier (supplier_id),
                          INDEX idx_products_sku (sku),
                          INDEX idx_products_active (active),

                          CONSTRAINT chk_price_positive CHECK (price >= 0),
                          CONSTRAINT chk_stock_nonnegative CHECK (stock_quantity >= 0),
                          CONSTRAINT chk_reserved_nonnegative CHECK (reserved_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Produtos do catálogo';

-- =========================================
-- DOMÍNIO: PEDIDOS
-- =========================================

CREATE TABLE orders (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        status VARCHAR(20) NOT NULL COMMENT 'PENDENTE, APROVADO, CANCELADO, EXPIRED',
                        total_amount DECIMAL(10, 2) NOT NULL,
                        order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        payment_date TIMESTAMP NULL,
                        reserved_until TIMESTAMP NULL COMMENT 'TTL da reserva de estoque (ADR-003)',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        created_by VARCHAR(100),
                        updated_by VARCHAR(100),

                        FOREIGN KEY (user_id) REFERENCES users(id),

                        INDEX idx_orders_user_id (user_id),
                        INDEX idx_orders_status (status),
                        INDEX idx_orders_date (order_date),
                        INDEX idx_orders_reserved_until (reserved_until),

                        CONSTRAINT chk_total_positive CHECK (total_amount >= 0),
                        CONSTRAINT chk_status_valid CHECK (status IN ('PENDENTE', 'APROVADO', 'CANCELADO', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Pedidos de clientes';

CREATE TABLE order_items (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(10, 2) NOT NULL COMMENT 'Preço no momento da compra',
                             subtotal DECIMAL(10, 2) NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             created_by VARCHAR(100),
                             updated_by VARCHAR(100),

                             FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             FOREIGN KEY (product_id) REFERENCES products(id),

                             INDEX idx_order_items_order (order_id),
                             INDEX idx_order_items_product (product_id),

                             CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
                             CONSTRAINT chk_unit_price_positive CHECK (unit_price >= 0),
                             CONSTRAINT chk_subtotal_positive CHECK (subtotal >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Itens dos pedidos';

-- =========================================
-- DOMÍNIO: AUDITORIA (Histórico de Preços)
-- =========================================

CREATE TABLE product_price_history (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       product_id BIGINT NOT NULL,
                                       old_price DECIMAL(10, 2) NOT NULL,
                                       new_price DECIMAL(10, 2) NOT NULL,
                                       changed_by VARCHAR(100) NOT NULL,
                                       changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       reason VARCHAR(255) COMMENT 'Motivo da mudança: promoção, ajuste, etc',

                                       FOREIGN KEY (product_id) REFERENCES products(id),
                                       INDEX idx_product_price_product (product_id),
                                       INDEX idx_product_price_date (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Histórico de mudanças de preço';

-- =========================================
-- INSERÇÃO DE DADOS
-- =========================================

-- -----------------------------------------
-- ROLES (2 perfis)
-- -----------------------------------------
INSERT INTO roles (id, name, description) VALUES
                                              (1, 'ADMIN', 'Administrador do sistema - acesso total'),
                                              (2, 'USER', 'Usuário cliente - acesso às compras');

-- -----------------------------------------
-- USERS (11 usuários: 1 ADMIN + 10 USERS)
-- Senha padrão: Admin@123 (admin) | User@123 (users)
-- -----------------------------------------
INSERT INTO users (name, email, password, active, created_by) VALUES
                                                                  ('Administrador', 'admin@ecommerce.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, 'SYSTEM'),
                                                                  ('João Silva', 'user1@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Maria Santos', 'user2@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Pedro Oliveira', 'user3@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Ana Costa', 'user4@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Lucas Almeida', 'user5@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Carla Ferreira', 'user6@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Rafael Souza', 'user7@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Fernanda Lima', 'user8@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Gustavo Rocha', 'user9@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM'),
                                                                  ('Juliana Martins', 'user10@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, 'SYSTEM');

-- -----------------------------------------
-- USER_ROLES (associações)
-- -----------------------------------------
INSERT INTO user_roles (user_id, role_id) VALUES
                                              (1, 1), -- Admin tem role ADMIN
                                              (2, 2), -- João tem role USER
                                              (3, 2), -- Maria tem role USER
                                              (4, 2), -- Pedro tem role USER
                                              (5, 2), -- Ana tem role USER
                                              (6, 2), -- Lucas tem role USER
                                              (7, 2), -- Carla tem role USER
                                              (8, 2), -- Rafael tem role USER
                                              (9, 2), -- Fernanda tem role USER
                                              (10, 2), -- Gustavo tem role USER
                                              (11, 2); -- Juliana tem role USER

-- -----------------------------------------
-- CATEGORIES (5 categorias)
-- -----------------------------------------
INSERT INTO categories (name, description) VALUES
                                               ('PERIFERICOS', 'Mouses, teclados, headsets e webcams'),
                                               ('COMPONENTES', 'Processadores, placas de vídeo, memórias RAM'),
                                               ('MONITORES', 'Monitores e displays'),
                                               ('ARMAZENAMENTO', 'SSDs, HDs externos e pen drives'),
                                               ('ACESSORIOS', 'Cabos, hubs, suportes e outros');

-- -----------------------------------------
-- SUPPLIERS (5 fornecedores reais)
-- -----------------------------------------
INSERT INTO suppliers (name, cnpj, email, phone) VALUES
                                                     ('Logitech Brasil', '12.345.678/0001-90', 'contato@logitech.com.br', '(11) 3000-1000'),
                                                     ('AMD do Brasil', '23.456.789/0001-01', 'vendas@amd.com.br', '(11) 3000-2000'),
                                                     ('Corsair Gaming', '34.567.890/0001-12', 'brasil@corsair.com', '(11) 3000-3000'),
                                                     ('Kingston Technology', '45.678.901/0001-23', 'suporte@kingston.com.br', '(11) 3000-4000'),
                                                     ('LG Electronics', '56.789.012/0001-34', 'b2b@lg.com.br', '(11) 3000-5000');

-- -----------------------------------------
-- PRODUCTS (12 produtos - reserved_quantity = 0 inicialmente)
-- -----------------------------------------
INSERT INTO products (name, description, price, stock_quantity, reserved_quantity, category_id, supplier_id, sku, metadata, created_by) VALUES
-- PERIFÉRICOS
('Mouse Gamer Logitech G203', 'Mouse gamer RGB com sensor óptico de 8.000 DPI, 6 botões programáveis', 149.90, 50, 0, 1, 1, 'LOG-G203-BK',
 '{"brand": "Logitech", "model": "G203", "color": "Preto", "dpi": 8000, "buttons": 6, "connection": "USB", "warranty_months": 12}', 'admin@ecommerce.com'),

('Teclado Mecânico HyperX Alloy Origins', 'Teclado mecânico RGB com switches HyperX Red, estrutura em alumínio', 489.90, 30, 0, 1, 3, 'HYX-ALLOY-RED',
 '{"brand": "HyperX", "model": "Alloy Origins", "color": "Preto", "switch_type": "HyperX Red", "connection": "USB-C", "warranty_months": 24}', 'admin@ecommerce.com'),

('Headset Gamer Corsair HS60 Pro', 'Headset surround 7.1 com drivers de 50mm e microfone removível', 399.90, 25, 0, 1, 3, 'COR-HS60-BK',
 '{"brand": "Corsair", "model": "HS60 Pro", "color": "Preto", "connection": "USB", "surround": "7.1", "warranty_months": 24}', 'admin@ecommerce.com'),

('Webcam Logitech C920 HD Pro', 'Webcam Full HD 1080p com microfone estéreo e foco automático', 449.90, 15, 0, 1, 1, 'LOG-C920-HD',
 '{"brand": "Logitech", "model": "C920", "resolution": "1080p", "fps": 30, "connection": "USB", "warranty_months": 12}', 'admin@ecommerce.com'),

-- COMPONENTES
('Processador AMD Ryzen 5 5600X', 'Processador 6-core/12-threads, 3.7GHz base/4.6GHz boost, Socket AM4', 1299.90, 8, 0, 2, 2, 'AMD-R5-5600X',
 '{"brand": "AMD", "model": "Ryzen 5 5600X", "cores": 6, "threads": 12, "base_clock_ghz": 3.7, "boost_clock_ghz": 4.6, "tdp_watts": 65, "warranty_months": 36}', 'admin@ecommerce.com'),

('Placa de Vídeo RTX 3060 12GB', 'GPU NVIDIA GeForce RTX 3060 com 12GB GDDR6, Ray Tracing e DLSS', 2499.90, 5, 0, 2, 2, 'NV-RTX3060-12G',
 '{"brand": "NVIDIA", "model": "RTX 3060", "memory_gb": 12, "memory_type": "GDDR6", "ray_tracing": true, "warranty_months": 36}', 'admin@ecommerce.com'),

('Memória RAM Kingston 16GB DDR4', 'Memória DDR4 16GB 3200MHz, CL16, HyperX Fury', 349.90, 40, 0, 2, 4, 'KNG-HX-16GB',
 '{"brand": "Kingston", "model": "HyperX Fury", "capacity_gb": 16, "type": "DDR4", "speed_mhz": 3200, "warranty_months": 36}', 'admin@ecommerce.com'),

-- MONITORES
('Monitor LG 24 UltraGear Full HD', 'Monitor gamer 24 polegadas, 144Hz, IPS, 1ms, FreeSync', 899.90, 10, 0, 3, 5, 'LG-24-144HZ',
 '{"brand": "LG", "model": "24GL600F", "size_inches": 24, "resolution": "1920x1080", "refresh_rate_hz": 144, "panel_type": "IPS", "warranty_months": 12}', 'admin@ecommerce.com'),

-- ARMAZENAMENTO
('SSD Kingston NV2 1TB NVMe', 'SSD M.2 NVMe PCIe 4.0, leitura 3500MB/s, gravação 2100MB/s', 499.90, 20, 0, 4, 4, 'KNG-NV2-1TB',
 '{"brand": "Kingston", "model": "NV2", "capacity_tb": 1, "interface": "NVMe PCIe 4.0", "read_speed_mbps": 3500, "write_speed_mbps": 2100, "warranty_months": 36}', 'admin@ecommerce.com'),

('SSD Kingston A400 480GB SATA', 'SSD 2.5 SATA III, leitura 500MB/s, gravação 450MB/s', 289.90, 35, 0, 4, 4, 'KNG-A400-480G',
 '{"brand": "Kingston", "model": "A400", "capacity_gb": 480, "interface": "SATA III", "read_speed_mbps": 500, "write_speed_mbps": 450, "warranty_months": 36}', 'admin@ecommerce.com'),

-- ACESSÓRIOS
('Mousepad Gamer Corsair MM300', 'Mousepad extended com superfície de tecido e base antiderrapante', 129.90, 60, 0, 5, 3, 'COR-MM300-XL',
 '{"brand": "Corsair", "model": "MM300", "size": "Extended", "material": "Tecido", "color": "Preto"}', 'admin@ecommerce.com'),

('Hub USB 3.0 Kingston 4 Portas', 'Hub USB 3.0 com 4 portas, retrocompatível USB 2.0', 89.90, 45, 0, 5, 4, 'KNG-HUB-4P',
 '{"brand": "Kingston", "model": "USB Hub", "ports": 4, "usb_version": "3.0", "cable_length_cm": 80}', 'admin@ecommerce.com');

-- -----------------------------------------
-- ORDERS (8 pedidos: 5 APROVADOS + 3 PENDENTES)
-- -----------------------------------------

-- Pedidos APROVADOS (reserved_until = NULL, reserva já liberada)
INSERT INTO orders (user_id, status, total_amount, order_date, payment_date, reserved_until, created_by) VALUES
                                                                                                             (2, 'APROVADO', 939.70, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL, 'user1@test.com'),
                                                                                                             (3, 'APROVADO', 2799.80, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL, 'user2@test.com'),
                                                                                                             (4, 'APROVADO', 1599.80, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NULL, 'user3@test.com'),
                                                                                                             (5, 'APROVADO', 549.80, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 'user4@test.com'),
                                                                                                             (6, 'APROVADO', 3149.70, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 'user5@test.com');

-- Pedidos PENDENTES (reserved_until = now + 10min, simulando reserva ativa)
INSERT INTO orders (user_id, status, total_amount, order_date, payment_date, reserved_until, created_by) VALUES
                                                                                                             (7, 'PENDENTE', 839.80, NOW(), NULL, DATE_ADD(NOW(), INTERVAL 10 MINUTE), 'user6@test.com'),
                                                                                                             (8, 'PENDENTE', 449.90, NOW(), NULL, DATE_ADD(NOW(), INTERVAL 10 MINUTE), 'user7@test.com'),
                                                                                                             (9, 'PENDENTE', 1649.80, NOW(), NULL, DATE_ADD(NOW(), INTERVAL 10 MINUTE), 'user8@test.com');

-- -----------------------------------------
-- ORDER_ITEMS (itens dos pedidos)
-- -----------------------------------------

-- Pedido #1 (João - APROVADO): Mouse + Teclado + Mousepad
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
                                                                                               (1, 1, 2, 149.90, 299.80, 'user1@test.com'),
                                                                                               (1, 2, 1, 489.90, 489.90, 'user1@test.com'),
                                                                                               (1, 11, 1, 129.90, 129.90, 'user1@test.com');

-- Pedido #2 (Maria - APROVADO): RTX 3060 + Mousepad
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
                                                                                               (2, 6, 1, 2499.90, 2499.90, 'user2@test.com'),
                                                                                               (2, 11, 1, 129.90, 129.90, 'user2@test.com');

-- Pedido #3 (Pedro - APROVADO): Ryzen 5 + RAM
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
                                                                                               (3, 5, 1, 1299.90, 1299.90, 'user3@test.com'),
                                                                                               (3, 7, 1, 349.90, 349.90, 'user3@test.com');

-- Pedido #4 (Ana - APROVADO): SSD NVMe + Hub
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
                                                                                               (4, 9, 1, 499.90, 499.90, 'user4@test.com'),
                                                                                               (4, 12, 1, 89.90, 89.90, 'user4@test.com');

-- Pedido #5 (Lucas - APROVADO): RTX 3060 + Headset + Mouse
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
                                                                                               (5, 6, 1, 2499.90, 2499.90, 'user5@test.com'),
                                                                                               (5, 3, 1, 399.90, 399.90, 'user5@test.com'),
                                                                                               (5, 1, 1, 149.90, 149.90, 'user5@test.com');

-- Pedido #6 (Carla - PENDENTE): Monitor + Mousepad
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
                                                                                               (6, 8, 1, 899.90, 899.90, 'user6@test.com'),
                                                                                               (6, 11, 1, 129.90, 129.90, 'user6@test.com');

-- Pedido #7 (Rafael - PENDENTE): Webcam
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
    (7, 4, 1, 449.90, 449.90, 'user7@test.com');

-- Pedido #8 (Fernanda - PENDENTE): Ryzen 5 + RAM
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, created_by) VALUES
                                                                                               (8, 5, 1, 1299.90, 1299.90, 'user8@test.com'),
                                                                                               (8, 7, 1, 349.90, 349.90, 'user8@test.com');

-- Corrigir totais
UPDATE orders SET total_amount = 919.60 WHERE id = 1;
UPDATE orders SET total_amount = 2629.80 WHERE id = 2;
UPDATE orders SET total_amount = 589.80 WHERE id = 4;
UPDATE orders SET total_amount = 3049.70 WHERE id = 5;
UPDATE orders SET total_amount = 1029.80 WHERE id = 6;

-- -----------------------------------------
-- PRODUCT_PRICE_HISTORY (histórico de preços)
-- -----------------------------------------

INSERT INTO product_price_history (product_id, old_price, new_price, changed_by, changed_at, reason) VALUES
                                                                                                         (1, 179.90, 149.90, 'admin@ecommerce.com', DATE_SUB(NOW(), INTERVAL 10 DAY), 'Promoção de novembro'),
                                                                                                         (2, 519.90, 489.90, 'admin@ecommerce.com', DATE_SUB(NOW(), INTERVAL 15 DAY), 'Ajuste competitivo de preço'),
                                                                                                         (6, 2799.90, 2499.90, 'admin@ecommerce.com', DATE_SUB(NOW(), INTERVAL 20 DAY), 'Redução sazonal - baixa demanda'),
                                                                                                         (8, 999.90, 899.90, 'admin@ecommerce.com', DATE_SUB(NOW(), INTERVAL 5 DAY), 'Black November - promoção de lançamento'),
                                                                                                         (9, 549.90, 499.90, 'admin@ecommerce.com', DATE_SUB(NOW(), INTERVAL 12 DAY), 'Liquidação de estoque antigo');

-- =========================================
-- VALIDAÇÕES E INTEGRIDADE
-- =========================================

SELECT
    o.id,
    o.total_amount as declared_total,
    SUM(oi.subtotal) as calculated_total,
    CASE
        WHEN ABS(o.total_amount - SUM(oi.subtotal)) < 0.01 THEN 'OK'
        ELSE 'ERRO: Inconsistência!'
        END as validation_status
FROM orders o
         JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id;

-- =========================================
-- ESTATÍSTICAS DO BANCO
-- =========================================

SELECT 'users' as tabela, COUNT(*) as registros FROM users
UNION ALL
SELECT 'roles', COUNT(*) FROM roles
UNION ALL
SELECT 'user_roles', COUNT(*) FROM user_roles
UNION ALL
SELECT 'categories', COUNT(*) FROM categories
UNION ALL
SELECT 'suppliers', COUNT(*) FROM suppliers
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'product_price_history', COUNT(*) FROM product_price_history;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================
-- FIM DO DUMP
-- =========================================
-- Versão: 1.1
-- Alterações: Adicionado controle de reserva (ADR-003)
--   - products.reserved_quantity
--   - orders.reserved_until
--   - orders.status CHECK constraint com EXPIRED
-- =========================================