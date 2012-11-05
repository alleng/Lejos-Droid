package ieor140;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.android.Header;
import lejos.android.RCNavigationControl;
import lejos.android.XmppComm;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ControlComm {

    private String TAG = "RCNavComms";
    Handler mUIMessageHandler;

    /**
     * Constructor: takes in a PCGuiControl
     * 
     * @param control
     */
    public ControlComm(Handler mUIMessageHandler) {
        System.out.println(" Control Comm built ***");
        this.mUIMessageHandler = mUIMessageHandler;
    }

    /**
     * connects establish bluetooth connection to named robot uses connect
     * method from lejos Android RCNavigationControl
     * 
     * @param name
     *            name of robot
     */
    public boolean connect(String name, String address) {
        Log.d(TAG, " connecting to " + name + " " + address);
        connector = new NXTConnector();

        boolean connected = connector.connectTo(name, address, NXTCommFactory.BLUETOOTH);
        System.out.println(" connect result " + connected);

        if (!connected) {
            return connected;
        }
        dataIn = connector.getDataIn();
        dataOut = connector.getDataOut();
        if (dataIn == null) {
            connected = false;
            return connected;
        }
        if (!reader.isRunning) {
            reader.start();
        }
        xmppComm.start();
        return connected;
    }

    /**
     * sends data to the PC with header
     * 
     * @param header
     * @param data
     * @param bit
     * @throws Exception
     */
    public void send(int header, float[] data, boolean bit) throws Exception {
        System.out.println(" Comm send " + header + " " + data[0] + " " + data[1] + " " + data[2]);
        try {
            dataOut.writeInt(header);
            dataOut.writeFloat(data[0]);
            dataOut.writeFloat(data[1]);
            dataOut.writeFloat(data[2]);
            dataOut.writeBoolean(bit);
            dataOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        reader.reading = true;
    }

    public NXTConnector getConnector() {
        return connector;
    }

    public void end() {
        reader.isRunning = false;
    }

    public void newMessage(Header header, float[] data, boolean bit) {
        Bundle b = new Bundle();
        b.putFloatArray(RCNavigationControl.ROBOT_POS, data);
        Message message_holder = new Message();
        message_holder.setData(b);
        message_holder.what = header.ordinal();
        mUIMessageHandler.sendMessage(message_holder);
    }

    /**
     * Reads incoming messages and passes them to the UI Thread
     */
    class Reader extends Thread {

        public boolean reading = true;
        int count = 0;
        boolean isRunning = false;

        @Override
        public void run() {
            setName("RCNavComms read thread");
            isRunning = true;
            float x = 0;
            float y = 0;
            float h = 0;
            int header = 0;
            float[] data = new float[3];
            boolean isMoving;
            while (isRunning) {
                if (reading) // reads one message at a time
                {
                    Log.d(TAG, "reading ");

                    boolean ok = false;
                    try {
                        header = dataIn.readInt();
                        x = dataIn.readFloat();
                        y = dataIn.readFloat();
                        h = dataIn.readFloat();
                        isMoving = dataIn.readBoolean();
                        data[0] = x;
                        data[1] = y;
                        data[2] = h;
                        ok = true;
                        Log.d(TAG, "data  " + x + " " + y + " " + h);
                    } catch (IOException e) {
                        Log.d(TAG, "connection lost");
                        count++;
                        // isRunning = count < 20;// give up
                        ok = false;
                    }
                    if (ok) {
                        sendToUIThread(header, data);
                        // reading = false;
                    }
                }
            }// if reading
            Thread.yield();
        }// while is running

    }

    public void sendToUIThread(int header, float[] data) {
        Bundle b = new Bundle();
        b.putFloatArray(RCNavigationControl.ROBOT_POS, data);
        Message message_holder = new Message();
        message_holder.what = header;
        message_holder.setData(b);
        mUIMessageHandler.sendMessage(message_holder);
    }

    /**
     * default bluetooth address. used by reader
     */
    // String address = "";
    /**
     * connects to NXT using bluetooth. Provides data input stream and data
     * output stream
     */
    private NXTConnector connector = new NXTConnector();
    /**
     * used by reader
     */
    private DataInputStream dataIn;
    /**
     * used by send()
     */
    private DataOutputStream dataOut;
    /**
     * inner class extends Thread; listens for incoming data from the NXT
     */
    private Reader reader = new Reader();

    public XmppComm xmppComm = new XmppComm(this);

}
