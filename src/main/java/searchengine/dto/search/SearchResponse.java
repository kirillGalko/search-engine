package searchengine.dto.search;

import java.util.List;

public class SearchResponse {

    private boolean result;
    private long count;
    private List<SearchData> data;

    public SearchResponse(boolean result, long count, List<SearchData> data){
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public boolean getResult(){
        return result;
    }
    public long getCount(){
        return count;
    }
    public List<SearchData> getData(){
        return data;
    }

    public void setResult(boolean result){
        this.result = result;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void setData(List<SearchData> data) {
        this.data = data;
    }
}
