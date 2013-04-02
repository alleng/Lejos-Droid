package com.example.lejos_droid_test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {
    // private Camera camera;
    ImageView iv;
    CameraSurfaceView csv;
    String pathToOurFile = Environment.getExternalStorageDirectory() + File.separator
            + "lejosImage.jpg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        csv = new CameraSurfaceView(this);
        FrameLayout fl = (FrameLayout) findViewById(R.id.preview);
        fl.addView(csv);
        iv = (ImageView) findViewById(R.id.image);
    }

    public void onClick(View view) {
        // camera.startPreview();
        csv.getCamera().takePicture(null, null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
                iv.setImageBitmap(b);
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