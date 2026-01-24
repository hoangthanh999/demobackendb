package com.badminton.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Data
public class VNPayConfig {
    private String tmnCode; // Mã website
    private String hashSecret; // Secret key
    private String url; // URL thanh toán
    private String returnUrl; // URL callback
    private String apiUrl; // API query transaction
    private String version;
    private String command;
    private String orderType;
    private String locale;
}
