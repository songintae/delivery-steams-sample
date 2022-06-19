package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.application.serde.DeliveryEventSerde;
import com.example.deliverystreamssample.domain.*;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.function.Consumer;

@Configuration
public class DeliveryStatusCountRankAggregatorConfiguration {

    private static final String STORE_LATEST_DELIVERY = "store-latest-delivery";
    private static final String STORE_COUNT_PER_DELIVERY_DISTRICT = "store-count-per-delivery-district";
    private static final String STORE_DELIVERY_STATUS_COUNT_RANK_PER_DISTRICT = "store-delivery-status-count-rank-per-district";
    private static final Serde<DeliveryEvent> deliveryEventSerde = new DeliveryEventSerde();
    private static final Serde<DistrictDeliveryStatusCondition> districtDeliveryStatusConditionSerde = new JsonSerde<>(DistrictDeliveryStatusCondition.class);
    private static final Serde<DeliveryStatusCondition> deliveryStatusConditionSerde = new JsonSerde<>(DeliveryStatusCondition.class);
    private static final Serde<DistrictDeliveryStatusCount> districtDeliveryStatusCountSerde = new JsonSerde<>(DistrictDeliveryStatusCount.class);
    private static final Serde<DeliveryStatusRankAggregator> deliveryStatusRankAggregatorSerde = new JsonSerde<>(DeliveryStatusRankAggregator.class);

    @Bean
    public Consumer<KStream<String, DeliveryEvent>> deliveryStatusCountRankAggregator() {
        return input -> {
            KTable<String, DeliveryEvent> latestDeliveryEvent = input.groupByKey(Grouped.with(Serdes.String(), deliveryEventSerde))
                    .reduce(DeliveryStatusCountRankAggregatorConfiguration::latest, Materialized.<String, DeliveryEvent, KeyValueStore<Bytes, byte[]>>as(STORE_LATEST_DELIVERY)
                            .withKeySerde(Serdes.String())
                            .withValueSerde(deliveryEventSerde));

            KTable<DistrictDeliveryStatusCondition, Long> districtDeliveryStatusCount = latestDeliveryEvent.groupBy(((key, value) -> KeyValue.pair(DistrictDeliveryStatusCondition.of(value.getOccurredDateTime().toLocalDate(), value.getDeliveryDistrict(), value.getDeliveryState()), value.getId())), Grouped.with(districtDeliveryStatusConditionSerde, Serdes.String()))
                    .count(Materialized.<DistrictDeliveryStatusCondition, Long, KeyValueStore<Bytes, byte[]>>as(STORE_COUNT_PER_DELIVERY_DISTRICT)
                            .withKeySerde(districtDeliveryStatusConditionSerde)
                            .withValueSerde(Serdes.Long()));

            districtDeliveryStatusCount.groupBy((key, value) -> KeyValue.pair(String.format("%s:%s", key.getLocalDate(), key.getDeliveryState()), DistrictDeliveryStatusCount.of(key.getLocalDate(), key.getDeliveryDistrict(), key.getDeliveryState(), value)), Grouped.with(Serdes.String(), districtDeliveryStatusCountSerde))
                    .aggregate(() -> new DeliveryStatusRankAggregator(2),
                            (key, value, aggregator) -> aggregator.add(value),
                            (key, value, aggregator) -> aggregator.remove(value),
                            Materialized.<String, DeliveryStatusRankAggregator, KeyValueStore<Bytes, byte[]>>as(STORE_DELIVERY_STATUS_COUNT_RANK_PER_DISTRICT)
                                    .withKeySerde(Serdes.String())
                                    .withValueSerde(deliveryStatusRankAggregatorSerde))
                    .toStream()
                    .mapValues(DeliveryStatusRankAggregator::getRankString)
                    .print(Printed.<String, String>toSysOut().withLabel("DeliveryStatusCountRank"));
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
}
