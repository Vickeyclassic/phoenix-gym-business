package com.phoenix.business.domain;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="revoked_tokens") @Getter @Setter @NoArgsConstructor
public class RevokedToken { @Id private String jti; @Column(name="expires_at",nullable=false) private Instant expiresAt; @Column(name="revoked_at",insertable=false,updatable=false) private Instant revokedAt; public RevokedToken(String jti,Instant expiresAt){this.jti=jti;this.expiresAt=expiresAt;} }
