package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import searchengine.services.lemma.EnglishLemmaParser;
import searchengine.services.lemma.LemmaParser;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SnippetGenerator {
    private static final int SYMBOLS_IN_SNIPPET = 100;
    private static final String regex = "[^A-Za-zА-Яа-я0-9]";
    private final LemmaParser lemmaParser;
    private final EnglishLemmaParser englishLemmaParser;

    private final RussianLuceneMorphology luceneMorphologyRu;
    private final EnglishLuceneMorphology luceneMorphologyEn;

    public String generateSnippet(String query, String content) {
        StringBuilder snippetBuilder = new StringBuilder();
        String cleanedContent = Jsoup.clean(content, Safelist.none());
        Set<String> queryLemmas = lemmaParser.getLemmaSet(query);
        queryLemmas.addAll(englishLemmaParser.getLemmaSet(query));
        String[] contentWords = lemmaParser.arrayContainsRussianWords(content);
        String[] englishContentWords = englishLemmaParser.arrayContainsEnglishWords(content);

        Set<String> wordsForSearch = new HashSet<>();
        for (String word : contentWords) {
            for (String lemma : queryLemmas) {
                if (luceneMorphologyRu.getNormalForms(word).contains(lemma)) {
                    wordsForSearch.add(word);
                }
            }
        }

        for (String word : englishContentWords) {
            for (String lemma : queryLemmas) {
                if (luceneMorphologyEn.getNormalForms(word).contains(lemma)) {
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
        if (index < 1){
            return "";
        }
        while (!content.substring(index - 1, index).matches(regex) ||
                !content.substring(index + query.length(), index + query.length() + 1).matches(regex)){
            index = content.toLowerCase().indexOf(query.toLowerCase(), index + 1);
        }
        StringBuilder snippet = new StringBuilder();
        int startIndex = Math.max(0, index - SYMBOLS_IN_SNIPPET);
        int endIndex = Math.min(content.length(), index + query.length() + SYMBOLS_IN_SNIPPET);
        String beginning = content.substring(startIndex, index);
        String boldQuery = "<b>" + content.substring(index, index + query.length()) + "</b>";
        String ending = content.substring(index + query.length(), endIndex);
        snippet.append("...").append(beginning).append(boldQuery).append(ending).append("...");

        return snippet.toString().trim();
    }

}
