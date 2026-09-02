package com.phoenix.business.domain;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false) private String action; @Column(name="entity_type") private String entityType; @Column(name="entity_id") private String entityId; @ManyToOne @JoinColumn(name="actor_user_id") private User actor; @Column(name="actor_username",nullable=false) private String actorUsername; private String summary; @Column(name="created_at",insertable=false,updatable=false) private Instant createdAt; }
