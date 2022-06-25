package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.application.serde.DeliveryEventSerde;
import com.example.deliverystreamssample.domain.DeliveryEvent;
import com.example.deliverystreamssample.domain.DeliveryState;
import com.example.deliverystreamssample.domain.DistrictDeliveryStatusCondition;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class WindowedDeliveryCountAggregatorConfiguration {

    private static final String SPLIT_PREFIX = "TIME_WINDOW";
    private static final String STORE_WAIT_ALLOCATE_WINDOW_COUNT = "store-wait-allocate-window-count";
    private static final String STORE_COMPLETE_ALLOCATE_WINDOW_COUNT = "store-complete-allocate-window-count";
    private static final String STORE_COMPLETE_PICKUP_WINDOW_COUNT = "store-complete-pickup-window-count";
    private static final String STORE_COMPLETE_DELIVERY_WINDOW_COUNT = "store-complete-delivery-window-count";
    private static final Serde<DeliveryEvent> deliveryEventSerde = new DeliveryEventSerde();
    private static final Serde<DistrictDeliveryStatusCondition> districtDeliveryStatusConditionSerde = new JsonSerde<>(DistrictDeliveryStatusCondition.class);

    @Bean
    public Consumer<KStream<String, DeliveryEvent>> windowedDeliveryCountAggregator() {
        return input -> {
            Map<String, KStream<String, DeliveryEvent>> splits = input.split(Named.as(SPLIT_PREFIX))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.WAIT_ALLOCATE, Branched.as(DeliveryState.WAIT_ALLOCATE.name()))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.COMPLETE_ALLOCATE, Branched.as(DeliveryState.COMPLETE_ALLOCATE.name()))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.COMPLETE_PICKUP, Branched.as(DeliveryState.COMPLETE_PICKUP.name()))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.COMPLETE_DELIVERY, Branched.as(DeliveryState.COMPLETE_DELIVERY.name()))
                    .noDefaultBranch();

            doCountWithWindow(splits.get(SPLIT_PREFIX + DeliveryState.WAIT_ALLOCATE.name()), STORE_WAIT_ALLOCATE_WINDOW_COUNT)
                    .toStream()
                    .print(Printed.<Windowed<DistrictDeliveryStatusCondition>, Long>toSysOut().withLabel("WaitAllocateWindowCount"));
        };
    }

    private KTable<Windowed<DistrictDeliveryStatusCondition>, Long> doCountWithWindow(KStream<String, DeliveryEvent> stream, String storeName) {
        return stream.groupBy((key, value) -> DistrictDeliveryStatusCondition.of(value.getOccurredDateTime().toLocalDate(), value.getDeliveryDistrict(), value.getDeliveryState()), Grouped.with(districtDeliveryStatusConditionSerde, deliveryEventSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                .count(Materialized.<DistrictDeliveryStatusCondition, Long, WindowStore<Bytes, byte[]>>as(storeName)
                        .withKeySerde(districtDeliveryStatusConditionSerde)
                        .withValueSerde(Serdes.Long()));
    }
}
