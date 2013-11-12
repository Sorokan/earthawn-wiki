package repository.files;

import java.util.ArrayList;
import java.util.List;

public class Page extends Node {

    public String destil;
    public Boolean ausgeglichen;
    public String quelle;
    public boolean excellent;
    public String imageUid;
    public String currentVersionId;
    public boolean noRobots;
    public Herkunft herkunft;
    public List<Version> versions = new ArrayList<Version>();
    public List<Comment> comments = new ArrayList<Comment>();
    public List<String> attachements = new ArrayList<String>();
    public List<String> referencedPageUids = new ArrayList<String>();

    public Version currentVersion() {
        if (currentVersionId==null) {
            return null;
        }
        for (Version version : versions) {
            if (currentVersionId.equals(version.id)) {
                return version;
            }
        }
        return null;
    }
    
    @Override
    public void validate(ValidationException e) {
        super.validate(e);
        for (Version version : versions) {
            version.validate(e);
        }
        for (Comment comment : comments) {
            comment.validate(e);
        }
        for (String attachement : attachements) {
            e.required(attachement, "attachement");
        }
    }

    public static class Version extends NodePart {
    }

    public static class Comment extends NodePart {
        public String html;
    }

}
