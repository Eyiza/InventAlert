package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.service.GoogleMapsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class GoogleMapsServiceImpl implements GoogleMapsService {

    @Value("${google.maps.api-key:}")
    private String apiKey;

    @Value("${google.maps.timeout-seconds:3}")
    private int timeoutSeconds;

    @Override
    @Cacheable(value = "distanceCache", key = "#fromId + '_' + #toId")
    public Double getDrivingDistanceKm(String fromId, BigDecimal fromLat, BigDecimal fromLng,
                                        String toId, BigDecimal toLat, BigDecimal toLng) {
        if (apiKey == null || apiKey.isBlank()) {
            return haversine(fromLat, fromLng, toLat, toLng);
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutSeconds * 1000);
            factory.setReadTimeout(timeoutSeconds * 1000);
            RestTemplate restTemplate = new RestTemplate(factory);

            String url = "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=" + fromLat + "," + fromLng +
                    "&destination=" + toLat + "," + toLng +
                    "&key=" + apiKey;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                @SuppressWarnings("unchecked")
                var routes = (java.util.List<Map<String, Object>>) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    var legs = (java.util.List<Map<String, Object>>) routes.get(0).get("legs");
                    if (legs != null && !legs.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        var distance = (Map<String, Object>) legs.get(0).get("distance");
                        if (distance != null) {
                            int meters = (Integer) distance.get("value");
                            return meters / 1000.0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Google Maps API call failed, falling back to Haversine: {}", e.getMessage());
        }
        return haversine(fromLat, fromLng, toLat, toLng);
    }

    private double haversine(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
