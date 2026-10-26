package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.LegalService;

import java.util.List;

public interface LegalServiceRepository extends JpaRepository<LegalService, Long> {

    List<LegalService> findByActiveTrueOrderByPriceAsc();

    List<LegalService> findByProviderIdAndActiveTrueOrderByPriceAsc(Long providerId);

    List<LegalService> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
