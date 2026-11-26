package br.com.insurehub.quote;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByCustomerIdOrderByIdDesc(Long customerId);

    Optional<Quote> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByCustomerIdAndExternalPolicyId(Long customerId, String externalPolicyId);
}
