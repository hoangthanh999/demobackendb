package com.badminton.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ResendEmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("📧 Sending password reset email via Resend");
        log.info("📧 To: {}", toEmail);
        log.info("📧 From: {}", fromEmail);
        log.info("📧 Token: {}", token);

        try {
            String htmlContent = buildResetEmailHtml(token);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("from", fromEmail);
            emailData.put("to", new String[] { toEmail });
            emailData.put("subject", "🔑 Đặt lại mật khẩu - Badminton Court Management");
            emailData.put("html", htmlContent);

            String jsonBody = objectMapper.writeValueAsString(emailData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.info("📤 Sending request to Resend API...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("📨 Resend API Response Status: {}", response.statusCode());
            log.info("📨 Resend API Response Body: {}", response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("✅ Email sent successfully via Resend!");
            } else {
                log.error("❌ Failed to send email. Status: {}, Body: {}",
                        response.statusCode(), response.body());
                throw new RuntimeException("Failed to send email via Resend: " + response.body());
            }

        } catch (Exception e) {
            log.error("❌ Error sending email via Resend: ", e);
            throw new RuntimeException("Không thể gửi email: " + e.getMessage(), e);
        }
    }

    private String buildResetEmailHtml(String token) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            margin: 0;
                            padding: 0;
                            background-color: #f5f5f5;
                        }
                        .container {
                            max-width: 600px;
                            margin: 40px auto;
                            background: white;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 40px 30px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0 0 10px 0;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        .header p {
                            margin: 0;
                            font-size: 16px;
                            opacity: 0.9;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .content p {
                            margin: 0 0 15px 0;
                            font-size: 16px;
                            line-height: 1.6;
                        }
                        .token-box {
                            background: #f8f9fa;
                            border: 2px dashed #667eea;
                            border-radius: 8px;
                            padding: 20px;
                            margin: 25px 0;
                            text-align: center;
                        }
                        .token {
                            font-family: 'Courier New', monospace;
                            font-size: 24px;
                            font-weight: bold;
                            color: #667eea;
                            letter-spacing: 2px;
                            word-break: break-all;
                        }
                        .warning {
                            background: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .warning p {
                            margin: 0;
                            color: #856404;
                            font-weight: 500;
                        }
                        .footer {
                            background: #f8f9fa;
                            padding: 30px;
                            text-align: center;
                            color: #6c757d;
                            font-size: 14px;
                            border-top: 1px solid #e9ecef;
                        }
                        .footer p {
                            margin: 5px 0;
                        }
                        .instructions {
                            background: #e7f3ff;
                            border-left: 4px solid #2196F3;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .instructions p {
                            margin: 5px 0;
                            color: #0d47a1;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🏸 Badminton Court Management</h1>
                            <p>Yêu cầu đặt lại mật khẩu</p>
                        </div>
                        <div class="content">
                            <p>Xin chào,</p>
                            <p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>

                            <div class="instructions">
                                <p><strong>📱 Hướng dẫn:</strong></p>
                                <p>Sao chép mã token bên dưới và nhập vào ứng dụng để đặt lại mật khẩu.</p>
                            </div>

                            <div class="token-box">
                                <div class="token">%s</div>
                            </div>

                            <div class="warning">
                                <p>⚠️ Mã này chỉ có hiệu lực trong <strong>1 giờ</strong>.</p>
                            </div>

                            <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                               Mật khẩu của bạn sẽ không bị thay đổi.</p>
                        </div>
                        <div class="footer">
                            <p><strong>© 2024 Badminton Court Management</strong></p>
                            <p>All rights reserved.</p>
                            <p style="margin-top: 15px; font-size: 12px;">
                                Email này được gửi tự động, vui lòng không trả lời.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(token);
    }
}
