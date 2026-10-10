package com.delivery.api.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.delivery.api.dto.Dtos.Filter;
import com.delivery.api.dto.Dtos.FilterMeta;
import com.delivery.api.dto.Dtos.GroupStat;
import com.delivery.api.dto.Dtos.Option;
import com.delivery.api.dto.Dtos.PincodeRisk;
import com.delivery.api.dto.Dtos.Summary;
import com.delivery.api.dto.Dtos.TrendPoint;
import com.delivery.api.repository.PartnerRepository;

@Service
public class AnalyticsService {

    private static final String FROM_JOIN = " FROM delivery d"
            + " JOIN pincode p ON d.pincode = p.code"
            + " JOIN partner pr ON d.partner_id = pr.id"
            + " JOIN hub h ON d.hub_id = h.id";

    private static final String LATE = "CASE WHEN d.late THEN 1.0 ELSE 0.0 END";
    private static final String FIRST = "CASE WHEN d.attempts = 1 THEN 1.0 ELSE 0.0 END";

    private final JdbcTemplate jdbc;
    private final PartnerRepository partnerRepo;
    private final double reattemptCost;

    public AnalyticsService(JdbcTemplate jdbc, PartnerRepository partnerRepo,
                            @Value("${app.reattempt-cost}") double reattemptCost) {
        this.jdbc = jdbc;
        this.partnerRepo = partnerRepo;
        this.reattemptCost = reattemptCost;
    }

    // ------------------------------------------------------------------ filters

    public FilterMeta filters() {
        List<Option> partners = partnerRepo.findAll().stream()
                .map(p -> new Option(p.getId(), p.getName()))
                .sorted(Comparator.comparing(Option::name))
                .toList();
        List<String> districts = jdbc.queryForList("SELECT DISTINCT district FROM pincode ORDER BY district",
                String.class);
        return jdbc.queryForObject("SELECT MIN(dispatch_date), MAX(dispatch_date) FROM delivery",
                (rs, i) -> new FilterMeta(partners, districts,
                        rs.getDate(1).toLocalDate(), rs.getDate(2).toLocalDate()));
    }

    // ------------------------------------------------------------------ KPIs

