package repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import repository.files.Attachement;
import repository.files.FileRepoVisitor;
import repository.files.FileRepository;
import repository.files.FileRepository.AttachementSuffix;
import repository.files.LoginTokenPlain;
import repository.files.Page;
import repository.files.Page.Comment;
import repository.files.RepoMeta;
import repository.files.User;
import repository.files.ValidationException;
import repository.lucene.LuceneWrapper;
import repository.lucene.SearchResult;

// Nicht vergessen: Map alte Url auf neue Url!
// Nicht vergessen: Attachement-Binaries auf Binär-Gleichheit prüfen bei gleichem Namen bei Import 
// TODO Mimetype-Erkennung, Thumbnailing, Menüstruktur / Pagegraph
// TODO Unit Tests

public class RepositoryService {

    private static RepositoryService INSTANCE;
    
    public static RepositoryService get() {
        return INSTANCE;
    }
    
    public static void create(FileRepository repo) throws ValidationException {
        INSTANCE = new RepositoryService(repo);
    }
    
    private FileRepository repo;

    public Map<String, LoginTokenPlain> loginTokenCache = new ConcurrentHashMap<>();
    public Map<String, User> userCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private Set<LoginTokenPlain> recentlyUsedLoginTokens = Collections.newSetFromMap(new ConcurrentHashMap<LoginTokenPlain, Boolean>());
    private RepoMeta meta;

    // TODO
    private List<RecentChangeDay> recentChanges = new ArrayList<>();
    private LuceneWrapper luceneWrapper;    

    private RepositoryService(FileRepository repo) throws ValidationException {
        this.repo = repo;
        this.luceneWrapper = new LuceneWrapper();
        initialize();
        startCleanupThreads();
    }

    
    public List<SearchResult> search(String freeText, int maxNumberOfResults) {
        return luceneWrapper.search(freeText, maxNumberOfResults);
    }

    public int getNumberOfPages() {
        // TODO
        return -1;
    }

    public List<RecentChangeDay> getRecentChanges() {
        return recentChanges;
    }

    public List<User> getRecentlyActiveUsers() {
        class UserAtDate implements Comparable<UserAtDate> {
            User user;
            Date date;

            public UserAtDate(User user, Date date) {
                this.user = user;
                this.date = date;
            }

            @Override
            public int compareTo(UserAtDate o) {
                return date.compareTo(o.date);
            };
        }
        List<UserAtDate> userAtDates = new ArrayList<>();
        for (LoginTokenPlain loginToken : recentlyUsedLoginTokens) {
            User user = userCache.get(loginToken.userUid);
            userAtDates.add(new UserAtDate(user, loginToken.lastUsed));
        }
        Collections.sort(userAtDates);
        List<User> users = new ArrayList<>();
        for (UserAtDate userAtDate : userAtDates) {
            users.add(userAtDate.user);
        }
        return users;
    }

    public void initialize() throws ValidationException {
        repo.ensureBaseStructure();
        this.meta = repo.readRepoMeta();
        initializeLoginTokenCache();
        initializeUserCache();
        initializeRecentChanges();
    }

    public RepoMeta getMeta() {
        return meta;
    }

    public RepoSession login(final String name, String passwordHash, String ip) {
        User user = userCache.get(name);
        if (user == null) {
            return null;
        }
        if (!ObjectUtils.equals(passwordHash, user.passwordHash)) {
            return null;
        }
        invalidateAllTokensForUser(user);
        LoginTokenPlain loginTokenPlain = repo.loginTokenRepo.create(user.uid);
        loginTokenCache.put(loginTokenPlain.token, loginTokenPlain);
        return new RepoSession(loginTokenPlain.token, ip);
    }

    private void invalidateAllTokensForUser(User user) {
        List<String> oldTokens = new ArrayList<>();
        for (LoginTokenPlain loginToken : loginTokenCache.values()) {
            if (ObjectUtils.equals(loginToken.userUid, user.uid)) {
                oldTokens.add(loginToken.token);
            }
        }
        removeLoginTokens(oldTokens);
    }

    private void removeLoginTokens(List<String> oldTokens) {
        for (String token : oldTokens) {
            LoginTokenPlain loginTokenPlain = loginTokenCache.remove(token);
            recentlyUsedLoginTokens.remove(loginTokenPlain);
            repo.loginTokenRepo.delete(token);
        }
    }

    public User getUser(RepoSession session) throws LoginTokenUnknown {
        updateLastAccess(session);
        LoginTokenPlain loginTokenPlain = getLoginTokenPlain(session);
        return userCache.get(loginTokenPlain.userUid);
    }

