package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.application.serde.DeliveryEventSerde;
import com.example.deliverystreamssample.domain.DeliveryEvent;
import com.example.deliverystreamssample.domain.DistrictDeliveryStatusCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeliveryCountAggregatorConfiguration {

    private static final String STORE_LATEST_DELIVERY = "store-latest-delivery";
    private static final String STORE_COUNT_PER_DELIVERY_DISTRICT = "store-count-per-delivery-district";

    private static final JsonSerde<DeliveryEvent> deliveryEventSerde = new DeliveryEventSerde();
    private static final JsonSerde<DistrictDeliveryStatusCondition> deliveryStatusConditionSerde = new JsonSerde<>(DistrictDeliveryStatusCondition.class);


    private final InteractiveQueryService interactiveQueryService;

    @Bean
    public Consumer<KStream<String, DeliveryEvent>> deliveryCountAggregator() {
        return input -> {
            KTable<String, DeliveryEvent> latestDeliveryEvent = input.groupByKey(Grouped.with(Serdes.String(), deliveryEventSerde))
                    .reduce(DeliveryCountAggregatorConfiguration::latest, Materialized.<String, DeliveryEvent, KeyValueStore<Bytes, byte[]>>as(STORE_LATEST_DELIVERY)
                            .withKeySerde(Serdes.String())
                            .withValueSerde(deliveryEventSerde)
                            .withRetention(Duration.ofMinutes(5)));

            latestDeliveryEvent.groupBy(((key, value) -> KeyValue.pair(DistrictDeliveryStatusCondition.of(value.getOccurredDateTime().toLocalDate(), value.getDeliveryDistrict(), value.getDeliveryState()), value.getId())), Grouped.with(deliveryStatusConditionSerde, Serdes.String()))
                    .count(Materialized.<DistrictDeliveryStatusCondition, Long, KeyValueStore<Bytes, byte[]>>as(STORE_COUNT_PER_DELIVERY_DISTRICT)
                            .withKeySerde(deliveryStatusConditionSerde)
                            .withValueSerde(Serdes.Long())
                            .withRetention(Duration.ofMinutes(5)));
        };
    }

    private static DeliveryEvent latest(DeliveryEvent oldest, DeliveryEvent newly) {
        if (oldest == null) {
            return newly;
        }
        if (newly == null) {
            return oldest;
        }

        return oldest.getOccurredDateTime().isBefore(newly.getOccurredDateTime()) ? newly : oldest;

    }

    public Optional<ReadOnlyKeyValueStore<DistrictDeliveryStatusCondition, Long>> getCountPerStatusStore() {
        try {
            return Optional.of(interactiveQueryService.getQueryableStore(STORE_COUNT_PER_DELIVERY_DISTRICT, QueryableStoreTypes.keyValueStore()));
        } catch (Exception e) {
            /**
             * StreamProcessor가 실행되는 동안(재배포 또는 재시작)에는 StateStore를 조회할 수 없다.
             */
            log.warn("State Store를 찾을 수 없습니다.", e);
            return Optional.empty();
        }

    }
}
