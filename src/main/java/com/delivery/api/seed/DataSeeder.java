package com.delivery.api.seed;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.delivery.api.model.Hub;
import com.delivery.api.model.Partner;
import com.delivery.api.model.Pincode;
import com.delivery.api.repository.HubRepository;
import com.delivery.api.repository.PartnerRepository;
import com.delivery.api.repository.PincodeRepository;

/**
 * Generates a realistic, deterministic delivery dataset on startup.
 * Delays depend on distance from hub, rain (heavier in Jun-Nov), festivals,
 * partner quality and failed first attempts - so the dashboard shows real patterns.
 *
 * NOTE: pin codes / coordinates are illustrative, replace with real ones if needed.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final int DAYS = 730;
    private static final double PROMISED_HOURS = 48.0;

    // extra hours added by each partner (P01..P05)
    private static final double[] PARTNER_PENALTY = {0, 2, 8, 3, 5};

    // festival centre dates: Sankranti and Diwali (window = +/- 2 days)
    private static final List<LocalDate> FESTIVALS = List.of(
            LocalDate.of(2024, 10, 31),
            LocalDate.of(2025, 1, 14),
            LocalDate.of(2025, 10, 20),
            LocalDate.of(2026, 1, 14),
            LocalDate.of(2026, 11, 8));

    private final PartnerRepository partners;
    private final HubRepository hubs;
    private final PincodeRepository pincodes;
    private final JdbcTemplate jdbc;
    private final int rows;

    public DataSeeder(PartnerRepository partners, HubRepository hubs, PincodeRepository pincodes,
                      JdbcTemplate jdbc, @Value("${app.seed.rows}") int rows) {
        this.partners = partners;
        this.hubs = hubs;
        this.pincodes = pincodes;
        this.jdbc = jdbc;
        this.rows = rows;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (partners.count() > 0) {
            return;
        }
        long t0 = System.currentTimeMillis();

        partners.saveAll(List.of(
                new Partner("P01", "SwiftKart"),
                new Partner("P02", "RoadRunner"),
                new Partner("P03", "GramExpress"),
                new Partner("P04", "DeltaCourier"),
                new Partner("P05", "CoastLine")));

        hubs.saveAll(List.of(
                new Hub("H01", "Rajahmundry Hub"),
                new Hub("H02", "Kakinada Hub"),
                new Hub("H03", "Amalapuram Hub"),
                new Hub("H04", "Vijayawada Hub")));

        pincodes.saveAll(List.of(
                new Pincode("533255", "Ramachandrapuram", "Konaseema", 16.84, 82.02, "H03", 38),
                new Pincode("533201", "Amalapuram", "Konaseema", 16.58, 82.01, "H03", 3),
                new Pincode("533216", "Mummidivaram", "Konaseema", 16.63, 82.08, "H03", 22),
                new Pincode("533233", "Atreyapuram", "Konaseema", 16.73, 81.97, "H03", 45),
                new Pincode("533242", "Razole", "Konaseema", 16.48, 81.84, "H03", 30),
                new Pincode("533001", "Kakinada", "Kakinada", 16.99, 82.25, "H02", 5),
                new Pincode("533437", "Peddapuram", "Kakinada", 17.08, 82.14, "H02", 20),
                new Pincode("533401", "Tuni", "Kakinada", 17.36, 82.54, "H02", 45),
                new Pincode("533101", "Rajahmundry", "East Godavari", 17.00, 81.80, "H01", 4),
                new Pincode("533308", "Mandapeta", "East Godavari", 16.87, 81.93, "H01", 28),
                new Pincode("533288", "Rampachodavaram", "Alluri Sitharama Raju", 17.44, 81.77, "H01", 75),
                new Pincode("534001", "Eluru", "Eluru", 16.71, 81.10, "H04", 60),
                new Pincode("521001", "Machilipatnam", "Krishna", 16.19, 81.14, "H04", 70)));

        generateDeliveries();
        log.info("Seeded {} deliveries in {} ms", rows, System.currentTimeMillis() - t0);
    }

    private void generateDeliveries() {
        List<Pincode> pins = pincodes.findAll();
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(DAYS - 1);
        Random r = new Random(42);

        String sql = "INSERT INTO delivery (pincode, hub_id, partner_id, dispatch_date, weather, festival, "
                + "attempts, order_value, promised_hours, actual_hours, delay_hours, late) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        List<Object[]> batch = new ArrayList<>(5000);

        for (int i = 0; i < rows; i++) {
            LocalDate d = start.plusDays(r.nextInt(DAYS));
            Pincode p = pins.get(r.nextInt(pins.size()));
            int pi = r.nextInt(5);
            String partnerId = "P0" + (pi + 1);

            int month = d.getMonthValue();
            boolean monsoon = month >= 6 && month <= 11;
            double w = r.nextDouble();
            String weather;
            if (monsoon && w < 0.30) {
                weather = "Heavy Rain";
            } else if ((monsoon && w < 0.50) || w < 0.15) {
                weather = "Light Rain";
            } else {
                weather = "Clear";
            }
            boolean heavy = weather.equals("Heavy Rain");
            boolean light = weather.equals("Light Rain");
            boolean festival = isFestival(d);

            // failed first attempts are more likely far away, in heavy rain and during festivals
            double retryChance = 0.12 + (heavy ? 0.20 : 0) + p.getDistKm() * 0.002 + (festival ? 0.05 : 0);
            int attempts = 1 + (r.nextDouble() < retryChance ? 1 : 0);
            if (attempts == 2 && r.nextDouble() < 0.25) {
                attempts = 3;
            }

            double orderValue = Math.round((200 + r.nextDouble() * 4800) * 100) / 100.0;

            double actual = 14
                    + p.getDistKm() * 0.35
                    + (heavy ? 18 : light ? 6 : 0)
                    + (festival ? 10 : 0)
                    + PARTNER_PENALTY[pi]
                    + r.nextDouble() * 18
                    + (attempts - 1) * 10;
            double delay = Math.max(0, actual - PROMISED_HOURS);
            boolean late = actual > PROMISED_HOURS;

            batch.add(new Object[]{p.getCode(), p.getHubId(), partnerId, Date.valueOf(d), weather, festival,
                    attempts, orderValue, PROMISED_HOURS, round1(actual), round1(delay), late});

            if (batch.size() == 5000) {
                jdbc.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate(sql, batch);
        }
    }

    private static boolean isFestival(LocalDate d) {
        for (LocalDate f : FESTIVALS) {
            if (Math.abs(ChronoUnit.DAYS.between(f, d)) <= 2) {
                return true;
            }
        }
        return false;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
