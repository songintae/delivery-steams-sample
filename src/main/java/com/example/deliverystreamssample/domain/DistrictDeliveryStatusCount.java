package com.example.deliverystreamssample.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@EqualsAndHashCode(of = {"localDate", "deliveryDistrict", "deliveryState"})
public class DistrictDeliveryStatusCount {
    private LocalDate localDate;
    private String deliveryDistrict;
    private DeliveryState deliveryState;
    private long count;

    public static DistrictDeliveryStatusCount of(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState, long count) {
        DistrictDeliveryStatusCount instance = new DistrictDeliveryStatusCount();
        instance.localDate = localDate;
        instance.deliveryDistrict = deliveryDistrict;
        instance.deliveryState = deliveryState;
        instance.count = count;

        return instance;
    }
}
