package repository.files;

import java.util.ArrayList;
import java.util.List;

public class Attachement extends Node {

    public String description;
    public String quelle;
    public String currentVersionId;
    public boolean noRobots;
    public List<Version> versions = new ArrayList<Version>();

    @Override
    public void validate(ValidationException e) {
        super.validate(e);
        for (Version version : versions) {
            version.validate(e);
        }
    }

    public static class Version extends NodePart {
        public String mimetype;

        @Override
        public void validate(ValidationException e) {
            super.validate(e);
            e.required(mimetype, "mimetype");
        }

    }

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

}
