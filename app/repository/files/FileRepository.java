package repository.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;

public class FileRepository {

    private SecureRandom rnd = new SecureRandom();

    public File rootPath;

    public FileRepository(File rootPath) {
        this.rootPath = rootPath;
    }

    private String createId() {
        SimpleDateFormat df = new SimpleDateFormat("yyyMMdd");
        return df.format(new Date()) + "_" + BigInteger.valueOf(rnd.nextLong()).toString(36);
    }

    private String createUid(String initialName) {
        String normalized = initialName == null ? "" : initialName.replaceAll("[^A-Za-zÄÖÜäöüß0-9]", "");
        normalized = normalized.substring(0, Math.min(12, normalized.length()));
        return normalized + "_" + createId();
    }

    private File getRepoMetaFile() {
        return new File(rootPath,"meta.json");
    }
    
    public void ensureBaseStructure() throws ValidationException {
        loginTokenRepo.ensureBasePath();
        users.ensureBasePath();
        pages.ensureBasePath();
        attachements.ensureBasePath();
        getRepoMetaFile().getParentFile().mkdirs();
        if (!getRepoMetaFile().exists()) {
            RepoMeta meta = new RepoMeta();
            meta.mainPageUid = createEmptySpecialPage("Main");
            meta.menuPageUid = createEmptySpecialPage("Menu");
            writeRepoMeta(meta);
        }
    }

    private String createEmptySpecialPage(String name) throws ValidationException {
        Page empty = new Page();
        empty.name = name;
        return pages.createAndReturnUid(empty);
    }

    public RepoMeta readRepoMeta() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(getRepoMetaFile(), RepoMeta.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeRepoMeta(final RepoMeta meta) {
        new AtomicWriter(getRepoMetaFile()) {
            @Override
            protected void writeUnsafe(File larve) throws Exception {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(larve, meta);
            }
        };
    }
    
    public class LoginTokenRepo {

        private String createSecureRandomToken() {
            return Long.toString(rnd.nextLong(), 36);
        }

        private File getBasePath() {
            return new File(rootPath, "loginTokens");
        }

        private File getFile(String token) {
            return new File(getBasePath(), token + ".json");
        }
        
        public void ensureBasePath() {
            getBasePath().mkdirs();
        }

        public LoginTokenPlain create(String userUid) {
            LoginTokenPlain loginToken = new LoginTokenPlain();
            loginToken.created = new Date();
            loginToken.lastUsed = loginToken.created;
            loginToken.userUid = userUid;
            loginToken.token = createSecureRandomToken();
            File file = getFile(loginToken.token);
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.writeValue(file, loginToken);
            } catch (Exception e) {
                throw Utils.soft(e);
            }
            return loginToken;
        }

        public LoginTokenPlain read(String token) {
            File file = getFile(token);
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(file, LoginTokenPlain.class);
            } catch (Exception e) {
                throw Utils.soft(e);
            }
        }

        public void delete(String token) {
            File file = getFile(token);
            file.delete();
        }

