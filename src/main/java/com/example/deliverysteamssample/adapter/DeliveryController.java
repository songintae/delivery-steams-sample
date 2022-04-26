package com.example.deliverysteamssample.adapter;


import com.example.deliverysteamssample.domain.DeliveryEvent;
import com.example.deliverysteamssample.domain.DeliveryState;
import lombok.Getter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/v1/api/deliveries")
public class DeliveryController {
    private final KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

    public DeliveryController(KafkaTemplate<String, DeliveryEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/action")
    public void waitAllocate(@RequestBody ActionRequest request) throws ExecutionException, InterruptedException {
        DeliveryEvent event = new DeliveryEvent(request.getId(), request.getDeliveryState(), LocalDateTime.now(), request.getDeliveryDistrict());
        kafkaTemplate.send("delivery", event.getId(), event).get();
    }

    @Getter
    static class ActionRequest {
        private String id;
        private DeliveryState deliveryState;
        private String deliveryDistrict;
    }

}
