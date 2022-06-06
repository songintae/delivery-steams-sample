package com.example.deliverystreamssample.application;

import com.example.deliverystreamssample.domain.DeliveryEvent;
import com.example.deliverystreamssample.domain.DeliveryState;
import com.example.deliverystreamssample.domain.DeliveryTracking;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class DeliveryTrackerConfiguration {

    private static final String STORE_DELIVERY_TRACKING = "store-delivery-tracking";
    private Serde<DeliveryTracking> deliveryTrackingSerde = new JsonSerde<>(DeliveryTracking.class);

    @Bean
    public Consumer<KStream<String, DeliveryEvent>> deliveryTracker() {
        return input -> {
            input.transformValues(DeliveryTrackingValueTransformer::new, STORE_DELIVERY_TRACKING)
                    .print(Printed.<String, DeliveryTracking>toSysOut().withLabel("DeliveryTracker"));
        };
    }

    @Bean
    public StoreBuilder<KeyValueStore<String, DeliveryTracking>> storeDeliveryTracking() {
        StoreBuilder<KeyValueStore<String, DeliveryTracking>> keyValueStoreStoreBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STORE_DELIVERY_TRACKING),
                Serdes.String(),
                deliveryTrackingSerde
        );

        /**
         * StateStore의 Chang Log Topic의 설정은 다음과 같이 변경할 수 있다.
         * Change Log Topic의 크기를 10G, 보존 기간을 1일로 설정
         */
        Map<String, String> changeLogConfigs = new HashMap<>();
        changeLogConfigs.put("retention.ms", "86400000");
        changeLogConfigs.put("retention.bytes", "10000000000");

        keyValueStoreStoreBuilder.withLoggingEnabled(changeLogConfigs);
        return keyValueStoreStoreBuilder;
    }


    static class DeliveryTrackingValueTransformer implements ValueTransformer<DeliveryEvent, DeliveryTracking> {
        private KeyValueStore<String, DeliveryTracking> keyValueStore;

        @Override
        public void init(ProcessorContext context) {
            this.keyValueStore = context.getStateStore(STORE_DELIVERY_TRACKING);

        }

        @Override
        public DeliveryTracking transform(DeliveryEvent value) {
            if(value.getDeliveryState() == DeliveryState.WAIT_ALLOCATE) {
                DeliveryTracking deliveryTracking = DeliveryTracking.of(value);
                keyValueStore.put(deliveryTracking.getId(), deliveryTracking);
                return deliveryTracking;
            }

            DeliveryTracking deliveryTracking = keyValueStore.get(value.getId());
            if(deliveryTracking == null) {
                return null;
            }

            switch (value.getDeliveryState()) {
                case COMPLETE_ALLOCATE:
                    deliveryTracking.completeAllocate(value.getOccurredDateTime());
                    break;
                case COMPLETE_PICKUP:
                    deliveryTracking.completePickup(value.getOccurredDateTime());
                    break;
                case COMPLETE_DELIVERY:
                    deliveryTracking.completeDelivery(value.getOccurredDateTime());
                    break;
            }

            keyValueStore.put(value.getId(), deliveryTracking);
            return deliveryTracking;
        }

        @Override
        public void close() {

        }
    }
}
