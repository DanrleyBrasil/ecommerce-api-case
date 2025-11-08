package com.danrley.ecommerce.products.mapper;

import com.danrley.ecommerce.products.dto.CategoryResponse;
import com.danrley.ecommerce.products.dto.ProductResponse;
import com.danrley.ecommerce.products.dto.SupplierResponse;
import com.danrley.ecommerce.products.entity.Category;
import com.danrley.ecommerce.products.entity.Product;
import com.danrley.ecommerce.products.entity.Supplier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper manual para conversão entre Entity e DTO.
 * Evita dependências externas como MapStruct para simplicidade.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Component
public class ProductMapper {

    /**
     * Converte Product Entity para ProductResponse DTO.
     *
     * @param product entidade do produto
     * @return DTO de resposta
     */
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .reservedQuantity(product.getReservedQuantity())
                .availableQuantity(product.getAvailableQuantity()) // método calculado
                .category(toCategoryResponse(product.getCategory()))
                .supplier(toSupplierResponse(product.getSupplier())) // PODE SER NULL
                .sku(product.getSku())
                .active(product.getActive())
                .metadata(product.getMetadata())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Converte lista de Product para lista de ProductResponse.
     *
     * @param products lista de entidades
     * @return lista de DTOs
     */
    public List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converte Category Entity para CategoryResponse DTO.
     *
     * @param category entidade da categoria
     * @return DTO de resposta
     */
    private CategoryResponse toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    /**
     * Converte Supplier Entity para SupplierResponse DTO.
     * IMPORTANTE: Supplier pode ser NULL (opcional).
     *
     * @param supplier entidade do fornecedor
     * @return DTO de resposta ou null
     */
    private SupplierResponse toSupplierResponse(Supplier supplier) {
        if (supplier == null) {
            return null;
        }

        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .cnpj(supplier.getCnpj())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .build();
    }
}