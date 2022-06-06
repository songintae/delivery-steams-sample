package com.example.deliverystreamssample.domain;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@ToString
public class DeliveryEvent {
    private String id;
    private DeliveryState deliveryState;
    private LocalDateTime occurredDateTime;
    private String deliveryDistrict;

    private DeliveryEvent() {
        //for-serialize
    }

    public DeliveryEvent(String id, DeliveryState deliveryState, LocalDateTime occurredDateTime, String deliveryDistrict) {
        this.id = id;
        this.deliveryState = deliveryState;
        this.occurredDateTime = occurredDateTime;
        this.deliveryDistrict = deliveryDistrict;
    }
}
