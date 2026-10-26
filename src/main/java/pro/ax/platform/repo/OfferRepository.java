package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.Offer;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Offer> findByCompanyIdAndActiveTrueOrderByCreatedAtDesc(Long companyId);

    Optional<Offer> findByIdAndCompanyId(Long id, Long companyId);

    long countByCompanyIdAndActiveTrue(Long companyId);

    long countByActiveTrue();

    List<Offer> findTop6ByActiveTrueOrderByCreatedAtDesc();
}
