package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.application.serde.DeliveryEventSerde;
import com.example.deliverystreamssample.domain.DeliveryEvent;
import com.example.deliverystreamssample.domain.DeliveryState;
import com.example.deliverystreamssample.domain.DistrictDeliveryStatusCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WindowedDeliveryCountAggregatorConfiguration {

    private static final String SPLIT_PREFIX = "TIME_WINDOW";
    private static final String STORE_WAIT_ALLOCATE_WINDOW_COUNT = "store-wait-allocate-window-count";
    private static final String STORE_COMPLETE_ALLOCATE_WINDOW_COUNT = "store-complete-allocate-window-count";
    private static final String STORE_COMPLETE_PICKUP_WINDOW_COUNT = "store-complete-pickup-window-count";
    private static final String STORE_COMPLETE_DELIVERY_WINDOW_COUNT = "store-complete-delivery-window-count";
    private static final Serde<DeliveryEvent> deliveryEventSerde = new DeliveryEventSerde();
    private static final Serde<DistrictDeliveryStatusCondition> districtDeliveryStatusConditionSerde = new JsonSerde<>(DistrictDeliveryStatusCondition.class);

    private final InteractiveQueryService interactiveQueryService;

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
                    .print(Printed.<Windowed<String>, Long>toSysOut().withLabel("WaitAllocateWindowCount"));
        };
    }

    private KTable<Windowed<String>, Long> doCountWithWindow(KStream<String, DeliveryEvent> stream, String storeName) {
        /**
         * StateStore의 Chang Log Topic의 설정은 다음과 같이 변경할 수 있다.
         * Change Log Topic의 크기를 1G, 보존 기간을 1일로 설정
         */
        Map<String, String> changeLogConfigs = new HashMap<>();
        changeLogConfigs.put("retention.ms", "86400000");
        changeLogConfigs.put("retention.bytes", "1000000000");

        //TODO Key를 객체로 GroupBy시 Serialize Exception 발생하는 부분 확인 필요
        return stream.groupBy((key, value) -> getKey(value), Grouped.with(Serdes.String(), deliveryEventSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(storeName)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long())
                        .withLoggingEnabled(changeLogConfigs));
    }

    private static String getKey(DeliveryEvent deliveryEvent) {
        return getKey(deliveryEvent.getOccurredDateTime().toLocalDate(), deliveryEvent.getDeliveryDistrict(), deliveryEvent.getDeliveryState());
    }

    public static String getKey(LocalDate localDate, String deliveryDistrict, DeliveryState deliveryState) {
        return String.format("%s:%s:%s",localDate.toString(), deliveryDistrict, deliveryState.name());
    }

    public Optional<ReadOnlyWindowStore<String, Long>> getWaitAllocateWindowCountStore() {
        try {
            return Optional.of(interactiveQueryService.getQueryableStore(STORE_WAIT_ALLOCATE_WINDOW_COUNT, QueryableStoreTypes.windowStore()));
        } catch (Exception e) {
            /**
             * StreamProcessor가 실행되는 동안(재배포 또는 재시작)에는 StateStore를 조회할 수 없다.
             */
            log.warn("State Store를 찾을 수 없습니다.", e);
            return Optional.empty();
        }

    }
}
