package searchengine.config;

import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EnglishLemmaConfiguration {
    @Bean
    public EnglishLuceneMorphology luceneMorphologyEn() throws IOException {
        return new EnglishLuceneMorphology();
    }
}