    public void createUser(UserMeta meta) throws ValidationException {
        User user = new User();
        meta.fillInto(user);
        repo.users.createAndReturnUid(user);
        userCache.put(user.uid, user);
    }

    public void deleteUser(RepoSession session, String uid) throws LoginTokenUnknown, Forbidden {
        updateLastAccess(session);
        checkAdmin(session);
        repo.users.delete(uid);
        userCache.remove(uid);
    }

    public void updateUser(RepoSession session, String uid, UserMeta meta) throws LoginTokenUnknown, Forbidden, ValidationException {
        updateLastAccess(session);
        checkOwnOrAdmin(session, uid);
        User user = userCache.get(uid);
        meta.fillInto(user);
        repo.users.update(user);
        userCache.put(user.uid, user);
    }

    public PageContent getCurrentPageContent(RepoSession session, String uid) {
        updateLastAccess(session);
        Page page = repo.pages.read(uid);
        String html = repo.pages.version.read(uid, page.currentVersionId);
        return new PageContent(page, html);
    }

    public PageContent getPageContent(RepoSession session, String uid, String id) {
        updateLastAccess(session);
        Page page = repo.pages.read(uid);
        String html = repo.pages.version.read(uid, id);
        return new PageContent(page, html);
    }

    public Page createPage(RepoSession session, PageMeta pageMeta) throws ValidationException {
        updateLastAccess(session);
        Page page = new Page();
        pageMeta.fillInto(page);
        repo.pages.createAndReturnUid(page);
        return page;
    }

