package com.example.deliverysteamssample.application.serde;

import com.example.deliverysteamssample.domain.DeliveryEvent;
import org.springframework.kafka.support.serializer.JsonSerde;

public class DeliveryEventSerde extends JsonSerde<DeliveryEvent> {

}
