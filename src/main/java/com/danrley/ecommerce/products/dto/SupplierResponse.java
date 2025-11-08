package com.danrley.ecommerce.products.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para fornecedores.
 * Usado dentro de ProductResponse (nested).
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {

    private Long id;
    private String name;
    private String cnpj;
    private String email;
    private String phone;
}