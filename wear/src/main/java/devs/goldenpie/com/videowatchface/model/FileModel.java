package devs.goldenpie.com.videowatchface.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import lombok.Data;

/**
 * @author anton
 * @version 3.4
 * @since 18.10.16
 */
@Data
@Table(name = "FileModel")
public class FileModel extends Model {
    @Column(name = "data")
    private byte[] data;

    public static boolean isExist() {
        return new Select().from(FileModel.class).exists();
    }

    public static byte[] getFile() {
        return getFileModel().getData();
    }

    public static FileModel getFileModel() {
        return new Select().from(FileModel.class).executeSingle();
    }
}
