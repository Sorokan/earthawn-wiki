package repository.files;

public class User extends Node {

    public String passwordHash;
    public String email;
    public String imageUid;
    public String pageUid;
    public String ip;
    public boolean admin;
    public String signatureUid; // wird statt Name bei Author angezeigt

    @Override
    public void validate(ValidationException e) {
        super.validate(e);
        e.required(passwordHash, "passwordHash");
    }

}
