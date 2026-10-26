package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.CompanyDocument;

import java.util.List;
import java.util.Optional;

public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, Long> {

    List<CompanyDocument> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Optional<CompanyDocument> findByIdAndCompanyId(Long id, Long companyId);

    long countByCompanyId(Long companyId);
}
