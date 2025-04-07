package com.example.ordersystem.ordering.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductUpdateStockDto {
    private Long productId;
    private Integer productQuantity;
}
