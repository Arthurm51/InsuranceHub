package br.com.insurehub.claim;

import java.util.List;

public enum ClaimStatus {
    OPEN,
    IN_ANALYSIS,
    APPROVED,
    REJECTED,
    PAID,
    CANCELLED;

    /** Sinistros "em aberto": bloqueiam novo sinistro e cancelamento da apólice. */
    public static final List<ClaimStatus> PENDING = List.of(OPEN, IN_ANALYSIS);

    /** Sinistros que consomem saldo de cobertura. */
    public static final List<ClaimStatus> CONSUMING_COVERAGE = List.of(APPROVED, PAID);
}
