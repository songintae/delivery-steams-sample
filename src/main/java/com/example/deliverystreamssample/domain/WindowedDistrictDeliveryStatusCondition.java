package com.example.deliverystreamssample.domain;

import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
public class WindowedDistrictDeliveryStatusCondition {
    private DistrictDeliveryStatusCondition deliveryStatusCondition;
    private TimeInterval timeInterval;

    private WindowedDistrictDeliveryStatusCondition() {
    }

    private WindowedDistrictDeliveryStatusCondition(DistrictDeliveryStatusCondition deliveryStatusCondition, TimeInterval timeInterval) {
        this.deliveryStatusCondition = deliveryStatusCondition;
        this.timeInterval = timeInterval;
    }

    public static WindowedDistrictDeliveryStatusCondition of(String key, TimeInterval timeInterval) {
        WindowedDistrictDeliveryStatusCondition instance = new WindowedDistrictDeliveryStatusCondition();
        instance.deliveryStatusCondition = parse(key);
        instance.timeInterval = timeInterval;
        return instance;
    }

    private static DistrictDeliveryStatusCondition parse(String key) {
        String[] values = key.split(":");
        return DistrictDeliveryStatusCondition.of(LocalDate.parse(values[0], DateTimeFormatter.ISO_DATE), values[1], DeliveryState.valueOf(values[2]));
    }
}
