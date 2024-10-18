package searchengine.services.lemma;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositoreis.IndexRepository;
import searchengine.repositoreis.LemmaRepository;
import searchengine.repositoreis.SiteRepository;
import searchengine.services.indexing.HtmlParser;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
@Slf4j
public class LemmaService {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final LemmaParser lemmaParser;
    private final EnglishLemmaParser englishLemmaParser;

    public void findAndSave(Page page) {
        String text = htmlToText(page.getContent());
        Map<String, Integer> lemmas = lemmaParser.collectLemmas(text);
        Map<String, Integer> englishLemmas = englishLemmaParser.collectLemmas(text);
        lemmas.putAll(englishLemmas);
        CopyOnWriteArraySet<Lemma> lemmaSetToSave = new CopyOnWriteArraySet<>();
        CopyOnWriteArraySet<Index> indices = new CopyOnWriteArraySet<>();
//        synchronized (lemmaRepository) {
            lemmas.forEach((name, count) -> {
                List<Lemma> optionalLemma = lemmaRepository.findBySiteAndLemma(page.getSite(), name);
                Lemma lemma;
                if (!optionalLemma.isEmpty()) {
                    lemma = optionalLemma.get(0);
                    int currentFrequency = lemma.getFrequency();
                    lemma.setFrequency(currentFrequency + 1);
                    lemmaSetToSave.add(lemma);
                } else {
                    lemma = new Lemma();
                    lemma.setFrequency(1);
                    lemma.setSite(page.getSite());
                    lemma.setLemma(name);
                    lemmaSetToSave.add(lemma);
                }


                Index index = new Index();
                index.setLemma(lemma);
                index.setRank((float) count);
                index.setPage(page);
                indices.add(index);

            });
            lemmaRepository.saveAll(lemmaSetToSave);
//        }
        indexRepository.saveAll(indices);
    }

    public static String htmlToText(String content) {
        Cleaner cleaner = new Cleaner(Safelist.none());
        return cleaner.clean(Jsoup.parse(content)).text();
    }
}
