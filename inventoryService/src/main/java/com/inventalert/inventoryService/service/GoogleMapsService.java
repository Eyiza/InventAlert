package com.inventalert.inventoryService.service;

import java.math.BigDecimal;

public interface GoogleMapsService {
    Double getDrivingDistanceKm(String fromId, BigDecimal fromLat, BigDecimal fromLng,
                                 String toId, BigDecimal toLat, BigDecimal toLng);
}
