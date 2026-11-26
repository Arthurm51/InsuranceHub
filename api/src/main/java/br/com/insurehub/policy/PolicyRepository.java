package br.com.insurehub.policy;

import br.com.insurehub.quote.ProductLine;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByCustomerIdOrderByIdDesc(Long customerId);

    List<Policy> findByCustomerIdAndStatusOrderByIdDesc(Long customerId, Policy.Status status);

    Optional<Policy> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByCustomerIdAndLineAndStatus(Long customerId, ProductLine line, Policy.Status status);

    List<Policy> findByStatusAndEndDateBefore(Policy.Status status, LocalDate date);

    @Query(value = "select nextval('policy_number_seq')", nativeQuery = true)
    long nextNumber();
}
