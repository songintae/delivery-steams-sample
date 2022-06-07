package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.application.serde.DeliveryEventSerde;
import com.example.deliverystreamssample.domain.DeliveryEvent;
import com.example.deliverystreamssample.domain.DeliveryState;
import com.example.deliverystreamssample.domain.DeliveryTimeElapsed;
import com.example.deliverystreamssample.domain.TimeElapsedType;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class DeliveryTimeElapsedTrackerConfiguration {
    private static final String SPLIT_PREFIX = "TIME_ELAPSED";
    private static final String STORE_ALLOCATE_TIME_ELAPSED = "store-allocate-time-elapsed";
    private static final String STORE_PICKUP_TIME_ELAPSED = "store-pickup-time-elapsed";
    private static final String STORE_DELIVERED_TIME_ELAPSED = "store-delivered-time-elapsed";
    private static final Serde<DeliveryEvent> deliveryEventSerde = new DeliveryEventSerde();

    @Bean
    public Consumer<KStream<String, DeliveryEvent>> deliveryTimeElapsedTracker() {
        return input -> {
            final Map<String, KStream<String, DeliveryEvent>> splits = input.split(Named.as(SPLIT_PREFIX))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.WAIT_ALLOCATE, Branched.as("WAIT_ALLOCATE"))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.COMPLETE_ALLOCATE, Branched.as("COMPLETE_ALLOCATE"))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.COMPLETE_PICKUP, Branched.as("COMPLETE_PICKUP"))
                    .branch((key, value) -> value.getDeliveryState() == DeliveryState.COMPLETE_DELIVERY, Branched.as("COMPLETE_DELIVERY"))
                    .noDefaultBranch();

            KStream<String, DeliveryTimeElapsed> allocateTimeElapsed = joinStream(
                    splits.get(SPLIT_PREFIX + DeliveryState.WAIT_ALLOCATE.name()),
                    splits.get(SPLIT_PREFIX + DeliveryState.COMPLETE_ALLOCATE.name()),
                    (thisValue, otherValue) -> DeliveryTimeElapsed.of(thisValue.getId(), TimeElapsedType.ALLOCATE, ChronoUnit.SECONDS.between(thisValue.getOccurredDateTime(), otherValue.getOccurredDateTime())),
                    JoinWindows.of(Duration.ofMinutes(5)),
                    STORE_ALLOCATE_TIME_ELAPSED
            );

            KStream<String, DeliveryTimeElapsed> pickupTimeElapsed = joinStream(
                    splits.get(SPLIT_PREFIX + DeliveryState.COMPLETE_ALLOCATE.name()),
                    splits.get(SPLIT_PREFIX + DeliveryState.COMPLETE_PICKUP.name()),
                    (thisValue, otherValue) -> DeliveryTimeElapsed.of(thisValue.getId(), TimeElapsedType.PICKUP, ChronoUnit.SECONDS.between(thisValue.getOccurredDateTime(), otherValue.getOccurredDateTime())),
                    JoinWindows.of(Duration.ofMinutes(5)),
                    STORE_PICKUP_TIME_ELAPSED
            );

            KStream<String, DeliveryTimeElapsed> deliveredTimeElapsed = joinStream(
                    splits.get(SPLIT_PREFIX + DeliveryState.COMPLETE_PICKUP.name()),
                    splits.get(SPLIT_PREFIX + DeliveryState.COMPLETE_DELIVERY.name()),
                    (thisValue, otherValue) -> DeliveryTimeElapsed.of(thisValue.getId(), TimeElapsedType.DELIVERED, ChronoUnit.SECONDS.between(thisValue.getOccurredDateTime(), otherValue.getOccurredDateTime())),
                    JoinWindows.of(Duration.ofMinutes(5)),
                    STORE_DELIVERED_TIME_ELAPSED
            );

            allocateTimeElapsed.print(Printed.<String, DeliveryTimeElapsed>toSysOut().withLabel("AllocateTimeElapsed"));
            pickupTimeElapsed.print(Printed.<String, DeliveryTimeElapsed>toSysOut().withLabel("PickupTimeElapsed"));
            deliveredTimeElapsed.print(Printed.<String, DeliveryTimeElapsed>toSysOut().withLabel("DeliveredTimeElapsed"));
        };
    }

    private KStream<String, DeliveryTimeElapsed> joinStream(KStream<String, DeliveryEvent> thisStream,
                                                            KStream<String, DeliveryEvent> otherStream,
                                                            ValueJoiner<DeliveryEvent, DeliveryEvent, DeliveryTimeElapsed> valueJoiner,
                                                            JoinWindows joinWindows,
                                                            String storeName) {

        /**
         * StateStore의 Chang Log Topic의 설정은 다음과 같이 변경할 수 있다.
         * Change Log Topic의 크기를 1G, 보존 기간을 1일로 설정
         */
        Map<String, String> changeLogConfigs = new HashMap<>();
        changeLogConfigs.put("retention.ms", "86400000");
        changeLogConfigs.put("retention.bytes", "1000000000");

        return thisStream.join(
                otherStream,
                valueJoiner,
                joinWindows,
                StreamJoined.<String, DeliveryEvent, DeliveryEvent>as(storeName)
                        .withLoggingEnabled(changeLogConfigs)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(deliveryEventSerde)
                        .withOtherValueSerde(deliveryEventSerde)
        );
    }
}
