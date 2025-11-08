package com.danrley.ecommerce.products.controller;

import com.danrley.ecommerce.products.dto.ProductFilterRequest;
import com.danrley.ecommerce.products.dto.ProductRequest;
import com.danrley.ecommerce.products.dto.ProductResponse;
import com.danrley.ecommerce.products.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gerenciamento de produtos.
 * <p>
 * Endpoints públicos (GET) não requerem autenticação.
 * Endpoints de escrita (POST/PUT/DELETE) requerem role ADMIN.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Gerenciamento de produtos do catálogo")
public class ProductController {

    private final ProductService productService;

    /**
     * Cria um novo produto.
     * PERMISSÃO: ADMIN only
     *
     * @param request dados do produto
     * @return produto criado (201 CREATED)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Criar produto",
            description = "Cria um novo produto no catálogo. Requer role ADMIN.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Busca produto por ID.
     * PERMISSÃO: Público (sem autenticação)
     *
     * @param id ID do produto
     * @return produto encontrado (200 OK)
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar produto por ID",
            description = "Retorna os detalhes de um produto específico. Acesso público."
    )
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista produtos com filtros e paginação.
     * PERMISSÃO: Público (sem autenticação)
     *
     * @param filters filtros opcionais (query params)
     * @param pageable paginação (page, size, sort)
     * @return página de produtos (200 OK)
     */
    @GetMapping
    @Operation(
            summary = "Listar produtos",
            description = "Lista produtos com filtros opcionais e paginação. Acesso público."
    )
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @ModelAttribute ProductFilterRequest filters,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        Page<ProductResponse> response = productService.getAllProducts(filters, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza um produto existente.
     * PERMISSÃO: ADMIN only
     *
     * @param id ID do produto
     * @param request novos dados
     * @return produto atualizado (200 OK)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Atualizar produto",
            description = "Atualiza os dados de um produto existente. Requer role ADMIN.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deleta um produto (SOFT DELETE).
     * PERMISSÃO: ADMIN only
     *
     * @param id ID do produto
     * @return sem conteúdo (204 NO CONTENT)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Deletar produto",
            description = "Desativa um produto (soft delete). Requer role ADMIN.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}