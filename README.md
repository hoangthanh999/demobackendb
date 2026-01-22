# 🏸 Badminton Court Management System - Backend API

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Giới thiệu

Hệ thống quản lý sân cầu lông toàn diện với đầy đủ tính năng:

### ✨ Tính năng chính

#### 🔐 Authentication & Authorization
- Đăng ký/Đăng nhập với JWT
- Quên mật khẩu (Email OTP)
- Role-based access control (USER, OWNER, ADMIN)
- Refresh token mechanism

#### 📅 Booking System
- Đặt sân theo giờ với conflict detection
- Tự động kiểm tra trùng lịch
- Hỗ trợ đặt nhiều sân cùng lúc
- Quản lý trạng thái booking (PENDING, CONFIRMED, CANCELLED, COMPLETED)

#### 💳 Payment Integration
- Tích hợp MoMo Payment Gateway
- Mock payment mode cho testing
- QR Code payment
- Webhook handling cho real-time updates
- Deposit system theo tier

#### 👑 User Tier System
- 6 cấp độ: BRONZE → SILVER → GOLD → PLATINUM → DIAMOND → VIP
- Giảm % cọc theo cấp độ (30% → 0%)
- VIP không cần cọc khi đặt sân
- Tự động nâng cấp dựa trên tổng chi tiêu

#### 🛍️ E-commerce Features
- Quản lý sản phẩm thể thao
- Giỏ hàng (Cart)
- Đặt hàng (Order) với nhiều trạng thái
- Đánh giá sản phẩm (Review) với verified purchase
- Thống kê bán hàng

#### 📍 Location-based Services
- Tìm sân gần nhất dựa trên GPS
- Haversine algorithm cho tính khoảng cách
- Cập nhật vị trí real-time

#### 🤖 AI Chatbot
- Tích hợp OpenAI GPT
- Intent analysis (Court search, Booking, Product search, Tier info)
- Context-aware conversations
- Quick actions suggestions

#### 📧 Email Service
- Mailjet integration
- Password reset emails
- Booking confirmations
- Order notifications

#### ☁️ Cloud Storage
- Cloudinary integration
- Upload/delete images
- Automatic optimization

---

## 🛠️ Tech Stack

### Backend Framework
- **Spring Boot 3.2.0** - Main framework
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - ORM
- **Hibernate** - Database operations

### Database
- **MySQL 8.0** - Main database
- **HikariCP** - Connection pooling

### Security
- **JWT (jjwt 0.12.3)** - Token-based authentication
- **BCrypt** - Password encryption

### Payment
- **MoMo API** - Payment gateway
- **Apache HttpClient** - HTTP requests

### Storage & Communication
- **Cloudinary** - Image storage
- **Mailjet** - Email service
- **OpenAI API** - AI chatbot

### Utilities
- **Lombok** - Reduce boilerplate code
- **Jackson** - JSON processing
- **ZXing** - QR code generation
- **Apache Commons Codec** - HMAC signature

---

## 📦 Cài đặt

### Yêu cầu hệ thống
- **Java 17** hoặc cao hơn
- **Maven 3.8+**
- **MySQL 8.0+**
- **Git**

### Bước 1: Clone repository
```bash
git clone https://github.com/your-username/badminton-backend.git
cd badminton-backend
