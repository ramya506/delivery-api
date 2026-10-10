package com.delivery.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pincode")
public class Pincode {

    @Id
    private String code;
    private String town;
    private String district;
    private double lat;
    private double lon;
    @Column(name = "hub_id")
    private String hubId;
    @Column(name = "dist_km")
    private int distKm;

    protected Pincode() {
    }

    public Pincode(String code, String town, String district, double lat, double lon, String hubId, int distKm) {
        this.code = code;
        this.town = town;
        this.district = district;
        this.lat = lat;
        this.lon = lon;
        this.hubId = hubId;
        this.distKm = distKm;
    }

    public String getCode() {
        return code;
    }

    public String getTown() {
        return town;
    }

    public String getDistrict() {
        return district;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getHubId() {
        return hubId;
    }

    public int getDistKm() {
        return distKm;
    }
}
