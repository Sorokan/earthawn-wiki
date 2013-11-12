package repository;

import repository.files.Attachement;

public class AttachementContent {
    public Attachement attachement;
    public byte[] content;

    public AttachementContent(Attachement attachement, byte[] content) {
        this.attachement = attachement;
        this.content = content;
    }
}