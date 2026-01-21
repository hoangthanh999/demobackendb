// service/MailjetEmailService.java
package com.badminton.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MailjetEmailService {

    @Value("${mailjet.api-key}")
    private String apiKey;

    @Value("${mailjet.secret-key}")
    private String secretKey;

    @Value("${mailjet.from-email}")
    private String fromEmail;

    @Value("${mailjet.from-name:Badminton Court}")
    private String fromName;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("📧 Sending password reset email via Mailjet");
        log.info("📧 To: {}", toEmail);
        log.info("📧 From: {} <{}>", fromName, fromEmail);

        try {
            String htmlContent = buildResetEmailHtml(token);

            // Mailjet API v3.1 format
            Map<String, Object> from = new HashMap<>();
            from.put("Email", fromEmail);
            from.put("Name", fromName);

            Map<String, Object> to = new HashMap<>();
            to.put("Email", toEmail);

            Map<String, Object> message = new HashMap<>();
            message.put("From", from);
            message.put("To", List.of(to));
            message.put("Subject", "🔑 Đặt lại mật khẩu - Badminton Court Management");
            message.put("HTMLPart", htmlContent);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("Messages", List.of(message));

            String jsonBody = objectMapper.writeValueAsString(emailData);

            // Basic Auth: base64(apiKey:secretKey)
            String auth = apiKey + ":" + secretKey;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mailjet.com/v3.1/send"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("📤 Sending request to Mailjet API...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("📨 Mailjet API Response Status: {}", response.statusCode());
            log.info("📨 Mailjet API Response Body: {}", response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("✅ Email sent successfully via Mailjet!");
            } else {
                log.error("❌ Failed to send email. Status: {}, Body: {}",
                        response.statusCode(), response.body());
                throw new RuntimeException("Failed to send email via Mailjet: " + response.body());
            }

        } catch (Exception e) {
            log.error("❌ Error sending email via Mailjet: ", e);
            throw new RuntimeException("Không thể gửi email: " + e.getMessage(), e);
        }
    }

    private String buildResetEmailHtml(String token) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        </head>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5;">
                            <div style="max-width: 600px; margin: 40px auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 40px 30px; text-align: center;">
                                    <h1 style="margin: 0 0 10px 0; font-size: 28px;">🏸 Badminton Court Management</h1>
                                    <p style="margin: 0; font-size: 16px;">Yêu cầu đặt lại mật khẩu</p>
                                </div>
                                <div style="padding: 40px 30px;">
                                    <p>Xin chào,</p>
                                    <p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                                    <div style="background: #e7f3ff; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                        <p style="margin: 5px 0; color: #0d47a1;"><strong>📱 Hướng dẫn:</strong></p>
                                        <p style="margin: 5px 0; color: #0d47a1;">Sao chép mã token bên dưới và nhập vào ứng dụng để đặt lại mật khẩu.</p>
                                    </div>
                                    <div style="background: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: center;">
                                        <div style="font-family: 'Courier New', monospace; font-size: 24px; font-weight: bold; color: #667eea; letter-spacing: 2px; word-break: break-all;">%s</div>
                                    </div>
                                    <div style="background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                        <p style="margin: 0; color: #856404; font-weight: 500;">⚠️ Mã này chỉ có hiệu lực trong <strong>1 giờ</strong>.</p>
                                    </div>
                                    <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này. Mật khẩu của bạn sẽ không bị thay đổi.</p>
                                </div>
                                <div style="background: #f8f9fa; padding: 30px; text-align: center; color: #6c757d; font-size: 14px; border-top: 1px solid #e9ecef;">
                                    <p style="margin: 5px 0;"><strong>© 2024 Badminton Court Management</strong></p>
                                    <p style="margin: 5px 0;">All rights reserved.</p>
                                    <p style="margin-top: 15px; font-size: 12px;">Email này được gửi tự động, vui lòng không trả lời.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                token);
    }
}
