package com.example.lejos_droid_test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {
//    private Camera camera;
    ImageView iv;
    CameraSurfaceView csv;

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
            }
        });
    }
}