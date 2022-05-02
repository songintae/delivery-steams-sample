package com.example.deliverysteamssample.application.extractor;

import com.example.deliverysteamssample.domain.DeliveryEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.FailOnInvalidTimestamp;

import java.time.ZoneId;

public class DeliveryEventTimestampExtractor extends FailOnInvalidTimestamp {

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        DeliveryEvent deliveryEvent = (DeliveryEvent) record.value();
        final long timestamp = deliveryEvent.getOccurredDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        if (timestamp < 0) {
            return onInvalidTimestamp(record, timestamp, partitionTime);
        }

        return timestamp;
    }
}