    public void updatePage(RepoSession session, String uid, PageMeta pageMeta) throws ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Page page = repo.pages.read(uid);
            pageMeta.fillInto(page);
            repo.pages.update(page);
        } finally {
            release(uid);
        }
    }

    public void deletePage(RepoSession session, String uid) throws LoginTokenUnknown, Forbidden, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            checkAdmin(session);
            repo.pages.delete(uid);
        } finally {
            release(uid);
            lockMap.remove(uid);
        }
    }

    public void addPageComment(RepoSession session, String uid, String html) throws ValidationException, LoginTokenUnknown, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Page page = repo.pages.read(uid);
            Comment comment = new Comment();
            comment.html = html;
            comment.id = repo.pages.newPartId();
            comment.ip = session.ip;
            comment.timestamp = new Date();
            comment.userUid = getUserUid(session);
            page.comments.add(comment);
            repo.pages.update(page);
        } finally {
            release(uid);
        }
    }

    public void updatePageComment(RepoSession session, String uid, String id, String html) throws LoginTokenUnknown, ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Page page = repo.pages.read(uid);
            for (Comment comment : page.comments) {
                if (ObjectUtils.equals(comment.id, id)) {
                    comment.html = html;
                    comment.ip = session.ip;
                    comment.timestamp = new Date();
                    comment.userUid = getUserUid(session);
                    break;
                }
            }
            repo.pages.update(page);
        } finally {
            release(uid);
        }
    }

    public void deletePageComment(RepoSession session, String uid, String id) throws LoginTokenUnknown, Forbidden, ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            checkAdmin(session);
            Page page = repo.pages.read(uid);
            Comment comment = new Comment();
            comment.id = id;
            page.comments.remove(comment);
            repo.pages.update(page);
        } finally {
            release(uid);
        }
    }

    // Für Ajax-Check der Modification der Page von jemand anderem
    // zwischendurch, als long standing HTTP connection
    // Dieser Check, sobald man anfängt, eine Seite zu bearbeiten
    // Dann Anzeige allen, die Bearbeiten, wer noch bearbeitet
    // Auto-Merge
    public boolean checkPageVersionSame(RepoSession session, String uid, String currentVersionId) {
        Page page = repo.pages.read(uid);
        return ObjectUtils.equals(page.currentVersionId, currentVersionId);
    }

    public void addPageVersion(RepoSession session, String uid, String html) throws NodeWasModifiedInbetween, LoginTokenUnknown, ValidationException,
            InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Page page = repo.pages.read(uid);
            String oldHtml = repo.pages.version.read(uid, page.currentVersionId);
            if (html.equals(oldHtml)) {
                // simply do nothing
                return;
            }
            Page.Version version = new Page.Version();
            version.id = repo.pages.newPartId();
            version.ip = session.ip;
            version.timestamp = new Date();
            version.userUid = getUserUid(session);
            page.versions.add(version);
            page.currentVersionId = version.id;
            repo.pages.version.createOrUpdate(uid, version.id, html);
            repo.pages.update(page);
        } finally {
            release(uid);
        }
    }

    public void deletePageVersion(RepoSession session, String uid, String id) throws LoginTokenUnknown, Forbidden, NodeWasModifiedInbetween,
            ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            checkAdmin(session);
            Page page = repo.pages.read(uid);
            Page.Version last = page.versions.isEmpty() ? null : page.versions.get(page.versions.size() - 1);
            for (Page.Version version : page.versions) {
                if (ObjectUtils.equals(version.id, id)) {
                    page.currentVersionId = last == null ? null : last.id;
                    repo.pages.update(page);
                    repo.pages.version.delete(uid, id);
                    return;
                }
                last = version;
            }
            throw new NodeWasModifiedInbetween();
        } finally {
            release(uid);
        }
    }

    public void restorePageVersion(RepoSession session, String uid, String id) throws ValidationException, NodeWasModifiedInbetween, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Page page = repo.pages.read(uid);
            for (Page.Version version : page.versions) {
                if (ObjectUtils.equals(version.id, id)) {
                    page.currentVersionId = id;
                    repo.pages.update(page);
                    return;
                }
            }
            throw new NodeWasModifiedInbetween();
        } finally {
            release(uid);
        }
    }

    public void addPageAttachement(RepoSession session, String pageUid, String attachementUid) throws ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(pageUid);
        try {
            Page page = repo.pages.read(pageUid);
            if (page.attachements.contains(attachementUid)) {
                return;
            }
            page.attachements.add(attachementUid);
            repo.pages.update(page);
        } finally {
            release(pageUid);
        }
    }

    public void removePageAttachement(RepoSession session, String pageUid, String attachementUid) throws ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(pageUid);
        try {
            Page page = repo.pages.read(pageUid);
            page.attachements.remove(attachementUid);
            repo.pages.update(page);
        } finally {
            release(pageUid);
        }
    }

    public AttachementContent getAttachementContent(RepoSession session, String uid, AttachementSuffix suffix) {
        updateLastAccess(session);
        Attachement attachement = repo.attachements.read(uid);
        byte[] content = repo.attachements.version.read(uid, attachement.currentVersionId, suffix);
        return new AttachementContent(attachement, content);
    }

    public Attachement createAttachement(RepoSession session, AttachementMeta meta) throws ValidationException {
        updateLastAccess(session);
        Attachement attachement = new Attachement();
        meta.fillInto(attachement);
        repo.attachements.createAndReturnUid(attachement);
        return attachement;
    }

    public void updateAttachement(RepoSession session, String uid, AttachementMeta meta) throws ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Attachement attachement = repo.attachements.read(uid);
            meta.fillInto(attachement);
            repo.attachements.update(attachement);
        } finally {
            release(uid);
        }
    }

    public void deleteAttachement(RepoSession session, String uid) throws LoginTokenUnknown, Forbidden, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            checkAdmin(session);
            repo.attachements.delete(uid);
        } finally {
            release(uid);
            lockMap.remove(uid);
        }
    }

    public void addAttachementVersion(RepoSession session, String uid, String mimeType, byte[] content, byte[] thumbnail) throws LoginTokenUnknown,
            ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Attachement attachement = repo.attachements.read(uid);
            Attachement.Version version = new Attachement.Version();
            version.id = repo.pages.newPartId();
            version.ip = session.ip;
            version.timestamp = new Date();
            version.userUid = getUserUid(session);
            version.mimetype = mimeType;
            attachement.versions.add(version);
            attachement.currentVersionId = version.id;
            repo.attachements.version.create(uid, version.id, AttachementSuffix.data, content);
            repo.attachements.version.create(uid, version.id, AttachementSuffix.thumbnail, thumbnail);
            repo.attachements.update(attachement);
        } finally {
            release(uid);
        }
    }

    public void deleteAttachementVersion(RepoSession session, String uid, String id) throws LoginTokenUnknown, Forbidden, NodeWasModifiedInbetween,
            ValidationException, InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            checkAdmin(session);
            Attachement attachement = repo.attachements.read(uid);
            Attachement.Version last = attachement.versions.isEmpty() ? null : attachement.versions.get(attachement.versions.size() - 1);
            for (Attachement.Version version : attachement.versions) {
                if (ObjectUtils.equals(version.id, id)) {
                    attachement.currentVersionId = last == null ? null : last.id;
                    repo.attachements.update(attachement);
                    repo.attachements.version.delete(uid, id);
                    return;
                }
                last = version;
            }
            throw new NodeWasModifiedInbetween();
        } finally {
            release(uid);
        }
    }

    public void restoreAttachementVersion(RepoSession session, String uid, String id) throws ValidationException, NodeWasModifiedInbetween,
            InterruptedException {
        updateLastAccess(session);
        lockOrWait(uid);
        try {
            Attachement attachement = repo.attachements.read(uid);
            for (Attachement.Version version : attachement.versions) {
                if (ObjectUtils.equals(version.id, id)) {
                    attachement.currentVersionId = id;
                    repo.attachements.update(attachement);
                    return;
                }
            }
            throw new NodeWasModifiedInbetween();
        } finally {
            release(uid);
        }
    }

    private LoginTokenPlain getLoginTokenPlain(RepoSession session) throws LoginTokenUnknown {
        if (session == null) {
            throw new LoginTokenUnknown();
        }
        if (StringUtils.isEmpty(session.loginToken)) {
            throw new LoginTokenUnknown();
        }
        LoginTokenPlain loginTokenPlain = loginTokenCache.get(session.loginToken);
        if (loginTokenPlain == null) {
            throw new LoginTokenUnknown();
        }
        return loginTokenPlain;
    }

    private void checkOwnOrAdmin(RepoSession session, String userUid) throws LoginTokenUnknown, Forbidden {
        LoginTokenPlain loginTokenPlain = getLoginTokenPlain(session);
        if (!ObjectUtils.equals(loginTokenPlain.userUid, userUid)) {
            User admin = userCache.get(loginTokenPlain.userUid);
            if (admin == null) {
                throw new Forbidden();
            }
            if (!admin.admin) {
                throw new Forbidden();
            }
        }
    }

    private void checkAdmin(RepoSession session) throws LoginTokenUnknown, Forbidden {
        if (!getUser(session).admin) {
            throw new Forbidden();
        }
    }

    private String getUserUid(RepoSession session) throws LoginTokenUnknown {
        User user = getUser(session);
        if (user == null) {
            return null;
        }
        return user.uid;
    }

    // TODO async instead of locking?
    private void lockOrWait(String uid) throws InterruptedException {
        lockMap.putIfAbsent(uid, new ReentrantLock());
        ReentrantLock lock = lockMap.get(uid);
        lock.tryLock(5, TimeUnit.SECONDS);
    }

    private void release(String uid) {
        lockMap.get(uid).unlock();
    }

    private void updateLastAccess(RepoSession session) {
        LoginTokenPlain loginToken = loginTokenCache.get(session.loginToken);
        if (loginToken != null) {
            loginToken.lastUsed = new Date();
            recentlyUsedLoginTokens.add(loginToken);
        }
    }

    private void startCleanupThreads() {
        Timer timer = new Timer("RepositoryServiceCleanUpThread", true);
        {
            // delete login tokens permanently that are too old
            // one month
            final long timeOutIntervall = 30 * 24 * 60 * 60 * 1000;
            // once a day
            long periodMs = 24 * 60 * 60 * 1000;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long t = System.currentTimeMillis();
                    List<String> loginTokensToDelete = new ArrayList<>();
                    for (LoginTokenPlain loginToken : loginTokenCache.values()) {
                        if (t - loginToken.lastUsed.getTime() > timeOutIntervall) {
                            loginTokensToDelete.add(loginToken.token);
                        }
                    }
                    removeLoginTokens(loginTokensToDelete);
                }
            }, periodMs, periodMs);
        }
        {
            // keep recentlyUsedLoginTokens fresh
            // 5 minutes
            final long timeOutIntervall = 5 * 60 * 1000;
            // each minute
            long periodMs = 60 * 1000;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long t = System.currentTimeMillis();
                    for (Iterator<LoginTokenPlain> i = recentlyUsedLoginTokens.iterator(); i.hasNext();) {
                        LoginTokenPlain loginToken = i.next();
                        if (t - loginToken.lastUsed.getTime() > timeOutIntervall) {
                            i.remove();
                        }
                    }
                }
            }, periodMs, periodMs);
        }
    }

    private void initializeLoginTokenCache() {
        loginTokenCache.clear();
        repo.loginTokenRepo.visit(new FileRepoVisitor<LoginTokenPlain>() {
            @Override
            public boolean visit(LoginTokenPlain loginToken) {
                loginTokenCache.put(loginToken.token, loginToken);
                return true;
            }
        });
    }

    private void initializeUserCache() {
        userCache.clear();
        repo.users.visit(new FileRepoVisitor<User>() {
            @Override
            public boolean visit(User user) {
                userCache.put(user.name, user);
                return true;
            }
        });
    }

    private void initializeRecentChanges() {
        repo.pages.visit(new FileRepoVisitor<Page>() {
            @Override
            public boolean visit(Page page) {
                for (Page.Version version : page.versions) {
                }
                return true;
            }
        });
        repo.attachements.visit(new FileRepoVisitor<Attachement>() {
            @Override
            public boolean visit(Attachement attachement) {
                for (Attachement.Version version : attachement.versions) {
                }
                return true;
            }
        });
    }
}
