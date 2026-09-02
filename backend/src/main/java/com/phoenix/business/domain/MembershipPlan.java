package com.phoenix.business.domain;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="membership_plans") @Getter @Setter @NoArgsConstructor
public class MembershipPlan { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String name; @Column(name="duration_months",nullable=false) private int durationMonths; @Column(nullable=false) private BigDecimal price; private String description; @Column(nullable=false) private boolean active=true; @Column(name="created_at",insertable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",insertable=false,updatable=false) private Instant updatedAt; }
