package ale.aprende.aprende.Ingresar;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ale.aprende.aprende.MenuJuego;
import ale.aprende.aprende.R;
import ale.aprende.aprende.helper.ImageHelper;
import ale.aprende.aprende.helper.LogHelper;
import ale.aprende.aprende.helper.SampleApp;
import ale.aprende.aprende.principal.MainActivity;
import ale.aprende.aprende.registrar.Cambiar_foto;
import ale.aprende.aprende.bd.DBHandler;
import ale.aprende.aprende.registrar.Registrar;

public class Ingresar extends AppCompatActivity {
    private Camera mCamara;
    private CameraPreview mVista;
    private int idActualCamara = Camera.CameraInfo.CAMERA_FACING_BACK;
    private Bitmap imagen1, imagen2;
    public ProgressDialog progressDialog;
    private UUID mRostroId0;
    private UUID mRostroId1;
    protected FaceListAdapter mFaceListAdapter0;
    protected FaceListAdapter mFaceListAdapter1;
    private BootstrapButton btnDetectar;
    private String genero = "";
    public static final int MEDIA_TYPE_IMAGE = 1;
    FrameLayout preview;
    private Boolean estadoActividad = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ale.aprende.aprende.R.layout.activity_ingresar);
        btnDetectar = (BootstrapButton) findViewById(R.id.btnDetectar);
        if (!(verificarImagenFolder())) {
            Intent intent = new Intent(Ingresar.this, Registrar.class);
            startActivity(intent);
            Toast.makeText(this, "No se encuentra una foto registrada", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // crear la instancia de la camara
            mCamara = getCameraInstance();
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mCamara.setDisplayOrientation(0);
            }
            preview = (FrameLayout) findViewById(R.id.camera_preview);
            mVista = new CameraPreview(this, mCamara);
            preview.addView(mVista);
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.progress_dialog_title));
            LogHelper.clearVerificationLog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (mCamara == null) {
                // preview.removeAllViews();
                mCamara = Camera.open(0);
                //mVista = new CameraPreview(this, mCamara);
                //preview.addView(mVista);
            }
            if (estadoActividad == true) {
                mVista = new CameraPreview(this, mCamara);
                preview.addView(mVista);
                mCamara.startPreview();
                estadoActividad = false;
            }

            //mVista.myStartPreview();
        } catch (Exception e) {

        }

    }

    @Override
    protected void onStop() {
        estadoActividad = true;
        preview.removeAllViews();
        mCamara.stopPreview();
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        if (mCamara != null) {
            mCamara.release();
        }
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Ingresar.this, MainActivity.class);
        startActivity(intent);
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamara != null) {
            mVista.getHolder().removeCallback(mVista);

            /*
            if (Build.VERSION.SDK_INT == 22) {
                mCamera.release();
            }
*/
        }

    }


    //Cambiar la horientación
    public void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {

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
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mCamara.setDisplayOrientation(0);
        } else {
            mCamara.setDisplayOrientation(90);
        }
    }

    //metodo onclick para reconocer el rostro
    public void deteccionRostro(View view) {
        if (btnDetectar.isEnabled()) {
            mCamara.takePicture(null, null, mPicture);
        }
        btnDetectar.setEnabled(false);
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
                genero = cursor.getString(cursor.getColumnIndex("genero"));
                String id = cursor.getString(cursor.getColumnIndex("id"));
                resultado = nombre_imagen.split("\\.");
                File file = new File(new File("/sdcard/Aprende/"), resultado[0] + "_" + id + "." + resultado[1]);
                if (file.exists()) {
                    String rostro = cursor.getString(cursor.getColumnIndex("rostro"));
                    imagen1 = BitmapFactory.decodeFile(file.getAbsolutePath());
                    mRostroId0 = UUID.fromString(rostro);
                    verificador = 1;
                }
                cursor.moveToNext();
            }
        }
        return (verificador == 0) ? false : true;
    }

    //metodo onclick para cambio de camara
    public void frontal(View view) {
        //swap the id of the camera to be used
        if (idActualCamara == Camera.CameraInfo.CAMERA_FACING_BACK) {
            idActualCamara = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            idActualCamara = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        if (Build.VERSION.SDK_INT != 21 || Build.VERSION.SDK_INT != 22) {
            mCamara.stopPreview();
            mCamara.release();
        }
        mCamara = Camera.open(idActualCamara);
        setCameraDisplayOrientation(Ingresar.this, idActualCamara, mCamara);
        try {
            mCamara.setPreviewDisplay(mVista.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamara.startPreview();
    }

    //Una forma segura de obtener una instancia del objeto Cámara
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {

        }
        return c;
    }


    //Tomar la foto
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File archivo_imagen = obtenerArchivoSalida(MEDIA_TYPE_IMAGE);
            if (archivo_imagen == null) {
                Log.d("", "Error creating media file, check storage permissions");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(archivo_imagen);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("", "Archivos no encontrados: " + e.getMessage());
            } catch (IOException e) {
                Log.d("", "Error al acceder al archivo: " + e.getMessage());
            }

            mRostroId1 = null;
            imagen2 = BitmapFactory.decodeFile(archivo_imagen.getAbsolutePath());
            int width = imagen2.getWidth();

            int height = imagen2.getHeight();


            int newWidth = 200;

            int newHeight = 200;

            // calculate the scale - in this case = 0.4f

            float scaleWidth = ((float) newWidth) / width;

            float scaleHeight = ((float) newHeight) / height;

            Matrix matrix = new Matrix();

            matrix.postScale(scaleWidth, scaleHeight);
            if (idActualCamara == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    matrix.postRotate(0);
                } else {
                    matrix.postRotate(270);
                }

            } else {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    matrix.postRotate(0);
                } else {
                    matrix.postRotate(90);
                }

            }
            Bitmap resizedBitmap = Bitmap.createBitmap(imagen2, 0, 0, width, height, matrix, true);
            mCamara.startPreview();
            detect(resizedBitmap, 1);
            archivo_imagen.delete();
        }
    };

    /**
     * Crear un archivo para guardar una imagen
     */
    private static File obtenerArchivoSalida(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // Cree el directorio de almacenamiento si no existe
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "No se pudo crear el directorio");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File archivo;
        if (type == MEDIA_TYPE_IMAGE) {
            archivo = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
        return archivo;
    }

