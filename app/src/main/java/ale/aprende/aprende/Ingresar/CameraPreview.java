package ale.aprende.aprende.Ingresar;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Created by Alejandro on 22/05/2017.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    //Declaración de variables
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private CameraPreview mPreview;
    //Constructor
    private boolean previewIsRunning;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.mCamera = camera;
        // Instala un SurfaceHolder.Callback para que seamos notificados cuando el
        // se crea y destruye la superficie subyacente.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {

        try {
            if (mCamera != null) {
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        Log.d("Function", "surfaceChanged iniciado");
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here


        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
        /*
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
        */
    }


    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void myStartPreview() {
        if (!previewIsRunning && (mCamera != null)) {
            mCamera.startPreview();
            previewIsRunning = true;
        }
    }

    // same for stopping the preview
    public void myStopPreview() {
        if (Build.VERSION.SDK_INT == 21) {

            if (previewIsRunning && (mCamera != null)) {
                //mCamera.release();
                mCamera.stopPreview();

                previewIsRunning = false;
            }


        }
    }
}
