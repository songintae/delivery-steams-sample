package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.domain.DeliveryState;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryFinder {
    private final DeliveryAggregatorConfiguration deliveryAggregatorConfiguration;

    public Long getCount(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        Optional<ReadOnlyKeyValueStore<DeliveryStatusCondition, Long>> mayBeCountPerStatusStore = deliveryAggregatorConfiguration.getCountPerStatusStore();
        if(mayBeCountPerStatusStore.isEmpty()) {
            return null;
        }
        ReadOnlyKeyValueStore<DeliveryStatusCondition, Long> countPerStatusStore = mayBeCountPerStatusStore.get();
        return countPerStatusStore.get(DeliveryStatusCondition.of(localDate, deliveryDistrict, deliveryState));
    }
}
