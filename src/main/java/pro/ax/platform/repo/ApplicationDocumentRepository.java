package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.ApplicationDocument;

import java.util.List;
import java.util.Optional;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {

    List<ApplicationDocument> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    Optional<ApplicationDocument> findByIdAndApplicationId(Long id, Long applicationId);
}
