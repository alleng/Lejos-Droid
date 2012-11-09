/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Interface containing format of messages to be sent and received
 * @author duttasho
 */
public interface CommListener {
    public void  newMessage(Header header, float[] data, boolean bit);
}
