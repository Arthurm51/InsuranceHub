package br.com.insurehub.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:userId is null or a.userId = :userId)
              and (:action is null or a.action = :action)
            order by a.id desc
            """)
    Page<AuditLog> search(@Param("userId") Long userId, @Param("action") String action, Pageable pageable);
}
