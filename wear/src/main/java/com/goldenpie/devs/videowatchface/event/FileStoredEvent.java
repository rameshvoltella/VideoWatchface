package com.goldenpie.devs.videowatchface.event;

import com.goldenpie.devs.videowatchface.model.db.FileModel;
import lombok.Getter;

public class FileStoredEvent {

    @Getter
    private FileModel fileModel;

    public FileStoredEvent(FileModel path) {
        this.fileModel = path;
    }
}
