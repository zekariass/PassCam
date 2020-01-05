package com.cvproject.passcam;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PassCameraView extends JavaCameraView implements Camera.PictureCallback {


    private Context context;


    private String mPictureFileName;


    public PassCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public void takePicture (final String filename){
        this.mPictureFileName = filename;
        mCamera.setPreviewCallback(null);
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // The camera preview was automatically stopped. Start it again.

        try {
            TimeUnit.SECONDS.sleep(2);
        }catch (Exception e){
            Log.d("WAIT ERROR: ", e.getMessage());
        }

        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Uri uri = Uri.parse(mPictureFileName);

        Bitmap bm = null;
        bm = rotate(bitmap, 90);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);
            bm.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }
    }

    private static Bitmap rotate(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            return bmOut;
        }
        return bm;
    }
}
