package com.example.deliverystreamssample.adapter;

import com.example.deliverystreamssample.domain.WindowedDistrictDeliveryStatusCondition;
import lombok.Getter;

@Getter
public class WindowedDistrictDeliveryStatusCountResponse {
    private WindowedDistrictDeliveryStatusCondition condition;
    private long count;

    private WindowedDistrictDeliveryStatusCountResponse() {
        //for-serialize
    }

    public WindowedDistrictDeliveryStatusCountResponse(WindowedDistrictDeliveryStatusCondition condition, long count) {
        this.condition = condition;
        this.count = count;
    }
}
