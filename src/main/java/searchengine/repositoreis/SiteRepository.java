package searchengine.repositoreis;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Site;
import searchengine.model.SiteStatus;

import java.util.Optional;
import java.util.Set;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    Set<Site> findAllByStatus(SiteStatus status);

    boolean existsByIdAndStatus(Integer id, SiteStatus status);

    Site findByUrl (String url);

    Optional<Site> findByUrlIgnoreCase(String url);

    boolean existsByStatus(SiteStatus status);
}
