package com.silverspot.orderservice.controller;

import com.silverspot.orderservice.dto.OrderRequest;
import com.silverspot.orderservice.service.OrderService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    // @TimeLimiter(name = "inventory")
    // @Retry(name = "inventory")
    public String placeOrder(@RequestBody OrderRequest orderRequest) {
        return orderService.placeOrder(orderRequest);
        //return  CompletableFuture.supplyAsync(()->"Order placed Successfully / हुर्रे!!! ऑर्डर यशस्वीरित्या दिली");
    }

    public CompletableFuture<String> fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException)  {
        return CompletableFuture.supplyAsync(()-> "OOPS!! Something Went Wrong, Please order after Some Time / अरेरे!! काहीतरी चूक झाली, कृपया काही वेळानंतर ऑर्डर करा (भाऊ, इन्व्हेंटरी सेवा बंद आहे)");
    }
}
