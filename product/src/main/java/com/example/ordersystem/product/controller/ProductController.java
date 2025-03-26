package com.example.ordersystem.product.controller;

import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductRegisterDto;
import com.example.ordersystem.product.dto.ProductResDto;
import com.example.ordersystem.product.dto.ProductUpdateStockDto;
import com.example.ordersystem.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Time;

@RestController
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> productCreate(ProductRegisterDto dto, @RequestHeader("X-User-Id") String userId){
        System.out.println("product x-user-id" + userId);
        Product product = productService.productCreate(dto, userId);
        return new ResponseEntity<>(product.getId(), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> productDetail(@PathVariable Long id) throws InterruptedException {
        Thread.sleep(3000L);
        ProductResDto productResDto = productService.productDetail(id);
        return new ResponseEntity<>(productResDto, HttpStatus.OK);
    }

    @PutMapping("/updatestock")
    public ResponseEntity<?> productStockUpdate(@RequestBody ProductUpdateStockDto dto, @RequestHeader("X-User-Id") String userId){
        System.out.println("update userId" + userId);
        Product product = productService.updateStockQuantity(dto);
        return new ResponseEntity<>(product.getId() , HttpStatus.OK);
    }

}

