package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repositoreis.IndexRepository;
import searchengine.repositoreis.LemmaRepository;
import searchengine.repositoreis.PageRepository;
import searchengine.repositoreis.SiteRepository;
import searchengine.services.lemma.LemmaService;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;

import static java.lang.Thread.sleep;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sitesList;
    private final LemmaService lemmaService;
    private static ForkJoinPool pool;

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        new Thread(() -> {
            indexingResponse.setResult(true);
        }).start();

        new  Thread(() -> {
            if (existsIndexingSite()) {
                indexingResponse.setResult(false);
                indexingResponse.setError("Индексация уже запущена");
             }else{
                deleteSites();
                log.info("Start indexing");
                pool = new ForkJoinPool();
                for (searchengine.config.Site site : sitesList.getSites()) {
                    String url = site.getUrl();
                    log.info("Save site with url: {}", url);
                    Site site1 = new Site();
                    site1.setName(site.getName());
                    site1.setUrl(url.toLowerCase());
                    site1.setStatus(SiteStatus.INDEXING);
                    site1.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site1);
                }

                for (Site site : siteRepository.findAll()) {
                    if (isFailed(site.getId())) {
                        continue;
                    }
                    log.info("Start indexing site: {}", site.getUrl());
                    pool.invoke(new HtmlParser(site.getUrl(), site, pageRepository, siteRepository, lemmaService));
                    if (isFailed(site.getId())) {
                        indexingResponse.setResult(false);
                        indexingResponse.setError(site.getLastError());
                    } else {
                        site.setStatus(SiteStatus.INDEXED);
                        site.setStatusTime(LocalDateTime.now());
                        siteRepository.save(site);
                        indexingResponse.setResult(true);
                    }

                }
            }
        }).start();
        return indexingResponse;
    }

    @Override
    public IndexingResponse stopIndexing() {
        Set<Site> sitesNotIndexed = siteRepository.findAllByStatus(SiteStatus.INDEXING);
        for (Site site : sitesNotIndexed) {
            site.setLastError("Индексация остановлена пользователем");
            site.setStatus(SiteStatus.FAILED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
        log.info("Индексация прервана пользователем");
        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(false);
        indexingResponse.setError("Индексация прервана пользователем");
        return indexingResponse;
    }

    @Override
    public IndexingResponse indexPage(String pageUrl) {
        IndexingResponse indexingResponse = new IndexingResponse();
        String cleanedUrl = pageUrl.substring(pageUrl.indexOf("=") + 1, pageUrl.length());
        cleanedUrl = URLDecoder.decode(cleanedUrl, StandardCharsets.UTF_8);
        String siteUrl = "";
        try {
            URL url = new URL(cleanedUrl);
            siteUrl = url.getProtocol() + "://" + url.getHost();
            Optional<Site> optional = siteRepository.findByUrlIgnoreCase(siteUrl);
            if (optional.isPresent()) {
                if (pageRepository.findByPath(url.getPath()) != null) {
                    deletePage(url.getPath());
                }
                Site site = optional.get();
                sleep(500);
                Document doc = HtmlParser.getConnection(cleanedUrl);
                Page page = new Page();
                page.setSite(site);
                page.setPath(url.getPath());
                page.setCode(doc.connection().response().statusCode());
                page.setContent(doc.html());
                pageRepository.save(page);
                if (page.getCode() < 400) {
                    log.info("Start indexing page " + cleanedUrl);
                    lemmaService.findAndSave(page);
                }

            } else {
                log.warn("Site not found: " + siteUrl);
                indexingResponse.setResult(false);
                indexingResponse.setError("Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле");
                return indexingResponse;
            }

        } catch (IOException | InterruptedException | CancellationException e) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Ошибка индексации");
            return indexingResponse;
        }
        indexingResponse.setResult(true);
        indexingResponse.setError("Indexing completed successfully");
        return indexingResponse;
    }

    private void deleteSites() {
        log.info("Delete all sites");
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }

    private void deletePage(String path) {
        Page page = pageRepository.findByPath(path);
        Set<Index> indices = indexRepository.findAllByPage(page);
        Set<Lemma> lemmas = new HashSet<>();
        for (Index index : indices) {
            Integer id = index.getLemma().getId();
            Lemma lemma = lemmaRepository.findById(id).orElseThrow(() -> new IllegalStateException("Lemma not found"));
            int currentFrequency = lemma.getFrequency();
            lemma.setFrequency(currentFrequency - 1);
            lemmas.add(lemma);
        }
        log.info("Delete indices for page " + path);
        indexRepository.deleteAll(indices);
        log.info("Delete lemmas for page " + path);
        lemmaRepository.saveAll(lemmas);
        log.info("Delete page " + path);
        pageRepository.deleteById(page.getId());

    }

    private boolean existsIndexingSite() {
        return siteRepository.existsByStatus(SiteStatus.INDEXING);
    }

    private boolean isFailed(Integer siteId) {
        return siteRepository.existsByIdAndStatus(siteId, SiteStatus.FAILED);
    }

}
