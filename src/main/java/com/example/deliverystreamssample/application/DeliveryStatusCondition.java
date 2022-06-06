package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.domain.DeliveryState;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DeliveryStatusCondition {
    private LocalDate localDate;
    private String deliveryDistrict;
    private DeliveryState deliveryState;

    private DeliveryStatusCondition() {
    }

    private DeliveryStatusCondition(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        this.localDate = localDate;
        this.deliveryDistrict = deliveryDistrict;
        this.deliveryState = deliveryState;
    }

    public static DeliveryStatusCondition of(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        return new DeliveryStatusCondition(localDate, deliveryDistrict, deliveryState);
    }

}
