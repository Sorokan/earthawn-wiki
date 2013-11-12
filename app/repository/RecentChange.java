package repository;

import java.util.Date;

public class RecentChange {

    public static enum ContentType {
        attachement, page, comment
    }

    public Date changedAt;
    public ContentType type;
    public String ip;
    public String uid;
    public String destil;
    public String imageUid;

    public String userUid;
    public String userName;
    public String userImageUid;

}
