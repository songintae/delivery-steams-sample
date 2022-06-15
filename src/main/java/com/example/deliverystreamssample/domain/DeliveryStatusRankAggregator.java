package com.example.deliverystreamssample.domain;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


@Getter
public class DeliveryStatusRankAggregator {
    private List<DistrictDeliveryStatusCount> ranks;
    private int maxSize;

    private DeliveryStatusRankAggregator() {
    }

    public DeliveryStatusRankAggregator(int maxSize) {
        this.ranks = new ArrayList<>();
        this.maxSize = maxSize;
    }

    public DeliveryStatusRankAggregator add(DistrictDeliveryStatusCount value) {
        ranks.add(value);
        if (ranks.size() > maxSize) {
            final Optional<DistrictDeliveryStatusCount> minDistrictDeliveryStatusCount = ranks.stream()
                    .min(Comparator.comparing(DistrictDeliveryStatusCount::getCount));
            minDistrictDeliveryStatusCount.ifPresent(v -> ranks.remove(minDistrictDeliveryStatusCount.get()));
        }

        ranks.sort(Comparator.comparing(DistrictDeliveryStatusCount::getCount).reversed());
        return this;
    }

    public String getRankString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranks.size() ; i++) {
            DistrictDeliveryStatusCount rank = ranks.get(i);
            sb.append(String.format("[%d, %s, %s, %s, %d] \n", i + 1, rank.getLocalDate(), rank.getDeliveryState(), rank.getDeliveryDistrict(), rank.getCount()));
        }
        return sb.toString();
    }

    public DeliveryStatusRankAggregator remove(DistrictDeliveryStatusCount value) {
        ranks.remove(value);
        return this;
    }
}
