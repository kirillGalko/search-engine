package searchengine.repositoreis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query("SELECT p FROM Page p where p.path = :path")
    Page findByPath(String path);

    boolean existsByPath (String path);
    boolean existsBySiteIdAndPath(Integer siteId, String path);

    Optional<Page> findBySiteAndPath(Site site, String path);
    long countBySite(Site site);

}
