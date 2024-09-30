package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.NotFoundException;
import searchengine.model.*;
import searchengine.repositoreis.IndexRepository;
import searchengine.repositoreis.LemmaRepository;
import searchengine.repositoreis.SiteRepository;
import searchengine.services.indexing.HtmlParser;
import searchengine.services.lemma.LemmaParser;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final String URL_REGEX = "^https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\b$";
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final SnippetGenerator snippetGenerator;
    private final LemmaParser lemmaParser;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        log.info("Query: {}", query);
        long startTime = System.currentTimeMillis();
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Задан пустой поисковый запрос");
        }
        List<Site> sites = getSites(site);
        Set<String> queryLemmas = lemmaParser.getLemmaSet(query.trim());

        Map<Page, Double> pageRank = new HashMap<>();

        for (Site persistSite : sites) {
            List<Lemma> sortedLemmas = new ArrayList<>();
            for (String lemma : queryLemmas) {
                List<Lemma> foundLemmas = lemmaRepository.findBySiteAndLemma(persistSite, lemma);
                foundLemmas.sort(Comparator.comparing(Lemma::getFrequency));
                sortedLemmas.addAll(foundLemmas);
            }
            Set<Lemma> lemmaSet = new HashSet<>(sortedLemmas);

            Set<Page> pages = getPages(sortedLemmas);

            pageRank.putAll(pages.stream().collect(Collectors.toMap(Function.identity(),
                    page -> sumRank(page, lemmaSet))));
        }

        Optional<Double> optionalMaxRank = pageRank.values().stream().max(Double::compareTo);
        List<SearchData> searchData;
        if (optionalMaxRank.isEmpty()) {
            searchData = List.of();
        } else {
            double maxRank = optionalMaxRank.get();
            searchData = pageRank.entrySet().parallelStream()
                    .map(entry -> {
                        Page page = entry.getKey();
                        Double rank = entry.getValue();
                        String content = page.getContent();
                        return new SearchData(page.getSite().getUrl(),
                                page.getSite().getName(),
                                page.getPath(),
                                HtmlParser.getTitle(content),
                                snippetGenerator.generateSnippet(query, content),
                                (float) (rank / maxRank));
                    })
                    .sorted((a, b) -> Float.compare(b.getRelevance(), a.getRelevance()))
                    .toList();
        }
        log.info("Search time: {} ms.", System.currentTimeMillis() - startTime);
        return new SearchResponse(true, searchData.size(), subList(searchData, offset, limit));
    }

    private List<SearchData> subList(List<SearchData> searchData, Integer offset, Integer limit) {
        int fromIndex = offset;
        int toIndex = fromIndex + limit;

        if (toIndex > searchData.size()) {
            toIndex = searchData.size();
        }
        if (fromIndex > toIndex) {
            return List.of();
        }

        return searchData.subList(fromIndex, toIndex);
    }

    private double sumRank(Page page, Set<Lemma> lemmas) {
        return indexRepository.findAllByPageAndLemmaIn(page, lemmas).stream().mapToDouble(Index::getRank).sum();
    }

    private Set<Page> getPages(List<Lemma> sortedLemmas) {
        if (sortedLemmas.isEmpty()) return Set.of();
        Set<Page> pages = new HashSet<>();
        for (Lemma lemma : sortedLemmas) {
            List<Index> indices = indexRepository.findAllByLemmaId(lemma.getId());

            pages.addAll(indices.stream()
                    .map(Index::getPage)
                    .collect(Collectors.toSet()));
            if (pages.isEmpty()) {
                return pages;
            }
        }
        return pages;
    }

    private List<Site> getSites(String siteUrl) {
        List<Site> sites;
        if (siteUrl == null || siteUrl.isBlank()) {
            sites = siteRepository.findAll();
        } else {
            Site site = getSite(siteUrl);
            sites = List.of(site);
        }
        checkIndexed(sites);
        return sites;
    }

    private Site getSite(String siteUrl) {
        String trimSiteUrl = siteUrl.trim();
        return siteRepository.findByUrlIgnoreCase(trimSiteUrl)
                .orElseThrow(() -> new NotFoundException("Сайт не найден"));

    }

    private List<Site> checkIndexed(List<Site> sites) {
        for (Site site : sites) {
            if (!site.getStatus().equals(SiteStatus.INDEXED)) {
                log.info("Сайт {} не проиндексирован", site.getUrl());
                sites.remove(site);
            }
        }
        return sites;
    }

}
