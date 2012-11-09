import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import java.io.*;
import lejos.nxt.*;

/**
 * Connects to robot and sends/receives messages
 * 
 * @author
 */
public class Communicator {

    DataInputStream dataIn;
    DataOutputStream dataOut;
    int x = 0;
    int y = 0;
    boolean shouldStop = false;
    CommListener myListener;

    /**
     * Constructor
     * 
     * @param listener
     *            takes in an object that implements CommListener: in this case,
     *            BTNavigation
     */
    public Communicator(CommListener listener) {
        myListener = listener;
    }

    /**
     * connects to PC via bluetooth
     */
    public void connect() {
        // you can copy most of this code from BTRecieve
        LCD.drawString("Waiting for Connection...", 0, 1);
        BTConnection btc = Bluetooth.waitForConnection();
        LCD.drawString("Connected", 0, 1);
        dataIn = btc.openDataInputStream();
        dataOut = btc.openDataOutputStream();
        Sound.beepSequence();
        if (dataIn != null) {
            Reader reader = new Reader();
            reader.start();
        }
    }

    class Reader extends Thread {

        int count = 0;
        boolean isRunning = false;

        public void run() {
            System.out.println(" reader started ");
            isRunning = true;
            float[] data = new float[3];

            while (isRunning) {
                try {
                    int header = dataIn.readInt();
                    for (int i = 0; i < 3; i++) {
                        data[i] = dataIn.readFloat();
                    }
                    boolean isMoving = dataIn.readBoolean();
                    myListener.newMessage(Header.values()[header], data, isMoving);
                } catch (IOException e) {
                    System.out.println("Input Error");
                    count++;
                }

            }
        }
    }

    /**
     * Gets destination from PC by reading stream
     * 
     * @return x and y returns x and y coordinates
     */
    public float[] getDestination() {

        LCD.drawString("Read ", 0, 3);
        float xy[] = { x, y };
        return xy;
    }

    /**
     * Sends data to PC. Header indicates whether data contains location of
     * obstacle or arrival at node
     * 
     * @param header
     *            Type of data: 0 for arrival at node, 1 for location of
     *            obstacle
     * @param data
     * @param bit
     */
    public void sendData(Header header, float[] data, boolean bit) {
        try {
            dataOut.writeInt(header.ordinal());
            dataOut.writeFloat(data[0]);
            dataOut.writeFloat(data[1]);
            dataOut.writeFloat(data[2]);
            dataOut.writeBoolean(bit);
            dataOut.flush();
        } catch (Exception e) {
        }
    }
}
