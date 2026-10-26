package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import pro.ax.platform.domain.Company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    Optional<Company> findBySlug(String slug);

    Optional<Company> findByOwnerId(Long ownerId);

    boolean existsBySlug(String slug);

    long countByVerifiedTrue();

    long countByPlanNot(pro.ax.platform.domain.Plan plan);

    List<Company> findByPlanNotAndPromotedUntilBefore(pro.ax.platform.domain.Plan plan, java.time.Instant moment);

    List<Company> findByVerifiedFalseOrderByCreatedAtDesc();

    List<Company> findTop6ByVerifiedTrueOrderByRatingDescReviewsCountDesc();

    @Query("select distinct c.city from Company c where c.city is not null and c.city <> '' order by c.city")
    List<String> findDistinctCities();

    @Query("select c.industry.code, count(c) from Company c group by c.industry.code")
    List<Object[]> countGroupedByIndustry();
}
