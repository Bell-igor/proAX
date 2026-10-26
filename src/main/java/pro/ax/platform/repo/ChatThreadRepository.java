package pro.ax.platform.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.ax.platform.domain.ChatThread;

import java.util.List;
import java.util.Optional;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {

    Optional<ChatThread> findByNeedIdAndCompanyId(Long needId, Long companyId);

    @Query("""
            select t from ChatThread t
            where t.need.author.id = :userId or t.company.owner.id = :userId
            order by t.lastMessageAt desc
            """)
    List<ChatThread> findForUser(@Param("userId") Long userId);

    @Query("""
            select count(t) from ChatThread t
            where t.need.author.id = :userId or t.company.owner.id = :userId
            """)
    long countForUser(@Param("userId") Long userId);
}
