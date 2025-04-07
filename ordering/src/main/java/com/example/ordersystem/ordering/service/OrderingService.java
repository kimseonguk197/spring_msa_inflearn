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

//        product get 요청
        String productGetUrl = "http://product-service/product/"+orderDto.getProductId();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-User-Id", userId);
        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);
        ResponseEntity<ProductDto> response =  restTemplate.exchange(productGetUrl, HttpMethod.GET, httpEntity, ProductDto.class);
        ProductDto productDto = response.getBody();
        int quantity = orderDto.getProductCount();
        if(productDto.getStockQuantity() < quantity){
            throw new IllegalArgumentException("재고 부족");
        }else {

//        product put 요청
            String productPutUrl = "http://product-service/product/updatestock";
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ProductUpdateStockDto> updateEntity = new HttpEntity(
                    ProductUpdateStockDto.builder().productId(orderDto.getProductId())
                            .productQuantity(orderDto.getProductCount()).build()
                    , httpHeaders
            );
            restTemplate.exchange(productPutUrl, HttpMethod.PUT, updateEntity, Void.class);
        }
        Ordering ordering = Ordering.builder()
                .memberId(Long.parseLong(userId))
                .productId(orderDto.getProductId())
                .quantity(orderDto.getProductCount())
                .build();
        orderingRepository.save(ordering);
        return  ordering;
    }



//    circuitbreaker는 해당 name을 가진 메서드에 한해서만 유효.
//    즉, circuit이 open되어도 다른 메서드에서 product-service에 요청을 보내는 것은 허용.
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackProductService")
    public Ordering orderFeignKafkaCreate(OrderCreateDto orderDto, String userId){

        ProductDto productDto = productFeign.getProductById(orderDto.getProductId(), userId);

        int quantity = orderDto.getProductCount();
        if(productDto.getStockQuantity() < quantity){
            throw new IllegalArgumentException("재고 부족");
        }else {
            ProductUpdateStockDto dto = ProductUpdateStockDto.builder().productId(orderDto.getProductId())
                    .productQuantity(orderDto.getProductCount()).build();
//            productFeign.updateProductStock(dto);
            kafkaTemplate.send("update-stock-topic", dto);

        }
        Ordering ordering = Ordering.builder()
                .memberId(Long.parseLong(userId))
                .productId(orderDto.getProductId())
                .quantity(orderDto.getProductCount())
                .build();
        orderingRepository.save(ordering);
        return  ordering;
    }

    public Ordering fallbackProductService(OrderCreateDto orderDto, String userId, Throwable t){
        throw new RuntimeException("상품서비스가 응답이 없어, 에러가 발생했습니다. 나중에 다시 시도해주세요.");
    }

}
