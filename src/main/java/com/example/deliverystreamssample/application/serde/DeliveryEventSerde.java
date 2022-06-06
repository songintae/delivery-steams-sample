package com.example.deliverystreamssample.application.serde;

import com.example.deliverystreamssample.domain.DeliveryEvent;
import org.springframework.kafka.support.serializer.JsonSerde;

public class DeliveryEventSerde extends JsonSerde<DeliveryEvent> {

}
