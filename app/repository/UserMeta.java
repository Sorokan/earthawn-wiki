package repository;

import org.springframework.beans.BeanUtils;

import repository.files.User;

public class UserMeta {

    public String name;
    public String passwordHash;
    public String email;
    public String imageUid;
    public String pageUid;
    public String ip;
    public boolean admin;
    public String signatureUid; // wird statt Name bei Author angezeigt

    public void fillInto(User user) {
        BeanUtils.copyProperties(this, user);
    }

}
