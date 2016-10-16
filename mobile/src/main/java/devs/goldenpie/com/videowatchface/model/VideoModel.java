package devs.goldenpie.com.videowatchface.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Created by EvilDev on 14.10.2016.
 */

@Data
@Table(name = "VideoModel")
public class VideoModel extends Model {
    @Column(name = "name")
    private String name;
    @Column(name = "path")
    private String path;
    @Column(name = "previewPath")
    private String previewPath;

    public static List<VideoModel> getAll() {
        List<VideoModel> videoModels = new ArrayList<>();
        From from = new Select().from(VideoModel.class);

        if (from.exists())
            videoModels.addAll(from.execute());

        return videoModels;
    }
}
