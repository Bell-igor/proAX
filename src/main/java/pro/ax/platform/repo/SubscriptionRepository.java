package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pro.ax.platform.domain.Subscription;

import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    @Query("select coalesce(sum(s.amount), 0) from Subscription s")
    BigDecimal totalRevenue();
}
