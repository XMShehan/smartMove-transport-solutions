package com.smartmove.backend.service;

import com.smartmove.backend.data.Route;
import com.smartmove.backend.data.RouteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RouteService {
    private final RouteRepository repository;

    public RouteService(RouteRepository repository) {
        this.repository = repository;
    }

    public List<Route> getAllRoutes() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "routeId"));
    }

    public Route createRoute(Route route) {
        if (route.getDistanceKm() == null || route.getDistanceKm().signum() <= 0) {
            throw new IllegalArgumentException("distanceKm must be greater than zero");
        }
        if (route.getDurationTime() == null || route.getDurationTime() <= 0) {
            throw new IllegalArgumentException("durationTime must be greater than zero");
        }
        String status = route.getStatus() == null ? "ACTIVE" : route.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Route status must be ACTIVE or INACTIVE");
        }
        return repository.save(route);
    }
}
