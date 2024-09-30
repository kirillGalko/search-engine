package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import searchengine.services.lemma.LemmaParser;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SnippetGenerator {
    private static final int SYMBOLS_IN_SNIPPET = 100;
    private final LemmaParser lemmaParser;

    private final LuceneMorphology luceneMorphology;

    public String generateSnippet(String query, String content) {
        StringBuilder snippetBuilder = new StringBuilder();
        String cleanedContent = Jsoup.clean(content, Safelist.none());
        Set<String> queryLemmas = lemmaParser.getLemmaSet(query);
        String[] contentWords = lemmaParser.arrayContainsRussianWords(content);
        Set<String> wordsForSearch = new HashSet<>();
        for (String word : contentWords) {
            for (String lemma : queryLemmas) {
                if (luceneMorphology.getNormalForms(word).contains(lemma)) {
                    wordsForSearch.add(word);
                }
            }
        }

        for (String word : wordsForSearch) {
            snippetBuilder.append(paddingSnippet(cleanedContent, word));
        }
        return snippetBuilder.toString();
    }

    private String paddingSnippet(String content, String query) {
        int index = content.toLowerCase().indexOf(query.toLowerCase());
        StringBuilder snippet = new StringBuilder();
        int startIndex = Math.max(0, index - SYMBOLS_IN_SNIPPET);
        int endIndex = Math.min(content.length(), index + query.length() + SYMBOLS_IN_SNIPPET);
        String beginning = content.substring(startIndex, index);
        String boldQuery = "<b>" + query.toLowerCase() + "</b>";
        String ending = content.substring(index + query.length(), endIndex);
        snippet.append("...").append(beginning).append(boldQuery).append(ending).append("...");

        return snippet.toString().trim();
    }

}
