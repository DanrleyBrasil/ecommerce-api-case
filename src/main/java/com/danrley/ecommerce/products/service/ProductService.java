package com.danrley.ecommerce.products.service;

import com.danrley.ecommerce.products.dto.ProductFilterRequest;
import com.danrley.ecommerce.products.dto.ProductRequest;
import com.danrley.ecommerce.products.dto.ProductResponse;
import com.danrley.ecommerce.products.entity.Category;
import com.danrley.ecommerce.products.entity.Product;
import com.danrley.ecommerce.products.entity.ProductPriceHistory;
import com.danrley.ecommerce.products.entity.Supplier;
import com.danrley.ecommerce.products.mapper.ProductMapper;
import com.danrley.ecommerce.products.repository.CategoryRepository;
import com.danrley.ecommerce.products.repository.ProductPriceHistoryRepository;
import com.danrley.ecommerce.products.repository.ProductRepository;
import com.danrley.ecommerce.products.repository.SupplierRepository;
import com.danrley.ecommerce.shared.exception.BusinessException;
import com.danrley.ecommerce.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service responsável pela lógica de negócio de produtos.
 * Implementa CRUD completo com validações.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductMapper productMapper;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;

    /**
     * Cria um novo produto.
     * PERMISSÃO: ADMIN only
     *
     * @param request dados do produto
     * @return produto criado
     * @throws ResourceNotFoundException se categoria ou fornecedor não existir
     * @throws BusinessException se SKU já existir
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // Validar categoria existe e está ativa
        Category category = validateAndGetCategory(request.getCategoryId());

        // Validar fornecedor (OPCIONAL)
        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = validateAndGetSupplier(request.getSupplierId());
        }

        // Validar SKU único (se fornecido)
        if (request.getSku() != null && productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU já cadastrado: " + request.getSku(), "DUPLICATE_SKU");
        }

        // Criar produto
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setReservedQuantity(0); // sempre inicia zerado
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setSku(request.getSku());
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setMetadata(request.getMetadata());

        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Busca produto por ID.
     * PERMISSÃO: Público (sem auth)
     *
     * @param id ID do produto
     * @return produto encontrado
     * @throws ResourceNotFoundException se produto não existir
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProductByIdOrThrow(id);
        return productMapper.toResponse(product);
    }

    /**
     * Lista produtos com filtros e paginação.
     * PERMISSÃO: Público (sem auth)
     *
     * @param filters filtros opcionais
     * @param pageable paginação
     * @return página de produtos
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(ProductFilterRequest filters, Pageable pageable) {
        Specification<Product> spec = buildSpecification(filters);
        Page<Product> products = productRepository.findAll(spec, pageable);
        return products.map(productMapper::toResponse);
    }

    /**
     * Atualiza um produto existente.
     * PERMISSÃO: ADMIN only
     *
     * @param id ID do produto
     * @param request novos dados
     * @return produto atualizado
     * @throws ResourceNotFoundException se produto, categoria ou fornecedor não existir
     * @throws BusinessException se SKU já existir para outro produto
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductByIdOrThrow(id);

        // Validar categoria
        Category category = validateAndGetCategory(request.getCategoryId());

        // Validar fornecedor (OPCIONAL)
        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = validateAndGetSupplier(request.getSupplierId());
        }

        // Validar SKU único (se mudou)
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(request.getSku())) {
                throw new BusinessException("SKU já cadastrado: " + request.getSku(), "DUPLICATE_SKU");
            }
        }

        // ⬇️ AUDITORIA DE PREÇO - ADR-004 ⬇️
        BigDecimal oldPrice = product.getPrice();
        BigDecimal newPrice = request.getPrice();

        if (oldPrice.compareTo(newPrice) != 0) {
            // Pegar usuário autenticado do SecurityContext
            String changedBy = SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();

            // Criar registro de histórico
            ProductPriceHistory history = new ProductPriceHistory(
                    product.getId(),
                    oldPrice,
                    newPrice,
                    changedBy,
                    "Atualização manual via API"
            );

            productPriceHistoryRepository.save(history);
        }
        // ⬆️ FIM AUDITORIA ⬆️

        // Atualizar campos
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setSku(request.getSku());
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setMetadata(request.getMetadata());

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Deleta um produto (SOFT DELETE).
     * Seta active = false ao invés de remover do banco.
     * PERMISSÃO: ADMIN only
     *
     * @param id ID do produto
     * @throws ResourceNotFoundException se produto não existir
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductByIdOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
    }

    // ========== MÉTODOS AUXILIARES ==========

    /**
     * Busca produto por ID ou lança exceção.
     */
    private Product findProductByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));
    }

    /**
     * Valida se categoria existe e está ativa.
     */
    private Category validateAndGetCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + categoryId));

        if (!category.getActive()) {
            throw new BusinessException("Categoria está inativa", "INACTIVE_CATEGORY");
        }

        return category;
    }

    /**
     * Valida se fornecedor existe e está ativo.
     */
    private Supplier validateAndGetSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado com ID: " + supplierId));

        if (!supplier.getActive()) {
            throw new BusinessException("Fornecedor está inativo", "INACTIVE_SUPPLIER");
        }

        return supplier;
    }

    /**
     * Constrói Specification para filtros dinâmicos.
     */
    private Specification<Product> buildSpecification(ProductFilterRequest filters) {
        Specification<Product> spec = Specification.where(null);

        if (filters == null) {
            return spec;
        }

        // Filtro: nome (contém - case insensitive)
        if (filters.getName() != null && !filters.getName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + filters.getName().toLowerCase() + "%"));
        }

        // Filtro: categoria
        if (filters.getCategoryId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), filters.getCategoryId()));
        }

        // Filtro: fornecedor
        if (filters.getSupplierId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("supplier").get("id"), filters.getSupplierId()));
        }

        // Filtro: preço mínimo
        if (filters.getMinPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), filters.getMinPrice()));
        }

        // Filtro: preço máximo
        if (filters.getMaxPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), filters.getMaxPrice()));
        }

        // Filtro: ativo/inativo
        if (filters.getActive() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("active"), filters.getActive()));
        }

        return spec;
    }
}