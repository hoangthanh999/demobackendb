package com.badminton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_phone", columnList = "phone"),
        @Index(name = "idx_user_tier", columnList = "tier"),
        @Index(name = "idx_user_location", columnList = "latitude,longitude")
})

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private Boolean active = true;

    // ✅ NEW: User Tier System
    @Column(nullable = false)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserTier tier = UserTier.BRONZE;

    @Column(nullable = false)
    private Integer depositPercentage = 30; // Default 30%

    // ✅ NEW: Location
    private Double latitude;
    private Double longitude;
    private String address;
    private String province;
    private String district;
    private String ward;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Court> courts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ChatMessage> chatMessages;

    // Manual Getters/Setters to fix Lombok issues
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public enum UserRole {
        USER, OWNER, ADMIN
    }

    public enum UserTier {
        BRONZE(0, 2_000_000, 30), // 0-2tr: 30% cọc
        SILVER(2_000_000, 5_000_000, 25), // 2-5tr: 25%
        GOLD(5_000_000, 10_000_000, 20), // 5-10tr: 20%
        PLATINUM(10_000_000, 20_000_000, 15), // 10-20tr: 15%
        DIAMOND(20_000_000, 50_000_000, 10), // 20-50tr: 10%
        VIP(50_000_000, Long.MAX_VALUE, 0); // >50tr: 0% (không cần cọc)

        private final long minSpent;
        private final long maxSpent;
        private final int depositPercentage;

        UserTier(long minSpent, long maxSpent, int depositPercentage) {
            this.minSpent = minSpent;
            this.maxSpent = maxSpent;
            this.depositPercentage = depositPercentage;
        }

        public long getMinSpent() {
            return minSpent;
        }

        public long getMaxSpent() {
            return maxSpent;
        }

        public int getDepositPercentage() {
            return depositPercentage;
        }

        public static UserTier fromTotalSpent(BigDecimal totalSpent) {
            long spent = totalSpent.longValue();
            for (UserTier tier : values()) {
                if (spent >= tier.minSpent && spent < tier.maxSpent) {
                    return tier;
                }
            }
            return VIP;
        }
    }
}
