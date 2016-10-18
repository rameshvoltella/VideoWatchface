package devs.goldenpie.com.videowatchface.event;

import devs.goldenpie.com.videowatchface.model.FileModel;
import lombok.Getter;

public class FileStoredEvent {

    @Getter
    private FileModel fileModel;

    public FileStoredEvent(FileModel path) {
        this.fileModel = path;
    }
}
