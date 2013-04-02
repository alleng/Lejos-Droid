package com.example.lejos_droid_test;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Main Activity: Runs when android is run
 * 
 * @author Shouvik
 * 
 */
public class RCNavigationControl extends Activity {
    protected static final String TAG = "RCNavigationControl";
    private boolean connected;
    private Handler mUIMessageHandler;
    private ControlComm communicator;
    EditText mName;
    private ConnectThread mConnectThread;
    public static final String ROBOT_POS = "rp";
    public static final String MESSAGE_CONTENT = "String_message";
    public static final int MESSAGE = 1000;
    public static final int TOAST = 2000;
    CameraSurfaceView csv;
    String pathToOurFile = Environment.getExternalStorageDirectory() + File.separator
            + "lejosImage.jpg";

    class UIMessageHandler extends Handler {
        float[] pos;
        float[] data;

        @Override
        public void handleMessage(Message msg) {
            // Log.d(TAG, "handleMessage");

            data = msg.getData().getFloatArray(ROBOT_POS);
            int header = msg.what;
            if (header == Header.POSE.ordinal()) {
                showRobotPosition(data[0], data[1], data[2]);
            } else if (header == MESSAGE) {
                ((TextView) findViewById(R.id.message)).setText(((String) msg.getData().get(
                        MESSAGE_CONTENT)));
            } else if (header == Header.MAP.ordinal()) {
                // drawObstacle(data[0], data[1]);
            } else if (header == Header.TRAVEL.ordinal()) {

            } else if (header == Header.PICTURE.ordinal()) {
                takePicture();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        Log.d(TAG, "onCreate 0");

        // Sets up NXJ cache file that is required to connect
        seupNXJCache();
        findViewById(R.id.connectButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doConnect();
            }
        });
        csv = new CameraSurfaceView(this);
        FrameLayout fl = (FrameLayout) findViewById(R.id.preview);
        fl.addView(csv);

    }

    public void showRobotPosition(float x, float y, float h) {
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView xText = (TextView) findViewById(R.id.xText);
        TextView yText = (TextView) findViewById(R.id.yText);
        TextView hText = (TextView) findViewById(R.id.hText);
        xText.setTypeface(tf);
        yText.setTypeface(tf);
        hText.setTypeface(tf);
        xText.setText("" + x);
        yText.setText("" + y);
        hText.setText("" + h + "¡");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu:
            communicator.end();
            try {
                communicator.getConnector().close();
                if (communicator.xmppComm.isRunning) {
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
            Editable name = ((EditText) findViewById(R.id.input)).getText();
            // Editable address = mAddress.getText();
            sendMessageToUIThread("Connecting ... ");
            if (!communicator.connect(name.toString(), "")) {
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
            b.putString(MESSAGE_CONTENT, message);
            Message message_holder = new Message();
            message_holder.setData(b);
            message_holder.what = MESSAGE;
            mUIMessageHandler.sendMessage(message_holder);
        }

    }

    public void takePicture() {
        // camera.startPreview();
        csv.getCamera().takePicture(null, null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
                // iv.setImageBitmap(b);
                try {
                    File f = new File(pathToOurFile);
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    b.compress(Bitmap.CompressFormat.JPEG, 50, new FileOutputStream(f));
                    Log.e("UPLOAD", "started");
                    new UploadTask().execute();
                    Log.e("UPLOAD", "finished");
                } catch (Exception e) {

                }
            }
        });
    }

    public void uploadImage() {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;

        String urlServer = "http://www.ocf.berkeley.edu/~duttasho/lejos_upload.php";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        try {
            FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile));

            URL url = new URL(urlServer);
            connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs & Outputs
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Enable POST method
            connection.setRequestMethod("POST");

            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="
                    + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream
                    .writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
                            + pathToOurFile + "\"" + lineEnd);
            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // Read file
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            // int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();
            Log.e("UPLOAD", serverResponseMessage);

            fileInputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (Exception ex) {
            // Exception handling
            ex.printStackTrace();
        }
    }

    class UploadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            uploadImage();
            return null;
        }

    }
}
