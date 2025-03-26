package com.example.ordersystem.ordering.service;

import com.example.ordersystem.ordering.domain.Ordering;
import com.example.ordersystem.ordering.dto.OrderCreateDto;
import com.example.ordersystem.ordering.dto.ProductDto;
import com.example.ordersystem.ordering.dto.ProductUpdateStockDto;
import com.example.ordersystem.ordering.repository.OrderingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final RestTemplate restTemplate;
    private final ProductFeign productFeign;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderingService(OrderingRepository orderingRepository, RestTemplate restTemplate, ProductFeign productFeign, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderingRepository = orderingRepository;
        this.restTemplate = restTemplate;
        this.productFeign = productFeign;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Ordering orderCreate(OrderCreateDto orderDto, String userId){
        Ordering ordering = Ordering.builder()
                .memberId(Long.parseLong(userId))
                .productId(orderDto.getProductId())
                .quantity(orderDto.getProductCount())
                .build();

        String productGetUrl = "http://product-service/product/"+orderDto.getProductId();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<ProductDto> response = restTemplate.exchange(productGetUrl, HttpMethod.GET, httpEntity, ProductDto.class);
        ProductDto product = response.getBody();
        int quantity = orderDto.getProductCount();
        if(product.getStockQuantity() < quantity){
            throw new IllegalArgumentException("재고 부족");
        }else {
            String productUpdateStockUrl =  "http://product-service/product/updatestock";
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ProductUpdateStockDto> updateEntity = new HttpEntity<>(
                    ProductUpdateStockDto.builder().productId(orderDto.getProductId()).productQuantity(orderDto.getProductCount()).build()
                    , headers);
            try {
                restTemplate.exchange(productUpdateStockUrl, HttpMethod.PUT, updateEntity, Void.class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    throw new IllegalArgumentException("재고 부족으로 업데이트 실패");
                }
                throw new RuntimeException("재고 업데이트 중 오류 발생: " + e.getMessage(), e);
            }
        }
        orderingRepository.save(ordering);
        return  ordering;
    }

    // ✅ Fallback 메서드 (서버 지연시 실행). Fallback 메서드의 매개변수와 원본 메서드의 매개변수가 정확히 일치해야함
    public Ordering fallbackProductService(OrderCreateDto orderDto, String userId, Throwable t) {
        throw new RuntimeException("상품 서비스(product-service) 응답 없음. 나중에 다시 시도해주세요: ");
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackProductService")
    public  Ordering orderFeignKafkaCreate(OrderCreateDto orderDto, String userId){
        Ordering ordering = Ordering.builder()
                .memberId(Long.parseLong(userId))
                .productId(orderDto.getProductId())
                .quantity(orderDto.getProductCount())
                .build();

//            product서버에 feign클라이언트를 통한 api요청 조회
        ProductDto product = productFeign.getProductById(orderDto.getProductId(), userId);
//        int quantity = orderDto.getProductCount();
//        if(product.getStockQuantity() < quantity){
//            throw new IllegalArgumentException("재고 부족");
//        }else {
////                재고감소 api요청을 product서버에 보내야함 -> kafka에 메시지 발행
////                productFeign.updateProductStock(ProductUpdateStockDto.builder().productId(o.getProductId()).productQuantity(o.getProductCount()).build());
//            ProductUpdateStockDto dto = ProductUpdateStockDto.builder()
//                    .productId(orderDto.getProductId()).productQuantity(orderDto.getProductCount()).build();
//            kafkaTemplate.send("update-stock-topic", dto);
//        }

        orderingRepository.save(ordering);
        return ordering;
    }


}
