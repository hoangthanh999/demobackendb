// service/EmailService.java
package com.badminton.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("📧 Starting email send process");
        log.info("📧 From: {}", fromEmail);
        log.info("📧 To: {}", toEmail);
        log.info("📧 Token: {}", token);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔑 Đặt lại mật khẩu - Badminton Court Management");

            String htmlContent = buildResetEmailHtml(token);
            helper.setText(htmlContent, true);

            log.info("📤 Attempting to send email via SMTP...");
            mailSender.send(message);
            log.info("✅ Email sent successfully!");

        } catch (MessagingException e) {
            log.error("❌ MessagingException: {}", e.getMessage());
            log.error("❌ Exception type: {}", e.getClass().getName());
            log.error("❌ Full stack trace: ", e);

            // Log cause nếu có
            if (e.getCause() != null) {
                log.error("❌ Caused by: {}", e.getCause().getMessage());
                log.error("❌ Cause type: {}", e.getCause().getClass().getName());
            }

            throw new RuntimeException("Không thể gửi email: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("❌ Unexpected exception: {}", e.getMessage());
            log.error("❌ Exception type: {}", e.getClass().getName());
            log.error("❌ Full stack trace: ", e);
            throw new RuntimeException("Lỗi không xác định khi gửi email: " + e.getMessage(), e);
        }
    }

    private String buildResetEmailHtml(String token) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                            border-radius: 10px 10px 0 0;
                        }
                        .content {
                            background: #f9f9f9;
                            padding: 30px;
                            border-radius: 0 0 10px 10px;
                        }
                        .token {
                            background: #e0e0e0;
                            padding: 15px;
                            border-radius: 5px;
                            font-family: monospace;
                            word-break: break-all;
                            margin: 20px 0;
                            font-size: 16px;
                            text-align: center;
                            font-weight: bold;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 20px;
                            color: #666;
                            font-size: 12px;
                        }
                        .warning {
                            color: #e74c3c;
                            font-weight: bold;
                            margin: 15px 0;
                        }
                        h1 { margin: 0; }
                        p { margin: 10px 0; }
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
                            <p><strong>Sao chép mã token bên dưới và nhập vào ứng dụng:</strong></p>
                            <div class="token">%s</div>
                            <p class="warning">⚠️ Mã này chỉ có hiệu lực trong 1 giờ.</p>
                            <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                               Mật khẩu của bạn sẽ không bị thay đổi.</p>
                        </div>
                        <div class="footer">
                            <p>© 2024 Badminton Court Management. All rights reserved.</p>
                            <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(token);
    }
}
