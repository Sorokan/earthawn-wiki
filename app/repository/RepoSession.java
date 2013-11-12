package repository;

public class RepoSession {

    public final String loginToken;
    public final String ip;

    public RepoSession(String loginToken, String ip) {
        this.loginToken = loginToken;
        this.ip = ip;
    }

}