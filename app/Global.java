import java.io.File;

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Play;
import repository.RepositoryService;
import repository.files.FileRepository;
import repository.files.Utils;
import repository.files.ValidationException;

public class Global extends GlobalSettings {

    @Override
    public void onStart(Application app) {
        FileRepository fileRepo = new FileRepository(new File("store"));
        try {
            RepositoryService.create(fileRepo);
        } catch (ValidationException e) {
            throw Utils.soft(e);
        }
    }

    @Override
    public void onStop(Application app) {
    }

}