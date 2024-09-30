package searchengine.repositoreis;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    List<Lemma> findBySiteAndLemma(Site site, String lemma);

    Set<Lemma> findAllBySite(Site site);

    long countBySite(Site site);
}
