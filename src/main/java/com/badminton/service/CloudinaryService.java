// backend/src/main/java/com/badminton/service/CloudinaryService.java
package com.badminton.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    private Cloudinary getCloudinary() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true));
        }
        return cloudinary;
    }

    /**
     * Delete image from Cloudinary
     * 
     * @param publicId The public_id of the image (e.g., "courts/court_123456.jpg")
     * @return true if deleted successfully
     */
    public boolean deleteImage(String publicId) {
        try {
            log.info("🗑️ Deleting image from Cloudinary: {}", publicId);

            Map result = getCloudinary().uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");

            if ("ok".equals(resultStatus)) {
                log.info("✅ Image deleted successfully: {}", publicId);
                return true;
            } else {
                log.warn("⚠️ Image deletion returned status: {} for {}", resultStatus, publicId);
                return false;
            }
        } catch (IOException e) {
            log.error("❌ Error deleting image from Cloudinary: {}", publicId, e);
            return false;
        }
    }

    /**
     * Delete multiple images
     */
    public void deleteImages(java.util.List<String> publicIds) {
        for (String publicId : publicIds) {
            deleteImage(publicId);
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     * 
     * @param url Full Cloudinary URL
     * @return public_id (e.g., "courts/court_123456")
     */
    public String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary.com")) {
            return null;
        }

        try {
            // URL format:
            // https://res.cloudinary.com/{cloud_name}/image/upload/{transformations}/{public_id}.{format}
            // We need to extract: {public_id}.{format}

            String[] parts = url.split("/upload/");
            if (parts.length < 2) {
                return null;
            }

            String afterUpload = parts[1];

            // Remove transformations (e.g., "w_800,h_600,c_fill/")
            String[] segments = afterUpload.split("/");

            // Get the last segment (public_id with extension)
            String lastSegment = segments[segments.length - 1];

            // Remove file extension
            int lastDotIndex = lastSegment.lastIndexOf('.');
            if (lastDotIndex > 0) {
                lastSegment = lastSegment.substring(0, lastDotIndex);
            }

            // If there's a folder, include it
            if (segments.length > 1) {
                // Find where the actual path starts (after transformations)
                int pathStartIndex = 0;
                for (int i = 0; i < segments.length - 1; i++) {
                    if (!segments[i].contains(",") && !segments[i].contains("_")) {
                        pathStartIndex = i;
                        break;
                    }
                }

                StringBuilder publicId = new StringBuilder();
                for (int i = pathStartIndex; i < segments.length; i++) {
                    if (i > pathStartIndex) {
                        publicId.append("/");
                    }
                    if (i == segments.length - 1) {
                        // Last segment - remove extension
                        int dotIndex = segments[i].lastIndexOf('.');
                        publicId.append(dotIndex > 0 ? segments[i].substring(0, dotIndex) : segments[i]);
                    } else {
                        publicId.append(segments[i]);
                    }
                }
                return publicId.toString();
            }

            return lastSegment;
        } catch (Exception e) {
            log.error("❌ Error extracting public_id from URL: {}", url, e);
            return null;
        }
    }
}
