package com.example.deliverystreamssample.application;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class WindowedDeliveryCountAggregatorConfigurationTest {

    @Test
    void test() {
        System.out.println(LocalDateTime.ofInstant(Instant.ofEpochMilli(1656232260000L), TimeZone.getDefault().toZoneId()));
        System.out.println(LocalDateTime.ofInstant(Instant.ofEpochMilli(1656232320000L), TimeZone.getDefault().toZoneId()));
    }


    @Test
    void test1() {
        String value = LocalDate.now().toString();
        System.out.println(value);

        LocalDate parse = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
        System.out.println(parse);
    }
}