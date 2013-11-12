package repository.files;

public class Utils {

    public static RuntimeException soft(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

}
