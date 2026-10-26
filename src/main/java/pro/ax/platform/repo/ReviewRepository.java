package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.ax.platform.domain.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    boolean existsByCompanyIdAndAuthorId(Long companyId, Long authorId);

    Optional<Review> findByIdAndAuthorId(Long id, Long authorId);

    long countByCompanyId(Long companyId);

    @Query("select avg(r.rating) from Review r where r.company.id = :companyId")
    Double averageRating(@Param("companyId") Long companyId);
}
