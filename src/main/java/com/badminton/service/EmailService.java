package com.badminton.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String FROM_EMAIL = "Badminton App <onboarding@resend.dev>";

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String apiKey = System.getenv("RESEND_API_KEY");
            if (apiKey == null) {
                throw new RuntimeException("RESEND_API_KEY not set");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", FROM_EMAIL);
            payload.put("to", toEmail);
            payload.put("subject", "🔑 Đặt lại mật khẩu");

            payload.put("html", buildResetEmailHtml(token));

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(payload);

            URL url = new URL(RESEND_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));

            int status = conn.getResponseCode();
            log.info("📧 Resend response status: {}", status);

            if (status >= 400) {
                throw new RuntimeException("Failed to send email via Resend");
            }

            log.info("✅ Email sent successfully via Resend!");

        } catch (Exception e) {
            log.error("❌ Error sending email via Resend", e);
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    private String buildResetEmailHtml(String token) {
        return """
                    <div style="font-family:Arial">
                      <h2>🏸 Badminton Court Management</h2>
                      <p>Mã đặt lại mật khẩu của bạn:</p>
                      <h3 style="background:#eee;padding:10px">%s</h3>
                      <p>Mã có hiệu lực trong 1 giờ.</p>
                    </div>
                """.formatted(token);
    }
}
