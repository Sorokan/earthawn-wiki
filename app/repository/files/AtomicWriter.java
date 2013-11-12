package repository.files;

import java.io.File;

public abstract class AtomicWriter {

    public AtomicWriter(File file) {
        File larve = new File(file.getParentFile(), file.getName() + ".larve");
        try {
            writeUnsafe(larve);
        } catch (Exception e) {
            throw Utils.soft(e);
        }
        file.delete();
        larve.renameTo(file);
    }

    protected abstract void writeUnsafe(File larve) throws Exception;

}
