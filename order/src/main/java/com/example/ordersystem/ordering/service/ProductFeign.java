package com.example.ordersystem.ordering.service;


import com.example.ordersystem.common.config.FeignTokenConfig;
import com.example.ordersystem.ordering.dto.ProductDto;
import com.example.ordersystem.ordering.dto.ProductUpdateStockDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", configuration = FeignTokenConfig.class)
public interface ProductFeign {

//    feign을 통해 header에 간결하게 userId값 세팅
    @GetMapping("/product/{productId}")
    ProductDto getProductById(@PathVariable Long productId, @RequestHeader("X-User-Id") String userId);

    @PutMapping(value = "/product/updatestock")
    void updateProductStock(@RequestBody ProductUpdateStockDto dto);
}
