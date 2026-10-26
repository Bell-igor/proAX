package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.LegalApplication;
import pro.ax.platform.domain.LegalApplicationStatus;

import java.util.List;

public interface LegalApplicationRepository extends JpaRepository<LegalApplication, Long> {

    List<LegalApplication> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<LegalApplication> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    long countByProviderIdAndStatusIn(Long providerId, List<LegalApplicationStatus> statuses);

    boolean existsByAuthorIdAndProviderIdAndStatusNotIn(Long authorId, Long providerId,
                                                        List<LegalApplicationStatus> statuses);
}
