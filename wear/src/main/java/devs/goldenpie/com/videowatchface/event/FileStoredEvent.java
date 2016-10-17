package devs.goldenpie.com.videowatchface.event;

import lombok.Getter;

public class FileStoredEvent {

    @Getter
    private String path;

    public FileStoredEvent(String path) {
        this.path = path;
    }
}
