package repository;

import org.springframework.beans.BeanUtils;

import repository.files.Herkunft;
import repository.files.Page;

public class PageMeta {
    
    public String name;
    public String destil;
    public Boolean ausgeglichen;
    public String quelle;
    public boolean excellent;
    public String imageUid;
    public boolean noRobots;
    public Herkunft herkunft;

    public void fillInto(Page page) {
        BeanUtils.copyProperties(this, page);
    }
}