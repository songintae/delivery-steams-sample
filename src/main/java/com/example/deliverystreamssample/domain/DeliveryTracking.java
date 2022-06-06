package com.example.deliverystreamssample.domain;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@ToString
@Getter
public class DeliveryTracking {
    private String id;
    private DeliveryState deliveryState;
    private String deliveryDistrict;
    private LocalDateTime waitAllocateDateTime;
    private LocalDateTime completeAllocatedDateTime;
    private LocalDateTime completePickupDateTime;
    private LocalDateTime completeDeliveryDateTime;

    private DeliveryTracking() {
        //for-serialize
    }

    public static DeliveryTracking of(DeliveryEvent waitAllocateEvent) {
        DeliveryTracking instance = new DeliveryTracking();
        instance.id = waitAllocateEvent.getId();
        instance.deliveryState = waitAllocateEvent.getDeliveryState();
        instance.deliveryDistrict = waitAllocateEvent.getDeliveryDistrict();
        instance.waitAllocateDateTime = waitAllocateEvent.getOccurredDateTime();

        return instance;
    }

    public void completeAllocate(LocalDateTime dateTime) {
        this.deliveryState = DeliveryState.COMPLETE_ALLOCATE;
        this.completeAllocatedDateTime = dateTime;
    }

    public void completePickup(LocalDateTime dateTime) {
        this.deliveryState = DeliveryState.COMPLETE_PICKUP;
        this.completePickupDateTime = dateTime;
    }

    public void completeDelivery(LocalDateTime dateTime) {
        this.deliveryState = DeliveryState.COMPLETE_DELIVERY;
        this.completeDeliveryDateTime = dateTime;
    }

}
