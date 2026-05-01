package com.medical.medcore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscription_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true)
    private String code;
}
