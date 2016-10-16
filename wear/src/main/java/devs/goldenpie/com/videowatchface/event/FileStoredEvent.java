package devs.goldenpie.com.videowatchface.event;

import devs.goldenpie.com.videowatchface.model.DataModel;

public class FileStoredEvent {
    private DataModel dataModel;

    public FileStoredEvent(DataModel model) {
        dataModel = model;
    }

    public DataModel getDataModel() {
        return dataModel;
    }

}
