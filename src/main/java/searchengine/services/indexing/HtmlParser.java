package searchengine.services.indexing;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.JsoupConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositoreis.PageRepository;
import searchengine.repositoreis.SiteRepository;
import searchengine.services.lemma.LemmaService;

import javax.persistence.OptimisticLockException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import static java.lang.Thread.sleep;

@RequiredArgsConstructor
@Slf4j
public class HtmlParser extends RecursiveAction {
    private final String url;
    private final Site site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaService lemmaService;

    private static CopyOnWriteArrayList<String> allLinks = new CopyOnWriteArrayList<>();

    @Override
    protected void compute() {
        try {
            if (isNotFailed(site.getId()) && isNotVisited(site, url)) {

                List<HtmlParser> taskList = new ArrayList<>();

                sleep(500);
                Document doc = getConnection(url);
                synchronized (Page.class) {
                    if (isNotVisited(site, url)) {
                    String path = url.equals(site.getUrl()) ? "/" : url.substring(site.getUrl().length());
                    Page page = new Page();
                    page.setSite(site);
                    page.setPath(path);
                    page.setCode(doc.connection().response().statusCode());
                    page.setContent(doc.html());
                    pageRepository.save(page);
                    if (page.getCode() < 400) {
                        lemmaService.findAndSave(page);
                    }

                    }
                }
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String absUrl = link.attr("abs:href");
                    if (isCorrect(absUrl)) {
                        allLinks.add(absUrl);
                        HtmlParser task = new HtmlParser(absUrl, site, pageRepository, siteRepository, lemmaService);
                        task.fork();
                        taskList.add(task);
                    }
                }
                for (HtmlParser task : taskList) {
                    task.join();
                }
            }
        } catch (IOException ex) {
            log.warn("Input-output exception");
            failed(site, "Ошибка парсинга: ошибка ввода-вывода");
        } catch (InterruptedException ex) {
            log.warn("Interrupted exception");
            failed(site, "Ошибка парсинга: процесс прерван");
        } catch (CancellationException ex) {
            log.warn("Cancellation exception");
            failed(site, "Ошибка парсинга: процесс отменен");
        } catch (Exception ex) {
            log.error("Parser exception");
            failed(site, "Ошибка парсинга сайта " + site.getUrl());
        }

    }


    private boolean isCorrect(String url) {
        return (!url.isEmpty() && url.startsWith(site.getUrl())
                && !url.contains("#") && !url.matches("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf|jpeg|mp4|sql|zip))$)"));
    }

    private boolean isNotFailed(Integer siteId) {
        return !siteRepository.existsByIdAndStatus(siteId, SiteStatus.FAILED);
    }

    private boolean isNotVisited(Site site, String url) throws MalformedURLException {
        String path = url.substring(site.getUrl().length());
        return !pageRepository.existsBySiteIdAndPath(site.getId(), path);
    }

    public static Document getConnection(String url) throws IOException, InterruptedException {
        return Jsoup.connect(url)
                .timeout(100000)
                .userAgent("MephistoSearchBot")
                .referrer("http://www.google.com")
                .ignoreHttpErrors(true)
                .get();
    }

    private void failed(Site site, String error) {
        log.warn("Failed indexing site with {}: {}", site.getUrl(), error);
        site.setLastError(error);
        site.setStatus(SiteStatus.FAILED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    public static String getTitle(String content) {
        Document document = Jsoup.parse(content);
        return document.title();
    }


}
