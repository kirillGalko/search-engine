package searchengine.repositoreis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Set;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    int countByLemma(Lemma lemma);

    List<Index> findAllByLemmaId (int lemmaId);

    Set<Index> findAllByLemmaAndPageIn(Lemma lemma, Set<Page> pages);

    Set<Index> findAllByPageAndLemmaIn(Page page, Set<Lemma> lemmas);
    Set<Index> findAllByPage(Page page);

}
