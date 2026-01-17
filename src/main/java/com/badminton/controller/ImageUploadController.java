// backend/src/main/java/com/badminton/controller/ImageUploadController.java
package com.badminton.controller;

import com.badminton.dto.response.ApiResponse;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class ImageUploadController {

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

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {

        try {
            log.info("📤 Uploading image to Cloudinary: {}", file.getOriginalFilename());

            Map uploadResult = getCloudinary().uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"));

            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("✅ Image uploaded successfully: {}", imageUrl);

            return ResponseEntity.ok(ApiResponse.success(imageUrl, "Upload ảnh thành công"));

        } catch (IOException e) {
            log.error("❌ Error uploading image", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi upload ảnh: " + e.getMessage()));
        }
    }

    @PostMapping("/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {

        List<String> imageUrls = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                log.info("📤 Uploading image: {}", file.getOriginalFilename());

                Map uploadResult = getCloudinary().uploader().upload(file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", folder,
                                "resource_type", "image"));

                String imageUrl = (String) uploadResult.get("secure_url");
                imageUrls.add(imageUrl);
                log.info("✅ Image uploaded: {}", imageUrl);
            }

            return ResponseEntity.ok(ApiResponse.success(imageUrls,
                    "Upload " + imageUrls.size() + " ảnh thành công"));

        } catch (IOException e) {
            log.error("❌ Error uploading images", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi upload ảnh: " + e.getMessage()));
        }
    }
}
