package br.com.insurehub.opin;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExternalPolicyRepository extends JpaRepository<ExternalPolicy, Long> {

    List<ExternalPolicy> findByCustomerIdOrderByEndDateAsc(Long customerId);

    Optional<ExternalPolicy> findByCustomerIdAndExternalId(Long customerId, String externalId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExternalPolicy e where e.customerId = :customerId")
    int deleteByCustomerId(@Param("customerId") Long customerId);
}
