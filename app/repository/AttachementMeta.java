package repository;

import org.springframework.beans.BeanUtils;

import repository.files.Attachement;

public class AttachementMeta {
    public String name;
    public String description;
    public String quelle;
    public boolean noRobots;

    public void fillInto(Attachement attachement) {
        BeanUtils.copyProperties(this, attachement);
    }
}