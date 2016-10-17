package devs.goldenpie.com.videowatchface.model;

import java.util.ArrayList;

import lombok.Data;
import pl.droidsonroids.gif.GifDrawable;

@Data
public class DataModel {

    private ArrayList<BytesPart> listOfBytes = new ArrayList<>();
    private int fullPart;
    private String author;
    private String description;

}
