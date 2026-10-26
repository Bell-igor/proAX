package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.EscrowPayment;

import java.util.Optional;

public interface EscrowPaymentRepository extends JpaRepository<EscrowPayment, Long> {

    Optional<EscrowPayment> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);
}
