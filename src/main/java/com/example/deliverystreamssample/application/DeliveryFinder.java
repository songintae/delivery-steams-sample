package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.domain.DeliveryState;
import com.example.deliverystreamssample.domain.DistrictDeliveryStatusCondition;
import com.example.deliverystreamssample.domain.TimeInterval;
import com.example.deliverystreamssample.domain.WindowedDistrictDeliveryStatusCondition;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryFinder {
    private final DeliveryCountAggregatorConfiguration deliveryAggregatorConfiguration;
    private final WindowedDeliveryCountAggregatorConfiguration windowedDeliveryCountAggregatorConfiguration;

    public Long getCount(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        Optional<ReadOnlyKeyValueStore<DistrictDeliveryStatusCondition, Long>> mayBeCountPerStatusStore = deliveryAggregatorConfiguration.getCountPerStatusStore();
        if (mayBeCountPerStatusStore.isEmpty()) {
            return null;
        }
        ReadOnlyKeyValueStore<DistrictDeliveryStatusCondition, Long> countPerStatusStore = mayBeCountPerStatusStore.get();
        return countPerStatusStore.get(DistrictDeliveryStatusCondition.of(localDate, deliveryDistrict, deliveryState));
    }


    public Map<WindowedDistrictDeliveryStatusCondition, Long> getWaitAllocateWindowCount(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState, LocalTime timeFrom, LocalTime timeTo) {
        Optional<ReadOnlyWindowStore<String, Long>> mayBeWaitAllocateWindowCountStore = windowedDeliveryCountAggregatorConfiguration.getWaitAllocateWindowCountStore();
        if (mayBeWaitAllocateWindowCountStore.isEmpty()) {
            return null;
        }

        final Map<WindowedDistrictDeliveryStatusCondition, Long> result = new HashMap<>();
        final String interestedKey = WindowedDeliveryCountAggregatorConfiguration.getKey(localDate, deliveryDistrict, deliveryState);
        ReadOnlyWindowStore<String, Long> waitAllocateWindowCountStore = mayBeWaitAllocateWindowCountStore.get();
        try (KeyValueIterator<Windowed<String>, Long> iterator = waitAllocateWindowCountStore.fetchAll(
                toInstant(LocalDateTime.of(localDate, timeFrom)),
                toInstant(LocalDateTime.of(localDate, timeTo)))) {
            iterator.forEachRemaining(action -> {
                if (interestedKey.equalsIgnoreCase(action.key.key())) {
                    Window window = action.key.window();
                    result.put(WindowedDistrictDeliveryStatusCondition.of(action.key.key(), new TimeInterval(toLocalDateTime(window.startTime()), toLocalDateTime(window.endTime()))), action.value);
                }
            });
        }
        return result;
    }

    private Instant toInstant(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }
}
