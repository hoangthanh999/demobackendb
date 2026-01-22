// backend/src/main/java/com/badminton/service/LocationService.java
package com.badminton.service;

import com.badminton.dto.request.UpdateLocationRequest;
import com.badminton.dto.response.CourtResponse;
import com.badminton.entity.User;

import java.util.List;

public interface LocationService {
    void updateUserLocation(Long userId, UpdateLocationRequest request);

    List<CourtResponse> findNearbyCourts(Long userId, Double radiusKm);

    Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2);
}
