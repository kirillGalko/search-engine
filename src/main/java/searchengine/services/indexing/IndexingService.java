package searchengine.services.indexing;

import org.springframework.http.ResponseEntity;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;

public interface IndexingService {
    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse indexPage(String pageUrl);

}
