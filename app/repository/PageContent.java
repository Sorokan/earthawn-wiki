package repository;

import repository.files.Page;

public class PageContent {
    public final Page page;
    public final String html;

    public PageContent(Page page, String html) {
        this.page = page;
        this.html = html;
    }
}