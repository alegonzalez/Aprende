package ale.aprende.aprende;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

public class Ingresar extends AppCompatActivity {
    //Declaración de variables
    private static final String TAG = "FaceTrackerDemo";
    private CameraSource mCameraSource = null;
    private CameraSurfacePreview mPreview;
    private CameraOverlay cameraOverlay;
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingresar);
        DBHandler db1 = new DBHandler(this);
        SQLiteDatabase db = db1.getWritableDatabase();
        if (db != null) {
            Categoria categoria = new Categoria(db);
            categoria.llenarTablaCategoria();
        }
        db.close();
        mPreview = (CameraSurfacePreview) findViewById(R.id.preview);
        cameraOverlay = (CameraOverlay) findViewById(R.id.faceOverlay);
        //verificar si hay permisos en el manifest
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{android.Manifest.permission.CAMERA};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }
        final Activity thisActivity = this;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            }
        };

//        Snackbar.make(cameraOverlay, "Camera permission is required", Snackbar.LENGTH_INDEFINITE).setAction("OK", listener).show();
    }

    private void createCameraSource() {
        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context).setClassificationType(FaceDetector.ALL_CLASSIFICATIONS).build();
        detector.setProcessor(new MultiProcessor.Builder<>(new Ingresar.GraphicFaceTrackerFactory()).build());
        if (!detector.isOperational()) {
            Log.e(TAG, "Las dependencias de detección de rostros aún no están disponibles.");
        }
        mCameraSource = new CameraSource.Builder(context, detector).setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Resultado obtenido de permiso inesperado: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permiso de cámara concedido - inicializa la fuente de cámara");
            createCameraSource();
            return;
        }
        Log.e(TAG, "Permiso no concedido resultado  = " + grantResults.length + " código de resultado = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Aprende")
                .setMessage("Necesitas permisos de acceso a la camara!")
                .setPositiveButton("OK", listener)
                .show();
    }

    private void startCameraSource() {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, cameraOverlay);
            } catch (IOException e) {
                Log.e(TAG, "No se puede iniciar la fuente de la cámara.", e);
                mCameraSource.release();
                mCameraSource = null;
            }

        }
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {

        @Override

        public Tracker<Face> create(Face face) {

            return new Ingresar.GraphicFaceTracker(cameraOverlay);

        }

    }

    private class GraphicFaceTracker extends Tracker<Face> {

        private CameraOverlay mOverlay;

        private FaceOverlayGraphics faceOverlayGraphics;

        GraphicFaceTracker(CameraOverlay overlay) {

            mOverlay = overlay;

            faceOverlayGraphics = new FaceOverlayGraphics(overlay);

        }

        @Override

        public void onNewItem(int faceId, Face item) {

            faceOverlayGraphics.setId(faceId);

        }

        @Override

        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {

            mOverlay.add(faceOverlayGraphics);

            faceOverlayGraphics.updateFace(face);

        }

        @Override

        public void onMissing(FaceDetector.Detections<Face> detectionResults) {

            mOverlay.remove(faceOverlayGraphics);

        }

        @Override

        public void onDone() {

            mOverlay.remove(faceOverlayGraphics);
        }

    }
}
