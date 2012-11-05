package lejos.android;

import ieor140.ControlComm;
import ieor140.Map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.TabActivity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

/**
 * Main Activity: Runs when android is run NOTES:there must be an nx
 * 
 * @author Shouvik
 * 
 */
public class RCNavigationControl extends TabActivity {
    protected static final String TAG = "RCNavigationControl";
    private boolean connected;
    private Handler mUIMessageHandler;
    private ControlComm communicator;
    TextView mMessage;
    EditText mName;
    EditText mAddress;
    Map map;
    private ConnectThread mConnectThread;
    public static final String ROBOT_POS = "rp";

    class UIMessageHandler extends Handler {
        float[] pos;
        float[] data;

        @Override
        public void handleMessage(Message msg) {
            // Log.d(TAG, "handleMessage");

            data = msg.getData().getFloatArray(ROBOT_POS);
            int header = msg.what;
            if (header == Header.POSE.ordinal()) {
                showtRobotPosition(data[0], data[1], data[2]);
            } else if (header == LeJOSDroid.MESSAGE) {
                mMessage.setText((String) msg.getData().get(LeJOSDroid.MESSAGE_CONTENT));
            } else if (header == Header.MAP.ordinal()) {
                drawObstacle(data[0], data[1]);
            } else if (header == Header.TRAVEL.ordinal()) {

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Log.d(TAG, "onCreate 0");

        // Sets up NXJ cache file that is required to connect
        seupNXJCache();
        // end setup

        TabHost tabHost = getTabHost();
        View layout = LayoutInflater.from(this).inflate(R.layout.rc_nav_control,
                tabHost.getTabContentView(), true);

        // Creates the tabs for the interface
        TabSpec ConnectTab = tabHost.newTabSpec("tab1");
        TabSpec NavigateTab = tabHost.newTabSpec("tab2");
        TabSpec PoseTab = tabHost.newTabSpec("tab3");
        TabSpec MapTab = tabHost.newTabSpec("tab4");
        // End create tabs

        /*
         * Sets labels and contents of tabs: Labels are read from
         * /res/values/string.xml while the content is received from
         * /res/layout/rc_nav_control.xml
         */
        ConnectTab.setIndicator(getResources().getText(R.string.connect_tab))
                .setContent(R.id.view1);
        NavigateTab.setIndicator(getResources().getText(R.string.command_tab)).setContent(
                R.id.view2);
        PoseTab.setIndicator(getResources().getText(R.string.xyz_tab)).setContent(R.id.view3);
        MapTab.setIndicator(getResources().getText(R.string.map_tab)).setContent(R.id.robotMap);
        // End Set Tab Info

        // Add tabs to Interface
        tabHost.addTab(ConnectTab);
        tabHost.addTab(NavigateTab);
        tabHost.addTab(PoseTab);
        tabHost.addTab(MapTab);
        // end Add tabs

        /*
         * Prints messages in LogCat when Tabs have been created and assigns
         * variables to various elements. Also sets the control variable in Map
         * by giving this object
         */
        Log.d(TAG, "onCreate 1");
        mMessage = (TextView) findViewById(R.id.message_status);
        Log.d(TAG, "onCreate 2");
        mName = (EditText) findViewById(R.id.name_edit);
        Log.d(TAG, "onCreate 3");
        mAddress = (EditText) findViewById(R.id.address_edit);
        Log.d(TAG, "onCreate 4");
        map = (Map) findViewById(R.id.robotMap);
        map.setControl(this);
        // end Messages
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Method called when Connect button is pressed. This method starts the
     * Connect Thread and starts the connector;
     */
    private void doConnect() {

        Log.d(TAG, "doConnect");
        mConnectThread = new ConnectThread();
        mConnectThread.start();
    }

    /**
     * Sends data from phone to robot via the communicator
     * 
     * @param header
     *            Header ENUM, tells what type of message this is
     * @param data
     *            array of 3 floats containing the data
     * @param bit
     */
    public void send(Header header, float[] data, boolean bit) {

        try {
            communicator.send(header.ordinal(), data, bit);
        } catch (Exception e) {
        }
    }

    /**
     * Parses X and Y fields for float input, creates the data array, then calls
     * the send method using the Header DESTINATION
     */
    private void goButtonMouseClicked() {
        if (!connected) {
            return;
        }

        float x;
        float y;
        try {
            EditText XField = (EditText) findViewById(R.id.goto_x_edit);
            EditText YField = (EditText) findViewById(R.id.goto_y_edit);
            mMessage.setText("GoTo " + XField.getText() + " " + YField.getText());

            x = Float.parseFloat(XField.getText().toString());
            y = Float.parseFloat(YField.getText().toString());
            System.out.println("Sent " + Header.TRAVEL + " x " + x + " y " + y);
            float[] data = { x, y, 0 };
            Header header = Header.DESTINATION;
            send(header, data, true);
            // mMessage.setText("waiting for data");
        } catch (NumberFormatException e) {
            mMessage.setText("Invalid x, y values");
        }
    }

    /**
     * Parses the distance Field for a float, creates the data array, then sends
     * using the TRAVEL Header
     */
    private void travelButtonMouseClicked() {
        if (!connected) {
            return;
        }
        EditText distanceField = (EditText) findViewById(R.id.travel_edit);
        mMessage.setText("Travel " + distanceField.getText());
        float distance;
        try {
            distance = Float.parseFloat(distanceField.getText().toString());
            System.out.println("Sent " + Header.TRAVEL + " " + distance);
            float[] data = { distance, 0, 0 };
            send(Header.TRAVEL, data, true);
            mMessage.setText("waiting for data");
        } catch (NumberFormatException e) {
            mMessage.setText("Invalid distance value");
        }
    }

    /**
     * Parses the Angle field for a float, creates the data array, then sends
     * using the ROTATE Header
     */
    private void rotateButtonMouseClicked() {
        if (!connected) {
            return;
        }
        EditText angleField = (EditText) findViewById(R.id.rotate_edit);
        mMessage.setText("Rotate " + angleField.getText());
        float angle;
        try {
            angle = Float.parseFloat(angleField.getText().toString());
            System.out.println("Sent " + Header.ROTATE + " " + angle);
            float[] data = { angle, 0, 0 };
            send(Header.ROTATE, data, true);
            mMessage.setText("waiting for data");
        } catch (NumberFormatException e) {
            mMessage.setText("Invalid angle value");
        }
    }

    /**
     * Parses the pose_x, pose_y, and pose-heading fields for floats, then sends
     * using the POSE header. Also draws new Pose on Map
     */
    private void poseButtonClicked() {
        float x;
        float y;
        float heading;
        try {
            EditText XField = (EditText) findViewById(R.id.pose_x_edit);
            EditText YField = (EditText) findViewById(R.id.pose_y_edit);
            EditText HeadingField = (EditText) findViewById(R.id.pose_heading_edit);
            x = Float.parseFloat(XField.getText().toString());
            y = Float.parseFloat(YField.getText().toString());
            heading = Float.parseFloat(HeadingField.getText().toString());
            float[] data = { x, y, heading };
            send(Header.POSE, data, true);
            showtRobotPosition(x, y, heading);
        } catch (Exception e) {

        }
    }

    /**
     * Sends message with STOP Header
     */
    public void stopButtonClicked() {
        float[] data = { 0, 0, 0 };
        send(Header.STOP, data, true);
    }

    /**
     * Parses goto_x and goto_y text fields for Floats, creates data array
     * (Note: 3rd element dictates which direction to map: Left = 0, Right = 1).
     * Sends with MAP Header
     */
    public void mapLeftButtonClicked() {
        float x;
        float y;
        try {
            EditText XField = (EditText) findViewById(R.id.goto_x_edit);
            EditText YField = (EditText) findViewById(R.id.goto_y_edit);
            mMessage.setText("GoTo " + XField.getText() + " " + YField.getText());

            x = Float.parseFloat(XField.getText().toString());
            y = Float.parseFloat(YField.getText().toString());
            float[] data = { x, y, 0 };
            Header header = Header.MAP;
            send(header, data, true);
            // mMessage.setText("waiting for data");
        } catch (NumberFormatException e) {
            mMessage.setText("Invalid x, y values");
        }
    }

    /**
     * Parses goto_x and goto_y text fields for Floats, creates data array
     * (Note: 3rd element dictates which direction to map: Left = 0, Right = 1).
     * Sends with MAP Header
     */
    public void mapRightButtonClicked() {
        float x;
        float y;
        try {
            EditText XField = (EditText) findViewById(R.id.goto_x_edit);
            EditText YField = (EditText) findViewById(R.id.goto_y_edit);
            mMessage.setText("GoTo " + XField.getText() + " " + YField.getText());

            x = Float.parseFloat(XField.getText().toString());
            y = Float.parseFloat(YField.getText().toString());
            float[] data = { x, y, 1 };
            Header header = Header.MAP;
            send(header, data, true);
            // mMessage.setText("waiting for data");
        } catch (NumberFormatException e) {
            mMessage.setText("Invalid x, y values");
        }
    }

    /**
     * Called when goTo Button is Clicked
     * 
     * @param v
     */
    public void handleGoTo(View v) {
        goButtonMouseClicked();
    }

    /**
     * Called when Connect Button is Clicked
     * 
     * @param v
     */
    public void handleConnect(View v) {
        Log.d(TAG, "handleConnect called ");
        doConnect();
    }

    /**
     * Called when Rotate Button is Clicked
     * 
     * @param v
     */
    public void handleRotate(View v) {
        rotateButtonMouseClicked();
    }

    /**
     * Called when Travel Button is Clicked
     * 
     * @param v
     */
    public void handleTravel(View v) {
        travelButtonMouseClicked();
    }

    /**
     * Called when Pose Button is Clicked
     * 
     * @param v
     */
    public void handlePose(View v) {
        poseButtonClicked();
    }

    /**
     * Called when Stop Button is Clicked
     * 
     * @param v
     */
    public void handleStop(View v) {
        stopButtonClicked();
    }

    /**
     * Called when Map Left Button is Clicked
     * 
     * @param v
     */
    public void handleMapLeft(View v) {
        mapLeftButtonClicked();
    }

    /**
     * Map Right
     * 
     * @param v
     */
    public void handleMapRight(View v) {
        mapRightButtonClicked();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu:
            communicator.end();
            try {
                communicator.getConnector().close();
                if (communicator.xmppComm.isRunning == true) {
                    communicator.xmppComm.connection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
         * Actually ends connections to bluetooth and XMPP when program closes;
         * however, will also end when phone sleeps, which disrupts operation
         * 
         * communicator.end(); communicator.getConnector().close();
         * communicator.xmppComm.connection.disconnect();
         */
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUIMessageHandler = new UIMessageHandler();
        communicator = new ControlComm(mUIMessageHandler);
    }

    /**
     * Draws robot position on map by adding coordinates to the end of the Path
     * arrayList in Map
     * 
     * @param x
     *            X-Coordinate of Robot
     * @param y
     *            Y-Coordinate of Robot
     * @param heading
     *            Heading of Robot in degrees from X-axis
     */
    public void showtRobotPosition(float x, float y, float heading) {
        Log.d(TAG, "showtRobotPosition");
        final TextView xField = (TextView) findViewById(R.id.x);
        final TextView yField = (TextView) findViewById(R.id.y);
        final TextView headingField = (TextView) findViewById(R.id.heading);
        xField.setText("" + x);
        yField.setText("" + y);
        headingField.setText("" + heading);
        mMessage.setText("waiting for command");
        Point point = new Point((int) x, (int) y);
        map.path.add(point);
        map.reDraw();

    }

    /**
     * Draws obstacle on Map by adding it to the Obstacles arrayList
     * 
     * @param x
     *            X-Coordinate of Obstacle
     * @param y
     *            Y-Coordinate of Obstacle
     */
    public void drawObstacle(float x, float y) {
        Point point = new Point((int) x, (int) y);
        map.obstacles.add(point);
        map.reDraw();
    }

    /**
     * Sets goto_x and goto_y text fields with parameters x and y - Called when
     * user touches map to set Destination
     * 
     * @param x
     *            Desired X-Coordinate
     * @param y
     *            Desired Y-Coordinate
     */
    public void setRobotDestination(int x, int y) {
        EditText XField = (EditText) findViewById(R.id.goto_x_edit);
        EditText YField = (EditText) findViewById(R.id.goto_y_edit);
        XField.setText("" + x);
        YField.setText("" + y);
        map.reDraw();
    }

    private void seupNXJCache() {

        File root = Environment.getExternalStorageDirectory();

        try {
            String androidCacheFile = "nxj.cache";
            File mLeJOS_dir = new File(root + "/leJOS");
            if (!mLeJOS_dir.exists()) {
                mLeJOS_dir.mkdir();

            }
            File mCacheFile = new File(root + "/leJOS/", androidCacheFile);

            if (root.canWrite() && !mCacheFile.exists()) {
                FileWriter gpxwriter = new FileWriter(mCacheFile);
                BufferedWriter out = new BufferedWriter(gpxwriter);
                out.write("");
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not write nxj.cache " + e.getMessage(), e);
        }
    }

    private class ConnectThread extends Thread {

        @Override
        public void run() {
            Looper.prepare();
            setName("RCNavigationControl ConnectThread");
            Editable name = mName.getText();
            Editable address = mAddress.getText();
            sendMessageToUIThread("Connecting ... ");
            if (!communicator.connect(name.toString(), address.toString())) {
                sendMessageToUIThread("Connection Failed");
                connected = false;
            } else {
                sendMessageToUIThread("Connected to "
                        + communicator.getConnector().getNXTInfo().name);

                connected = true;
            }
            Looper.loop();
        }

        /**
         * Sends Message to UI Thread: Used by run() to notify user of status of
         * connection
         * 
         * @param message
         *            Message to send
         */
        public void sendMessageToUIThread(String message) {
            // Log.d(TAG,"sendMessageToUIThread: "+message);
            Bundle b = new Bundle();
            b.putString(LeJOSDroid.MESSAGE_CONTENT, message);
            Message message_holder = new Message();
            message_holder.setData(b);
            message_holder.what = LeJOSDroid.MESSAGE;
            mUIMessageHandler.sendMessage(message_holder);
        }

    }
}
