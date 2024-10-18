package searchengine.dto.search;

public class SearchData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

//    public SearchData(String site, String siteName, String uri, String title, String snippet, float relevance){
//        this.site = site;
//        this.siteName = siteName;
//        this.uri = uri;
//        this.title = title;
//        this.snippet = snippet;
//        this.relevance = relevance;
//    }

    public String getSite() {
        return site;
    }
    public String getSiteName(){
        return siteName;
    }
    public String getUri(){
        return uri;
    }
    public String getTitle(){
        return title;
    }
    public String getSnippet(){
        return snippet;
    }
    public float getRelevance(){
        return relevance;
    }

    public void setSite(String site){
        this.site = site;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
    public void setRelevance(float relevance) {
        this.relevance = relevance;
    }
}
