package com.delivery.api.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.api.dto.Dtos.Filter;
import com.delivery.api.dto.Dtos.FilterMeta;
import com.delivery.api.dto.Dtos.GroupStat;
import com.delivery.api.dto.Dtos.PincodeRisk;
import com.delivery.api.dto.Dtos.Summary;
import com.delivery.api.dto.Dtos.TrendPoint;
import com.delivery.api.service.AnalyticsService;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "delivery-api");
    }

    @GetMapping("/filters")
    public FilterMeta filters() {
        return service.filters();
    }

    @GetMapping("/summary")
    public Summary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(required = false) String partner,
                           @RequestParam(required = false) String district) {
        return service.summary(Filter.of(from, to, partner, district));
    }

    @GetMapping("/pincodes/risk")
    public List<PincodeRisk> pincodeRisk(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) String partner,
                                         @RequestParam(required = false) String district) {
        return service.pincodeRisk(Filter.of(from, to, partner, district));
    }

    @GetMapping("/trend")
    public List<TrendPoint> trend(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) String partner,
                                  @RequestParam(required = false) String district) {
        return service.trend(Filter.of(from, to, partner, district));
    }

    @GetMapping("/partners/leaderboard")
    public List<GroupStat> partners(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(required = false) String partner,
                                    @RequestParam(required = false) String district) {
        return service.partnerLeaderboard(Filter.of(from, to, partner, district));
    }

    @GetMapping("/hubs/leaderboard")
    public List<GroupStat> hubs(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @RequestParam(required = false) String partner,
                                @RequestParam(required = false) String district) {
        return service.hubLeaderboard(Filter.of(from, to, partner, district));
    }

    @GetMapping("/factors")
    public List<GroupStat> factors(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) String partner,
                                   @RequestParam(required = false) String district) {
        return service.factors(Filter.of(from, to, partner, district));
    }
}