//DETECTAR Y RECONOCER


    // Tarea de fondo para la verificación de la cara.
    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        private UUID mRostroId0;
        private UUID mRostroId1;

        VerificationTask(UUID faceId0, UUID faceId1) {
            mRostroId0 = faceId0;
            mRostroId1 = faceId1;
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {
            // Obtener una instancia del cliente de servicio de cara para detectar las caras en la imagen.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                // publishProgress("Verificando");
                // Inicio de la verificación
                return faceServiceClient.verify(
                        mRostroId0,      /* The first face ID to verify */
                        mRostroId1);     /* The second face ID to verify */
            } catch (Exception e) {
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            addLog("Request: Verificando rostro " + mRostroId0 + " y el rostro " + mRostroId1);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(VerifyResult result) {
            if (result != null) {
                addLog("Response: Success. Rostro " + mRostroId0 + " y el rostro "
                        + mRostroId1 + (result.isIdentical ? " " : " no ")
                        + "pertenece a la misma persona");
            }
            // Muestra el resultado en la pantalla cuando se realiza la verificación.
            setUiAfterVerification(result);
        }
    }

    // Inicia la detección en la imagen especificada por índice.
    private void detect(Bitmap bitmap, int index) {
        //Poner la imagen en un flujo de entrada para su detección.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        // Iniciar una tarea en segundo plano para detectar rostros en la imagen.
        new DetectionTask(index).execute(inputStream);
        // Establece el estado para mostrar que se inicia la detección.
        setInfo("Detectando...");
    }

    // Mostrar el resultado en la pantalla cuando se realiza la verificación.
    private void setUiAfterVerification(VerifyResult result) {
        // Se realiza la verificación, ocultar el cuadro de diálogo de progreso.
        progressDialog.dismiss();
        // Mostrar el resultado de la verificación.
        if (result != null) {
            DecimalFormat formatter = new DecimalFormat("#0.00");
            String verificationResult = (result.isIdentical ? "La misma persona" : "Diferente persona");
            //+ ". La confianza es de  " + formatter.format(result.confidence);
            if (verificationResult.trim() == "La misma persona") {
                Intent intento = new Intent(Ingresar.this, MenuJuego.class);
                intento.putExtra("genero", genero);
                startActivity(intento);
                finish();
            } else {
                //verificationResult += ". La confianza es de  " + formatter.format(result.confidence);
                setInfo(verificationResult);
                btnDetectar.setEnabled(true);
            }

        }
    }

    // Tarea de fondo de detección de rostros.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private int mIndex;
        private boolean mSucceed = true;

        DetectionTask(int index) {
            mIndex = index;
        }

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Obtener una instancia del cliente de servicio de cara para detectar rostros en la imagen.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Detectando...");

                // Inicio de la detección.
                return faceServiceClient.detect(
                        params[0],
                        true,
                        false,
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
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            // Muestra el resultado en la pantalla cuando se realiza la detección.
            setUiAfterDetection(result, mIndex, mSucceed);
        }

        // Mostrar el resultado en la pantalla cuando se realiza la verificación.
        private void setUiAfterVerification(VerifyResult result) {
            // Se realiza la verificación, ocultar el cuadro de diálogo de progreso.
            progressDialog.dismiss();
            // Show verification result.
            if (result != null) {
                DecimalFormat formatter = new DecimalFormat("#0.00");
                String verificationResult = (result.isIdentical ? "La misma persona" : "Diferente")
                        + ". La confianza es de  " + formatter.format(result.confidence);
                setInfo(verificationResult);
            }
        }

        // Muestra el resultado en la pantalla cuando se realiza la detección en la imagen indicada por índice.
        private void setUiAfterDetection(Face[] result, int index, boolean succeed) {
            if (succeed) {

                addLog("Response: Success. Detectado "
                        + result.length + " rostro(s) en la imagen" + index);
                //setInfo(result.length + " rostro" + (result.length != 1 ? "s" : "") + " detectado");
                // Mostrar la lista detallada de caras detectadas.
                FaceListAdapter faceListAdapter = new FaceListAdapter(result, index);
                // Establece el ID de cara predeterminado en el ID de la primera cara, si se detectan una o más caras.
                if (faceListAdapter.faces.size() != 0) {
                    if (index == 0) {
                        mRostroId0 = faceListAdapter.faces.get(0).faceId;
                    } else {
                        mRostroId1 = faceListAdapter.faces.get(0).faceId;
                    }
                }
                // Establece los adaptadores de lista de rostros y los mapas de bits.
                if (index == 0) {
                    mFaceListAdapter0 = faceListAdapter;
                    imagen1 = null;
                } else {
                    mFaceListAdapter1 = faceListAdapter;
                    imagen2 = null;
                    btnDetectar.setEnabled(false);
                }
                if (faceListAdapter.faces.size() > 0) {
                    new VerificationTask(mRostroId0, mRostroId1).execute();
                }

            } else {
                preview.removeAllViews();
                btnDetectar.setEnabled(true);
            }
            if (result != null && result.length == 0) {
                setInfo("El rostro no pudo ser detectado!");
                btnDetectar.setEnabled(true);
            }
            if ((index == 0 && imagen1 == null) || (index == 1 && imagen2 == null) || index == 2) {
                progressDialog.dismiss();
            }
        }
    }

    // Establece el panel de información en la pantalla.
    private void setInfo(String info) {
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
    }

    // El adaptador del GridView que contiene las miniaturas de las caras detectadas.
    private class FaceListAdapter extends BaseAdapter {
        // Las caras detectadas.
        List<Face> faces;
        int mIndex;
        // Las miniaturas de las caras detectadas.
        List<Bitmap> faceThumbnails;

        // Inicializar con el resultado de la detección y el índice que indica en qué imagen se obtiene el resultado.
        FaceListAdapter(Face[] detectionResult, int index) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            mIndex = index;
            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
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
            if (mIndex == 0 && faces.get(position).faceId.equals(mRostroId0)) {
                thumbnailToShow = ImageHelper.highlightSelectedFaceThumbnail(thumbnailToShow);
            } else if (mIndex == 1 && faces.get(position).faceId.equals(mRostroId1)) {
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

    //metodo onclick para ir a la actividad, para cambiar la foto de perfil
    public void cambiarFotoPerfil(View view) {
        Intent cambiar = new Intent(Ingresar.this, Cambiar_foto.class);
        startActivity(cambiar);
        finish();
        setResult(Ingresar.RESULT_OK);
    }
}

