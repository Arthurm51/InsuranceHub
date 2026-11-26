package br.com.insurehub.opin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consents")
public class Consent {

    public enum Status { ACTIVE, REVOKED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    protected Consent() {}

    public Consent(Long customerId, Instant grantedAt, Instant expiresAt) {
        this.customerId = customerId;
        this.status = Status.ACTIVE;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        this.status = Status.REVOKED;
        this.revokedAt = now;
        this.lastSyncAt = null;
    }

    public void expire() {
        this.status = Status.EXPIRED;
    }

    public void markSynced(Instant when) {
        this.lastSyncAt = when;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Status getStatus() { return status; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getLastSyncAt() { return lastSyncAt; }
}