        public void visit(FileRepoVisitor<LoginTokenPlain> visitor) {
            String[] fileNames = getBasePath().list();
            for (String fileName : fileNames) {
                LoginTokenPlain node;
                try {
                    node = read(fileName);
                } catch (Exception e) {
                    Logger.warn("error visiting " + getBasePath() + "/" + fileName + " but proceed", e);
                    continue;
                }
                if (!visitor.visit(node)) {
                    break;
                }
            }
        }

    }

    public LoginTokenRepo loginTokenRepo = new LoginTokenRepo();

    public Repo<User> users = new Repo<User>() {

        @Override
        public File getBasePath() {
            return new File(rootPath, "users");
        }

        @Override
        public Class<User> getType() {
            return User.class;
        }

    };

    public PagesRepo pages = new PagesRepo();

    public class PagesRepo extends Repo<Page> {

        public String newPartId() {
            return createId();
        }

        @Override
        public File getBasePath() {
            return new File(rootPath, "pages");
        }

        @Override
        public Class<Page> getType() {
            return Page.class;
        }

        public PartRepo version = new PartRepo();

        public class PartRepo {

            public File getPartBasePath(String uid) {
                return new File(getDir(uid), "version");
            }

            protected File getFile(String uid, String id) {
                return new File(getPartBasePath(uid), id + ".html");
            }

            public void createOrUpdate(String uid, String id, String html) {
                try {
                    FileUtils.writeStringToFile(getFile(uid, id), html, "UTF8");
                } catch (IOException e) {
                    throw Utils.soft(e);
                }
            }

            public String read(String uid, String id) {
                try {
                    return FileUtils.readFileToString(getFile(uid, id), "UTF8");
                } catch (FileNotFoundException e) {
                    return "";
                } catch (IOException e) {
                    throw Utils.soft(e);
                }
            }

            public void delete(String uid, final String id) {
                getFile(uid, id).delete();
            }
        }

    };

    public AttachementsRepo attachements = new AttachementsRepo();

    public static enum AttachementSuffix {
        thumbnail, data
    }

    public class AttachementsRepo extends Repo<Attachement> {

        public String newPartId() {
            return createId();
        }

        @Override
        public File getBasePath() {
            return new File(rootPath, "attachements");
        }

        @Override
        public Class<Attachement> getType() {
            return Attachement.class;
        }

        public PartRepo version = new PartRepo();

        public class PartRepo {

            public File getPartBasePath(String uid) {
                return new File(getDir(uid), "version");
            }

            protected File getFile(String uid, String id, AttachementSuffix suffix) {
                return new File(getPartBasePath(uid), id + "." + suffix);
            }

            public void create(String uid, String id, AttachementSuffix suffix, byte[] bytes) {
                try {
                    FileUtils.writeByteArrayToFile(getFile(uid, id, suffix), bytes);
                } catch (IOException e) {
                    throw Utils.soft(e);
                }
            }

            public byte[] read(String uid, String id, AttachementSuffix suffix) {
                try {
                    return FileUtils.readFileToByteArray(getFile(uid, id, suffix));
                } catch (IOException e) {
                    throw Utils.soft(e);
                }
            }

            public void delete(String uid, final String id) {
                File[] files = getPartBasePath(uid).listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return FilenameUtils.getBaseName(name).equals(id);
                    }
                });
                for (File file : files) {
                    file.delete();
                }
            }
        }
    };

    public abstract class Repo<T extends Node> {

        protected abstract File getBasePath();

        protected abstract Class<T> getType();

        public void ensureBasePath() {
            getBasePath().mkdirs();
        }

        public String createAndReturnUid(T node) throws ValidationException {
            String uid = createUid(node.name);
            node.uid = uid;
            update(node);
            return node.uid;
        }

        protected File getDir(String uid) {
            return new File(getBasePath(), uid);
        }

        public T read(String uid) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                File dir = getDir(uid);
                dir.mkdirs();
                return mapper.readValue(getMetaFile(dir), getType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private File getMetaFile(File dir) {
            return new File(dir, "meta.json");
        }

        public void delete(String uid) {
            File dir = getDir(uid);
            dir.delete();
        }

        public void update(final T node) throws ValidationException {
            ValidationException.validate(node);
            File dir = getDir(node.uid);
            dir.mkdirs();
            new AtomicWriter(getMetaFile(dir)) {
                @Override
                protected void writeUnsafe(File larve) throws Exception {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(larve, node);
                }
            };
        }

        public void visit(FileRepoVisitor<T> visitor) {
            String[] fileNames = getBasePath().list();
            for (String fileName : fileNames) {
                T node;
                try {
                    node = read(fileName);
                } catch (Exception e) {
                    Logger.warn("error visiting " + getBasePath() + "/" + fileName + " but proceed", e);
                    continue;
                }
                if (!visitor.visit(node)) {
                    break;
                }
            }
        }

    }

}
