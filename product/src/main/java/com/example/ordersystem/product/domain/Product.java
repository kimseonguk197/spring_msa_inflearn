package com.example.ordersystem.product.domain;

import com.example.ordersystem.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Product  extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer price;
    private Integer stockQuantity;
//    memberId로 변경
    private Long memberId;

    public void updateStockQuantity(int stockQuantity){
        if (this.stockQuantity < stockQuantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        this.stockQuantity = this.stockQuantity - stockQuantity;
    }

}
