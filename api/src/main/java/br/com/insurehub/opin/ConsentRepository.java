package br.com.insurehub.opin;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    Optional<Consent> findByCustomerIdAndStatus(Long customerId, Consent.Status status);
}
