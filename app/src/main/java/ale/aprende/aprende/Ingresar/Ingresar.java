package ale.aprende.aprende.Ingresar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ale.aprende.aprende.R;
import ale.aprende.aprende.helper.ImageHelper;
import ale.aprende.aprende.helper.LogHelper;
import ale.aprende.aprende.helper.SampleApp;
import ale.aprende.aprende.registrar.DBHandler;
import ale.aprende.aprende.registrar.Registrar;

public class Ingresar extends AppCompatActivity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private BitmapFactory.Options options, o, o2;
    private Bitmap imagen1, imagen2;
    private FileInputStream fis;
    public ProgressDialog progressDialog;
    private UUID mFaceId0;
    private UUID mFaceId1;
    protected FaceListAdapter mFaceListAdapter0;
    protected FaceListAdapter mFaceListAdapter1;
    Button btnDetectar, btnVerificar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ale.aprende.aprende.R.layout.activity_ingresar);
        btnDetectar = (Button) findViewById(R.id.btnDetectar);
        btnVerificar = (Button) findViewById(R.id.btnReconocimiento);
        btnVerificar.setEnabled(false);
        if (!(verificarImagenFolder())) {
            Intent intent = new Intent(Ingresar.this, Registrar.class);
            startActivity(intent);
            Toast.makeText(this, "No se encuentra una foto registrada", Toast.LENGTH_SHORT).show();
        } else {
            // crear la instancia de la camara
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
        // Initialize the two ListViews which contain the thumbnails of the detected faces.

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));
        LogHelper.clearVerificationLog();
    }

    //metodo onclick para reconocer el rostro
    public void deteccionRostro(View view) {
        btnDetectar.setEnabled(false);
        mCamera.takePicture(null, null, mPicture);
    }

    public void reconocimiento(View view) {
        new VerificationTask(mFaceId0, mFaceId1).execute();
    }

    /**
     * Verifica si la imagen se encuentra en la carpeta del dispositivo
     */
    private boolean verificarImagenFolder() {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        int verificador = 0;
        Cursor cursor = db.rawQuery("select * from persona", null);
        String[] resultado;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String nombre_imagen = cursor.getString(cursor.getColumnIndex("imagen"));
                String id = cursor.getString(cursor.getColumnIndex("id"));
                resultado = nombre_imagen.split("\\.");
                File file = new File(new File("/sdcard/Aprende/"), resultado[0] + "_" + id + "."+resultado[1]);
                if (file.exists()) {
                    String rostro = cursor.getString(cursor.getColumnIndex("rostro"));
                    imagen1 = BitmapFactory.decodeFile(file.getAbsolutePath());
                    mFaceId0 = UUID.fromString(rostro);
                    verificador = 1;
                }
                cursor.moveToNext();
            }
        }
        return (verificador == 0) ? false : true;
    }

    //Este metodo cambia la camara a frontal o vicebersa, es un onclick
    public void frontal(View view) {
        mCamera.release();
        //swap the id of the camera to be used
        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        mCamera = Camera.open(currentCameraId);
        setCameraDisplayOrientation(Ingresar.this, currentCameraId, mCamera);
        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    //Cambiar la horientación
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    //Una forma segura de obtener una instancia del objeto Cámara
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    //Tomar la foto
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d("", "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("", "Error accessing file: " + e.getMessage());
            }
            mFaceId1 = null;
            imagen2 = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
            detect(imagen2, 1);
            pictureFile.delete();
        }
    };


    public static final int MEDIA_TYPE_IMAGE = 1;

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
        return mediaFile;
    }

    //DETECTAR Y RECONOCER


    // Background task for face verification.
    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        // The IDs of two face to verify.
        private UUID mFaceId0;
        private UUID mFaceId1;

        VerificationTask(UUID faceId0, UUID faceId1) {
            mFaceId0 = faceId0;
            mFaceId1 = faceId1;
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Verifying...");

                // Start verification.
                return faceServiceClient.verify(
                        mFaceId0,      /* The first face ID to verify */
                        mFaceId1);     /* The second face ID to verify */
            } catch (Exception e) {
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            addLog("Request: Verifying face " + mFaceId0 + " and face " + mFaceId1);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(VerifyResult result) {
            if (result != null) {
                addLog("Response: Success. Face " + mFaceId0 + " and face "
                        + mFaceId1 + (result.isIdentical ? " " : " don't ")
                        + "belong to the same person");
            }

            // Show the result on screen when verification is done.
            setUiAfterVerification(result);
        }
    }

    // Set the verify button is enabled or not.
    private void setVerifyButtonEnabledStatus(boolean isEnabled) {
        //Button button = (Button) findViewById(R.id.btnReconocer);
        //button.setEnabled(isEnabled);
    }

    // Start detecting in image specified by index.
    private void detect(Bitmap bitmap, int index) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTask(index).execute(inputStream);
        // Set the status to show that detection starts.
        setInfo("Detecting...");
    }

    // Show the result on screen when verification is done.
    private void setUiAfterVerification(VerifyResult result) {
        // Verification is done, hide the progress dialog.
        progressDialog.dismiss();

        // Enable all the buttons.
        // setAllButtonEnabledStatus(true);

        // Show verification result.
        if (result != null) {
            DecimalFormat formatter = new DecimalFormat("#0.00");
            String verificationResult = (result.isIdentical ? "The same person" : "Different persons")
                    + ". The confidence is " + formatter.format(result.confidence);
            Toast.makeText(this, verificationResult, Toast.LENGTH_SHORT).show();
            setInfo(verificationResult);
        }
    }

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        // Index indicates detecting in which of the two images.
        private int mIndex;
        private boolean mSucceed = true;

        DetectionTask(int index) {
            mIndex = index;
        }

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            //progressDialog.show();
            addLog("Request: Detecting in image" + mIndex);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            // Show the result on screen when detection is done.
            setUiAfterDetection(result, mIndex, mSucceed);
        }

        // Show the result on screen when verification is done.
        private void setUiAfterVerification(VerifyResult result) {
            // Verification is done, hide the progress dialog.
            progressDialog.dismiss();

            // Enable all the buttons.
            // setAllButtonEnabledStatus(true);

            // Show verification result.
            if (result != null) {
                DecimalFormat formatter = new DecimalFormat("#0.00");
                String verificationResult = (result.isIdentical ? "The same person" : "Different persons")
                        + ". The confidence is " + formatter.format(result.confidence);
                setInfo(verificationResult);
            }
        }

        // Show the result on screen when detection in image that indicated by index is done.
        private void setUiAfterDetection(Face[] result, int index, boolean succeed) {
            // setSelectImageButtonEnabledStatus(true, index);

            if (succeed) {
                addLog("Response: Success. Detected "
                        + result.length + " face(s) in image" + index);

                setInfo(result.length + " face" + (result.length != 1 ? "s" : "") + " detected");

                // Show the detailed list of detected faces.
                FaceListAdapter faceListAdapter = new FaceListAdapter(result, index);

                // Set the default face ID to the ID of first face, if one or more faces are detected.
                if (faceListAdapter.faces.size() != 0) {
                    if (index == 0) {
                        mFaceId0 = faceListAdapter.faces.get(0).faceId;
                    } else {
                        mFaceId1 = faceListAdapter.faces.get(0).faceId;
                    }
                    // Show the thumbnail of the default face.
                    //ImageView imageView = (ImageView) findViewById(index == 0 ? R.id.image_0 : R.id.image_1);
                    //imageView.setImageBitmap(faceListAdapter.faceThumbnails.get(0));
                }

                // Show the list of detected face thumbnails.
                //   ListView listView = (ListView) findViewById(
                //         index == 0 ? R.id.list_faces_0 : R.id.list_faces_1);
                // listView.setAdapter(faceListAdapter);
                // listView.setVisibility(View.VISIBLE);

                // Set the face list adapters and bitmaps.
                if (index == 0) {
                    mFaceListAdapter0 = faceListAdapter;
                    imagen1 = null;
                } else {
                    mFaceListAdapter1 = faceListAdapter;
                    imagen2 = null;
                    btnDetectar.setEnabled(false);
                    btnVerificar.setEnabled(true);
                }

            }

            if (result != null && result.length == 0) {
                setInfo("El rostro no pudo ser detectado!");
                btnDetectar.setEnabled(true);
            }

            if ((index == 0 && imagen1 == null) || (index == 1 && imagen2 == null) || index == 2) {
                progressDialog.dismiss();
            }

            if (mFaceId0 != null && mFaceId1 != null) {
                setVerifyButtonEnabledStatus(true);
            }
        }
    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
    }

    // The adapter of the GridView which contains the thumbnails of the detected faces.
    private class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<Face> faces;

        int mIndex;

        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        // Initialize with detection result and index indicating on which image the result is got.
        FaceListAdapter(Face[] detectionResult, int index) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            mIndex = index;

            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
/*
                    try {
                        // Crop face thumbnail without landmarks drawn.
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                index == 0 ? imagen1 : imagen2, face.faceRectangle));
                    } catch (IOException e) {
                        // Show the exception when generating face thumbnail fails.
                        setInfo(e.getMessage());
                    }
                    */

                }
            }
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face, parent, false);
            }
            convertView.setId(position);

            Bitmap thumbnailToShow = faceThumbnails.get(position);
            if (mIndex == 0 && faces.get(position).faceId.equals(mFaceId0)) {
                thumbnailToShow = ImageHelper.highlightSelectedFaceThumbnail(thumbnailToShow);
            } else if (mIndex == 1 && faces.get(position).faceId.equals(mFaceId1)) {
                thumbnailToShow = ImageHelper.highlightSelectedFaceThumbnail(thumbnailToShow);
            }


            // Show the face thumbnail.
            ((ImageView) convertView.findViewById(R.id.image_face)).setImageBitmap(thumbnailToShow);

            return convertView;
        }


    }

    private void addLog(String log) {
        LogHelper.addVerificationLog(log);
    }
}

