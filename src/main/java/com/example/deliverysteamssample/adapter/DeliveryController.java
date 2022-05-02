package com.example.deliverysteamssample.adapter;


import com.example.deliverysteamssample.application.DeliveryFinder;
import com.example.deliverysteamssample.domain.DeliveryEvent;
import com.example.deliverysteamssample.domain.DeliveryState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/v1/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final KafkaTemplate<String, DeliveryEvent> kafkaTemplate;
    private final DeliveryFinder deliveryFinder;


    @PostMapping("/action")
    public void waitAllocate(@RequestBody ActionRequest request) throws ExecutionException, InterruptedException {
        DeliveryEvent event = new DeliveryEvent(request.getId(), request.getDeliveryState(), LocalDateTime.now(), request.getDeliveryDistrict());
        kafkaTemplate.send("delivery", event.getId(), event).get();
    }

    @GetMapping("/count")
    public Long getCount(@RequestParam LocalDate localDate, @RequestParam DeliveryState deliveryState) {
        return deliveryFinder.getCount(localDate, deliveryState);
    }

    @Getter
    static class ActionRequest {
        private String id;
        private DeliveryState deliveryState;
        private String deliveryDistrict;
    }

}
