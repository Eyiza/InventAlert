package com.inventalert.inventoryService.service.impl;

import com.inventalert.inventoryService.model.DistanceSource;
import com.inventalert.inventoryService.service.DistanceResult;
import com.inventalert.inventoryService.service.GoogleMapsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
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
    public DistanceResult getDrivingDistanceKm(String fromId, BigDecimal fromLat, BigDecimal fromLng,
                                                String toId, BigDecimal toLat, BigDecimal toLng) {
        if (apiKey == null || apiKey.isBlank()) {
            return new DistanceResult(haversine(fromLat, fromLng, toLat, toLng), DistanceSource.HAVERSINE);
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutSeconds * 1000);
            factory.setReadTimeout(timeoutSeconds * 1000);
            RestTemplate restTemplate = new RestTemplate(factory);

            String url = "https://maps.googleapis.com/maps/api/distancematrix/json"
                    + "?origins=" + fromLat + "," + fromLng
                    + "&destinations=" + toLat + "," + toLng
                    + "&mode=driving"
                    + "&key=" + apiKey;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                @SuppressWarnings("unchecked")
                var rows = (List<Map<String, Object>>) response.get("rows");
                if (rows != null && !rows.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    var elements = (List<Map<String, Object>>) rows.get(0).get("elements");
                    if (elements != null && !elements.isEmpty()) {
                        Map<String, Object> element = elements.get(0);
                        if ("OK".equals(element.get("status"))) {
                            @SuppressWarnings("unchecked")
                            var distance = (Map<String, Object>) element.get("distance");
                            if (distance != null) {
                                int meters = (Integer) distance.get("value");
                                return new DistanceResult(meters / 1000.0, DistanceSource.GOOGLE_MAPS);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Google Maps API call failed, falling back to Haversine: {}", e.getMessage());
        }
        return new DistanceResult(haversine(fromLat, fromLng, toLat, toLng), DistanceSource.HAVERSINE);
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
