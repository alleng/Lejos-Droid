package ieor140;

import lejos.android.Header;

/**
 * Interface to dictate format for sending and receiving messages
 * 
 * @author duttasho
 */
public interface CommListener {
    public void newMessage(Header header, float[] data, boolean bit);
}
