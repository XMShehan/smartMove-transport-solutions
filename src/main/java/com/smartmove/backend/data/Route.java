package com.smartmove.backend.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(name = "ROUTE")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(name = "route_name", nullable = false, length = 100)
    private String routeName;

    @Column(name = "start_location", nullable = false, length = 120)
    private String startLocation;

    @Column(name = "end_location", nullable = false, length = 120)
    private String endLocation;

    @Column(name = "distance_km", nullable = false, precision = 9, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "duration_time", nullable = false)
    private Integer durationTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    protected Route() { }

    public Route(Long managerId, String routeName, String startLocation,
                 String endLocation, BigDecimal distanceKm,
                 Integer durationTime, String status) {
        this.managerId = managerId;
        this.routeName = routeName;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.distanceKm = distanceKm;
        this.durationTime = durationTime;
        this.status = (status == null || status.isBlank())
                ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
    }

    public Long getRouteId() { return routeId; }
    public Long getManagerId() { return managerId; }
    public String getRouteName() { return routeName; }
    public String getStartLocation() { return startLocation; }
    public String getEndLocation() { return endLocation; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public Integer getDurationTime() { return durationTime; }
    public String getStatus() { return status; }
}
