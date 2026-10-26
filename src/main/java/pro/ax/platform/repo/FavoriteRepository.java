package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.ax.platform.domain.Favorite;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select f.company.id from Favorite f where f.user.id = :userId")
    List<Long> findCompanyIdsByUserId(@Param("userId") Long userId);

    Optional<Favorite> findByUserIdAndCompanyId(Long userId, Long companyId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    void deleteByUserIdAndCompanyId(Long userId, Long companyId);
}
