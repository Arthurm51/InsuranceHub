package br.com.insurehub.claim;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    boolean existsByPolicyIdAndStatusIn(Long policyId, Collection<ClaimStatus> statuses);

    List<Claim> findByCustomerIdOrderByIdDesc(Long customerId);

    Optional<Claim> findByIdAndCustomerId(Long id, Long customerId);

    List<Claim> findAllByOrderByIdDesc();

    List<Claim> findByStatusOrderByIdDesc(ClaimStatus status);

    @Query("""
            select coalesce(sum(c.approvedAmount), 0) from Claim c
            where c.policyId = :policyId and c.status in :statuses
            """)
    BigDecimal sumApprovedAmount(@Param("policyId") Long policyId,
                                 @Param("statuses") Collection<ClaimStatus> statuses);

    @Query(value = "select nextval('claim_protocol_seq')", nativeQuery = true)
    long nextProtocol();
}
