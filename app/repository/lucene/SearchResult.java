package repository.lucene;

import java.util.Date;

public class SearchResult {
    
    public static enum Type {
        page, attachement, comment
    }
    
    public Type type;
    public String uid;
    public String id;
    public String imageUid;
    public String userUid;
    public String name;
    public Date lastChanged;
    public String explanationHtml;
    public String quelle;

}