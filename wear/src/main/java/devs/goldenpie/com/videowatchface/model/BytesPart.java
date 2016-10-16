package devs.goldenpie.com.videowatchface.model;

public class BytesPart {

    private int position;
    private byte[] bytes;

    public BytesPart() {
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
