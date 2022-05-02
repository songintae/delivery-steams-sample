package com.example.deliverysteamssample.application;

import com.example.deliverysteamssample.application.extractor.DeliveryEventTimestampExtractor;
import com.example.deliverysteamssample.application.serde.DeliveryEventSerde;
import com.example.deliverysteamssample.domain.DeliveryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeliveryAggregatorConfiguration {

    private static final String STORE_LATEST_DELIVERY = "store-latest-delivery";
    private static final String STORE_COUNT_PER_STATUS = "store-count-per-status";

    private static final JsonSerde<DeliveryEvent> deliveryEventSerde = new DeliveryEventSerde();
    private static final JsonSerde<DeliveryStatusCondition> deliveryStatusConditionSerde = new JsonSerde<>(DeliveryStatusCondition.class);


    private final InteractiveQueryService interactiveQueryService;

    @Bean
    public Consumer<KStream<String, DeliveryEvent>> deliveryCountAggregator() {
        return input -> {
            KTable<String, DeliveryEvent> latestDeliveryEvent = input.toTable(Materialized.<String, DeliveryEvent, KeyValueStore<Bytes, byte[]>>as(STORE_LATEST_DELIVERY)
                    .withKeySerde(Serdes.String())
                    .withValueSerde(deliveryEventSerde));

            latestDeliveryEvent.groupBy(((key, value) -> KeyValue.pair(DeliveryStatusCondition.of(value.getOccurredDateTime().toLocalDate(), value.getDeliveryState()), value.getId())), Grouped.with(deliveryStatusConditionSerde, Serdes.String()))
                    .count(Materialized.<DeliveryStatusCondition, Long, KeyValueStore<Bytes, byte[]>>as(STORE_COUNT_PER_STATUS)
                            .withKeySerde(deliveryStatusConditionSerde)
                            .withValueSerde(Serdes.Long()));
        };
    }

    @Bean
    public TimestampExtractor deliveryEventTimestampExtractor() {
        return new DeliveryEventTimestampExtractor();
    }

    public Optional<ReadOnlyKeyValueStore<DeliveryStatusCondition, Long>> getCountPerStatusStore() {
        try {
            return Optional.of(interactiveQueryService.getQueryableStore(STORE_COUNT_PER_STATUS, QueryableStoreTypes.keyValueStore()));
        } catch (Exception e) {
            /**
             * StreamProcessor가 실행되는 동안(재배포 또는 재시작)에는 StateStore를 조회할 수 없다.
             */
            log.warn("State Store를 찾을 수 없습니다.", e);
            return Optional.empty();
        }

    }
}
