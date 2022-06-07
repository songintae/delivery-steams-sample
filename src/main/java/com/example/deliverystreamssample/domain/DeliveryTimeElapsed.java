package com.example.deliverystreamssample.domain;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DeliveryTimeElapsed {
    private String id;
    private TimeElapsedType timeElapsedType;
    private long seconds;

    private DeliveryTimeElapsed() {
    }

    public static DeliveryTimeElapsed of(String id, TimeElapsedType timeElapsedType, long seconds) {
        final DeliveryTimeElapsed instance = new DeliveryTimeElapsed();
        instance.id = id;
        instance.timeElapsedType = timeElapsedType;
        instance.seconds = seconds;

        return instance;
    }
}
