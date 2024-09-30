package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositoreis.LemmaRepository;
import searchengine.repositoreis.PageRepository;
import searchengine.repositoreis.SiteRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        log.info("Get statistics");
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : siteRepository.findAll()) {
            DetailedStatisticsItem detailedStatisticsItem = new DetailedStatisticsItem();
            detailedStatisticsItem.setUrl(site.getUrl());
            detailedStatisticsItem.setName(site.getName());
            detailedStatisticsItem.setStatus(site.getStatus().name());
            detailedStatisticsItem.setStatusTime(site.getStatusTime().toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
            detailedStatisticsItem.setError(site.getLastError());
            detailedStatisticsItem.setPages((int) pageRepository.countBySite(site));
            detailedStatisticsItem.setLemmas((int) lemmaRepository.countBySite(site));
            detailed.add(detailedStatisticsItem);
        }

        TotalStatistics total = new TotalStatistics();
        total.setSites((int) siteRepository.count());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(siteRepository.existsByStatus(SiteStatus.INDEXING));

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData(total, detailed);
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
