package com.delivery.api.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One delivered order. Rows are bulk-inserted by DataSeeder (JDBC batch),
 * this entity mainly defines the table schema for Hibernate.
 */
@Entity
@Table(name = "delivery", indexes = {
        @Index(name = "idx_delivery_date", columnList = "dispatch_date"),
        @Index(name = "idx_delivery_pincode", columnList = "pincode")
})
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pincode;
    @Column(name = "hub_id")
    private String hubId;
    @Column(name = "partner_id")
    private String partnerId;
    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;
    private String weather;
    private boolean festival;
    private int attempts;
    @Column(name = "order_value")
    private double orderValue;
    @Column(name = "promised_hours")
    private double promisedHours;
    @Column(name = "actual_hours")
    private double actualHours;
    @Column(name = "delay_hours")
    private double delayHours;
    private boolean late;

    public Long getId() {
        return id;
    }

    public String getPincode() {
        return pincode;
    }

    public String getHubId() {
        return hubId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public String getWeather() {
        return weather;
    }

    public boolean isFestival() {
        return festival;
    }

    public int getAttempts() {
        return attempts;
    }

    public double getOrderValue() {
        return orderValue;
    }

    public double getPromisedHours() {
        return promisedHours;
    }

    public double getActualHours() {
        return actualHours;
    }

    public double getDelayHours() {
        return delayHours;
    }

    public boolean isLate() {
        return late;
    }
}
