package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.ax.platform.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
