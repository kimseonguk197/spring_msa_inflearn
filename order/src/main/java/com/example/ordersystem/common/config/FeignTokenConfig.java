package com.example.ordersystem.common.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignTokenConfig {
    @Bean
    public RequestInterceptor requestInterceptor(){
        return request -> {
//            feign을 통한 요청시 header에 token을 interceptor를 통해 자동세팅
//            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
//            request.header(HttpHeaders.AUTHORIZATION, token);

        };
    }

}
