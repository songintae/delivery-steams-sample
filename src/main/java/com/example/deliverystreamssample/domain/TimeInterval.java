package com.example.deliverystreamssample.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TimeInterval {
    private LocalDateTime fromDateTime;
    private LocalDateTime toDateTime;

    private TimeInterval() {
        //for-serialize
    }

    public TimeInterval(LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        this.fromDateTime = fromDateTime;
        this.toDateTime = toDateTime;
    }
}
