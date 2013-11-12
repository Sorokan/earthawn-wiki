package controllers;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.mvc.Controller;
import play.mvc.Result;
import repository.LoginTokenUnknown;
import repository.NodeWasModifiedInbetween;
import repository.PageContent;
import repository.RepoSession;
import repository.RepositoryService;
import repository.files.LoginTokenPlain;
import repository.files.User;
import repository.files.ValidationException;
//import util.pdf.PDF;
import views.html.index;
import views.html.page.*;

public class Application extends Controller {

    public static Result index() {
        RepoSession session = new RepoSession("", null);
        PageContent mainPage = repo().getCurrentPageContent(session, repo().getMeta().mainPageUid);
        PageContent menuPage = repo().getCurrentPageContent(session, repo().getMeta().menuPageUid);
        return ok(index.render(menuPage,mainPage));
    }

    
    public static Result pdf() {
/*        RepoSession session = new RepoSession("", null);
        PageContent mainPage = repo().getCurrentPageContent(session, repo().getMeta().mainPageUid);
        return PDF.ok(pageAll.render(mainPage));
*/
    	return ok("not currently implemented");
    }
    
    public static Result save() throws NodeWasModifiedInbetween, LoginTokenUnknown, ValidationException, InterruptedException {
        RepoSession session = fakeSession();
        Map<String,String[]> parameters = request().body().asFormUrlEncoded();
        String content = parameters.get("content")[0];
        String menu = parameters.get("menu")[0];
        
        repo().addPageVersion(session, repo().getMeta().menuPageUid, menu);
        repo().addPageVersion(session, repo().getMeta().mainPageUid, content);
        
        return ok();
    }


    public static Result createFirst() throws NodeWasModifiedInbetween, LoginTokenUnknown, ValidationException, InterruptedException {
        RepoSession session = fakeSession();
        createPage(session, repo().getMeta().menuPageUid);
        createPage(session, repo().getMeta().mainPageUid);
        return ok();
    }

    private static RepoSession fakeSession() {
        RepoSession session = new RepoSession("x", "127.0.0.1");
        User u = new User();
        u.uid = "x";
        repo().userCache.put("x", u);
        LoginTokenPlain plain = new LoginTokenPlain();
        plain.userUid="x";
        
        repo().loginTokenCache.put("x", plain);
        return session;
    }    

    private static void createPage(RepoSession session, String uid) throws NodeWasModifiedInbetween, LoginTokenUnknown, ValidationException, InterruptedException {
        PageContent page = repo().getCurrentPageContent(session, uid);
        if (StringUtils.isEmpty(page.html)) {
            repo().addPageVersion(session, page.page.uid, "Schreib was...");
        }
    }

    public static RepositoryService repo() {
        return RepositoryService.get();
    }

}
