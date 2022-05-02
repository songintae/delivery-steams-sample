package com.example.deliverysteamssample.application;

import com.example.deliverysteamssample.domain.DeliveryState;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DeliveryStatusCondition {
    private LocalDate localDate;
    private DeliveryState deliveryState;

    private DeliveryStatusCondition() {
    }

    private DeliveryStatusCondition(LocalDate localDate, DeliveryState deliveryState) {
        this.localDate = localDate;
        this.deliveryState = deliveryState;
    }

    public static DeliveryStatusCondition of(LocalDate localDate, DeliveryState deliveryState) {
        return new DeliveryStatusCondition(localDate, deliveryState);
    }

}
