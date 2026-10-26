package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.Industry;

import java.util.List;
import java.util.Optional;

public interface IndustryRepository extends JpaRepository<Industry, Long> {

    List<Industry> findAllByOrderBySortOrderAsc();

    Optional<Industry> findByCode(String code);

    boolean existsByCode(String code);
}
