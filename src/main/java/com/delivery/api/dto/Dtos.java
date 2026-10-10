package com.delivery.api.dto;

import java.time.LocalDate;
import java.util.List;

/** Response shapes returned by the REST API. */
public final class Dtos {

    private Dtos() {
    }

    public record Option(String id, String name) {
    }

    public record FilterMeta(List<Option> partners, List<String> districts, LocalDate minDate, LocalDate maxDate) {
    }

    public record Summary(long orders, double lateRate, double firstAttemptRate, double avgDelayHours,
                          double orderValue, double revenueAtRisk, double reattemptCost) {
    }

    public record PincodeRisk(String pincode, String town, String district, double lat, double lon,
                              long orders, double delayProbability, double firstAttemptRate,
                              double avgDelayHours, double orderValue, double revenueAtRisk) {
    }

    public record TrendPoint(String date, long orders, double lateRate, double lateRate7d,
                             double firstAttemptRate7d, double heavyRainShare7d, boolean festival) {
    }

    public record GroupStat(String label, long orders, double lateRate, double firstAttemptRate,
                            double avgDelayHours, double orderValue, double revenueAtRisk) {
    }

    /** Query filter. Null/blank partner or district means "all". */
    public record Filter(LocalDate from, LocalDate to, String partner, String district) {

        public static Filter of(LocalDate from, LocalDate to, String partner, String district) {
            return new Filter(
                    from != null ? from : LocalDate.of(1970, 1, 1),
                    to != null ? to : LocalDate.of(2999, 12, 31),
                    partner, district);
        }

        public Filter withoutPartner() {
            return new Filter(from, to, null, district);
        }
    }
}
