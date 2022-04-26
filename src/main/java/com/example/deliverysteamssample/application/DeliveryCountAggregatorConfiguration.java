package com.example.deliverysteamssample.application;

import com.example.deliverysteamssample.domain.DeliveryEvent;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.function.Consumer;

@Configuration
public class DeliveryCountAggregatorConfiguration {

    @Bean
    public Consumer<KStream<String, DeliveryEvent>> deliveryCountAggregator() {
        return input -> {
            input
                    .print(Printed.<String, DeliveryEvent>toSysOut().withLabel("DeliveryCountAggregator streams"));
        };
    }

    public static class DeliveryEventSerde extends JsonSerde<DeliveryEvent> {}
}
