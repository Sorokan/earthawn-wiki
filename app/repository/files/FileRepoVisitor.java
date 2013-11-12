package repository.files;

public interface FileRepoVisitor<T> {
    public boolean visit(T node);
}