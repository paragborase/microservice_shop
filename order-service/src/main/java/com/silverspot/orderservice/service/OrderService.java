package com.silverspot.orderservice.service;

import com.silverspot.orderservice.dto.InventoryResponse;
import com.silverspot.orderservice.dto.OrderLineItemsDto;
import com.silverspot.orderservice.dto.OrderRequest;
import com.silverspot.orderservice.event.OrderPlacedEvent;
import com.silverspot.orderservice.model.Order;
import com.silverspot.orderservice.model.OrderLineItems;
import com.silverspot.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");



        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())) {
            //Call Inventory service and placed order when product is in STOCK
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                                .uri("http://inventory-service/api/inventory",
                                    uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                                .retrieve()
                                .bodyToMono(InventoryResponse[].class)
                                .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                                                .allMatch(InventoryResponse::isInStock);

            if(allProductsInStock){
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order placed Successfully / हुर्रे!!! ऑर्डर यशस्वीरित्या दिली";
            } else {
                throw new IllegalArgumentException("Product in Not in stock, please try again later");
            }
        } finally {
            inventoryServiceLookup.end();
        }  

    
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;

    }
}
