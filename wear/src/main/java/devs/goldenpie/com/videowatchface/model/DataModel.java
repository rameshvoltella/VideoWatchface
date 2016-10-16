package devs.goldenpie.com.videowatchface.model;

import java.util.ArrayList;

import pl.droidsonroids.gif.GifDrawable;

public class DataModel {
    private ArrayList<BytesPart> listOfBytes = new ArrayList<>();
    private String author;
    private String description;
    //    private String videoPath;
    private int fullPart;
    //    private ArrayList<Bitmap> frames = new ArrayList<>();
    private GifDrawable gifDrawable;
    public DataModel() {

    }

//    public ArrayList<Bitmap> getFrames() {
//        return frames;
//    }
//
//    public void setFrames(ArrayList<Bitmap> frames) {
//        this.frames = frames;
//    }

    public ArrayList<BytesPart> getListOfBytes() {
        return listOfBytes;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

//    public String getVideoPath() {
//        return videoPath;
//    }
//
//    public void setVideoPath(String videoPath) {
//        this.videoPath = videoPath;
//    }

    public int getFullPart() {
        return fullPart;
    }

    public void setFullPart(int fullPart) {
        this.fullPart = fullPart;
    }

    public GifDrawable getGifDrawable() {
        return gifDrawable;
    }

    public void setGifDrawable(GifDrawable gifDrawable) {
        this.gifDrawable = gifDrawable;
    }
}
