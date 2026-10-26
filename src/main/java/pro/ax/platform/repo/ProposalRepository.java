package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.Proposal;
import pro.ax.platform.domain.ProposalStatus;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    List<Proposal> findByNeedIdOrderByCreatedAtDesc(Long needId);

    List<Proposal> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Proposal> findByNeedAuthorIdOrderByCreatedAtDesc(Long authorId);

    boolean existsByNeedIdAndCompanyId(Long needId, Long companyId);

    Optional<Proposal> findByIdAndCompanyId(Long id, Long companyId);

    long countByCompanyIdAndStatus(Long companyId, ProposalStatus status);
}
