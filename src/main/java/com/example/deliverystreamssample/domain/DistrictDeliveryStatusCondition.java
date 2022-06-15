package com.example.deliverystreamssample.domain;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DistrictDeliveryStatusCondition {
    private LocalDate localDate;
    private String deliveryDistrict;
    private DeliveryState deliveryState;

    private DistrictDeliveryStatusCondition() {
    }

    private DistrictDeliveryStatusCondition(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        this.localDate = localDate;
        this.deliveryDistrict = deliveryDistrict;
        this.deliveryState = deliveryState;
    }

    public static DistrictDeliveryStatusCondition of(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        return new DistrictDeliveryStatusCondition(localDate, deliveryDistrict, deliveryState);
    }

}
