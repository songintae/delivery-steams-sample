package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.domain.DeliveryState;
import com.example.deliverystreamssample.domain.DistrictDeliveryStatusCondition;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryFinder {
    private final DeliveryCountAggregatorConfiguration deliveryAggregatorConfiguration;

    public Long getCount(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        Optional<ReadOnlyKeyValueStore<DistrictDeliveryStatusCondition, Long>> mayBeCountPerStatusStore = deliveryAggregatorConfiguration.getCountPerStatusStore();
        if(mayBeCountPerStatusStore.isEmpty()) {
            return null;
        }
        ReadOnlyKeyValueStore<DistrictDeliveryStatusCondition, Long> countPerStatusStore = mayBeCountPerStatusStore.get();
        return countPerStatusStore.get(DistrictDeliveryStatusCondition.of(localDate, deliveryDistrict, deliveryState));
    }
}
