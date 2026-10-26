package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import pro.ax.platform.domain.NeedRequest;
import pro.ax.platform.domain.NeedStatus;

import java.util.List;

public interface NeedRequestRepository extends JpaRepository<NeedRequest, Long>, JpaSpecificationExecutor<NeedRequest> {

    List<NeedRequest> findTop8ByStatusOrderByCreatedAtDesc(NeedStatus status);

    List<NeedRequest> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    long countByStatus(NeedStatus status);

    long countByAuthorCompanyIdAndStatus(Long companyId, NeedStatus status);

    @Query("select n.industry.code, count(n) from NeedRequest n group by n.industry.code")
    List<Object[]> countGroupedByIndustry();
}