    public Summary summary(Filter f) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT COUNT(*) AS orders, AVG(" + LATE + ") AS late_rate, AVG(" + FIRST + ") AS fa_rate,"
                + " AVG(d.delay_hours) AS avg_delay, SUM(d.order_value) AS total_value,"
                + " SUM(d.attempts) AS attempts" + FROM_JOIN + where(f, args);
        return jdbc.queryForObject(sql, (rs, i) -> {
            long orders = rs.getLong("orders");
            double lateRate = rs.getDouble("late_rate");
            double value = rs.getDouble("total_value");
            return new Summary(orders, lateRate, rs.getDouble("fa_rate"), rs.getDouble("avg_delay"),
                    value, value * lateRate, (rs.getLong("attempts") - orders) * reattemptCost);
        }, args.toArray());
    }

    // ------------------------------------------------------------------ map

    public List<PincodeRisk> pincodeRisk(Filter f) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT p.code, p.town, p.district, p.lat, p.lon, COUNT(*) AS orders,"
                + " AVG(" + LATE + ") AS late_rate, AVG(" + FIRST + ") AS fa_rate,"
                + " AVG(d.delay_hours) AS avg_delay, SUM(d.order_value) AS total_value"
                + FROM_JOIN + where(f, args)
                + " GROUP BY p.code, p.town, p.district, p.lat, p.lon";
        List<PincodeRisk> rows = jdbc.query(sql, (rs, i) -> {
            double lateRate = rs.getDouble("late_rate");
            double value = rs.getDouble("total_value");
            return new PincodeRisk(rs.getString("code"), rs.getString("town"), rs.getString("district"),
                    rs.getDouble("lat"), rs.getDouble("lon"), rs.getLong("orders"), lateRate,
                    rs.getDouble("fa_rate"), rs.getDouble("avg_delay"), value, value * lateRate);
        }, args.toArray());
        return rows.stream()
                .sorted(Comparator.comparingDouble(PincodeRisk::revenueAtRisk).reversed())
                .toList();
    }

    // ------------------------------------------------------------------ trend

    public List<TrendPoint> trend(Filter f) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT d.dispatch_date AS dt, COUNT(*) AS orders,"
                + " SUM(CASE WHEN d.late THEN 1 ELSE 0 END) AS late_orders,"
                + " SUM(CASE WHEN d.attempts = 1 THEN 1 ELSE 0 END) AS fa_orders,"
                + " SUM(CASE WHEN d.weather = 'Heavy Rain' THEN 1 ELSE 0 END) AS rain_orders,"
                + " MAX(CASE WHEN d.festival THEN 1 ELSE 0 END) AS fest"
                + FROM_JOIN + where(f, args)
                + " GROUP BY d.dispatch_date ORDER BY d.dispatch_date";

        record Day(String date, long orders, long late, long fa, long rain, boolean fest) {
        }
        List<Day> days = jdbc.query(sql, (rs, i) -> new Day(rs.getDate("dt").toString(), rs.getLong("orders"),
                rs.getLong("late_orders"), rs.getLong("fa_orders"), rs.getLong("rain_orders"),
                rs.getInt("fest") == 1), args.toArray());

        List<TrendPoint> out = new ArrayList<>(days.size());
        for (int i = 0; i < days.size(); i++) {
            long o = 0, l = 0, fa = 0, rain = 0;
            for (int k = Math.max(0, i - 6); k <= i; k++) {
                Day dd = days.get(k);
                o += dd.orders();
                l += dd.late();
                fa += dd.fa();
                rain += dd.rain();
            }
            Day cur = days.get(i);
            out.add(new TrendPoint(cur.date(), cur.orders(),
                    cur.orders() == 0 ? 0 : (double) cur.late() / cur.orders(),
                    o == 0 ? 0 : (double) l / o,
                    o == 0 ? 0 : (double) fa / o,
                    o == 0 ? 0 : (double) rain / o,
                    cur.fest()));
        }
        return out;
    }

    // ------------------------------------------------------------------ leaderboards & factors

    /** Partner leaderboard ignores the partner filter so partners can be compared. Best first. */
    public List<GroupStat> partnerLeaderboard(Filter f) {
        return group("pr.name", f.withoutPartner()).stream()
                .sorted(Comparator.comparingDouble(GroupStat::lateRate)).toList();
    }

    public List<GroupStat> hubLeaderboard(Filter f) {
        return group("h.name", f).stream()
                .sorted(Comparator.comparingDouble(GroupStat::lateRate)).toList();
    }

    /** Late rate by weather and by festival vs normal days. */
    public List<GroupStat> factors(Filter f) {
        List<GroupStat> out = new ArrayList<>(group("d.weather", f));
        out.addAll(group("CASE WHEN d.festival THEN 'Festival days' ELSE 'Normal days' END", f));
        return out;
    }

    private List<GroupStat> group(String labelExpr, Filter f) {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT " + labelExpr + " AS label, COUNT(*) AS orders,"
                + " AVG(" + LATE + ") AS late_rate, AVG(" + FIRST + ") AS fa_rate,"
                + " AVG(d.delay_hours) AS avg_delay, SUM(d.order_value) AS total_value"
                + FROM_JOIN + where(f, args) + " GROUP BY " + labelExpr;
        RowMapper<GroupStat> mapper = (rs, i) -> {
            double lateRate = rs.getDouble("late_rate");
            double value = rs.getDouble("total_value");
            return new GroupStat(rs.getString("label"), rs.getLong("orders"), lateRate,
                    rs.getDouble("fa_rate"), rs.getDouble("avg_delay"), value, value * lateRate);
        };
        return jdbc.query(sql, mapper, args.toArray());
    }

    // ------------------------------------------------------------------ helpers

    /** Builds the WHERE clause. Only fixed SQL fragments are appended; values go in as bind parameters. */
    private String where(Filter f, List<Object> args) {
        StringBuilder sb = new StringBuilder(" WHERE d.dispatch_date BETWEEN ? AND ?");
        args.add(Date.valueOf(f.from()));
        args.add(Date.valueOf(f.to()));
        if (f.partner() != null && !f.partner().isBlank()) {
            sb.append(" AND d.partner_id = ?");
            args.add(f.partner());
        }
        if (f.district() != null && !f.district().isBlank()) {
            sb.append(" AND p.district = ?");
            args.add(f.district());
        }
        return sb.toString();
    }
}
