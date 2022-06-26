package com.example.deliverystreamssample.adapter;


import com.example.deliverystreamssample.application.DeliveryFinder;
import com.example.deliverystreamssample.domain.DeliveryEvent;
import com.example.deliverystreamssample.domain.DeliveryState;
import com.example.deliverystreamssample.domain.WindowedDistrictDeliveryStatusCondition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    public Long getCount(@RequestParam LocalDate localDate, @RequestParam String deliveryDistrict, @RequestParam DeliveryState deliveryState) {
        return deliveryFinder.getCount(localDate, deliveryDistrict, deliveryState);
    }

    @GetMapping("/windowed/count")
    public List<WindowedDistrictDeliveryStatusCountResponse> getWindowedCount(@RequestParam LocalDate localDate,
                                                                              @RequestParam String deliveryDistrict,
                                                                              @RequestParam DeliveryState deliveryState,
                                                                              @RequestParam LocalTime fromTime,
                                                                              @RequestParam LocalTime toTime) {

        return deliveryFinder.getWaitAllocateWindowCount(localDate, deliveryDistrict, deliveryState, fromTime, toTime).entrySet().stream()
                .map(entry -> new WindowedDistrictDeliveryStatusCountResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Getter
    static class ActionRequest {
        private String id;
        private DeliveryState deliveryState;
        private String deliveryDistrict;
    }

}
