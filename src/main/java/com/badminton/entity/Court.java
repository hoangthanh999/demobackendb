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
@Table(name = "courts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private Integer numberOfCourts;

    @Column(columnDefinition = "TEXT")
    private String facilities; // Lưu dạng JSON string: ["Parking", "Shower", "Locker"]

    @Column(columnDefinition = "TEXT")
    private String images; // Lưu dạng JSON string: ["url1", "url2"]

    @Column(nullable = false)
    private String openTime; // Format: "06:00"

    @Column(nullable = false)
    private String closeTime; // Format: "22:00"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourtStatus status = CourtStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL)
    private List<TimeSlot> timeSlots;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL)
    private List<Booking> bookings;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum CourtStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
}
