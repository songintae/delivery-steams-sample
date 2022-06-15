package com.example.deliverystreamssample.domain;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DeliveryStatusCondition {
    private LocalDate localDate;
    private DeliveryState deliveryState;

    public static DeliveryStatusCondition of(LocalDate localDate, DeliveryState deliveryState) {
        DeliveryStatusCondition instance = new DeliveryStatusCondition();
        instance.localDate = localDate;
        instance.deliveryState = deliveryState;

        return instance;
    }
}
