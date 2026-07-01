package com.car.sharing.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(name = "session_url", nullable = false)
    private String sessionUrl;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "amount_to_pay", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountToPay;

    public enum Status {
        PENDING,
        PAID,
        CANCELED,
        EXPIRED
    }

    public enum Type {
        PAYMENT,
        FINE
    }
}
