package ale.aprende.aprende.registrar;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import ale.aprende.aprende.bd.DBHandler;
import ale.aprende.aprende.helper.ImageHelper;
import ale.aprende.aprende.helper.LogHelper;
import ale.aprende.aprende.helper.SampleApp;
import ale.aprende.aprende.principal.MainActivity;
import ale.aprende.aprende.R;

public class Registrar extends AppCompatActivity {
    //Declaración de variables
    private final int ELEGIR_IMAGEN = 1;
    public RadioButton rbtFemenina, rbtMasculino;
    public ImageView imageView, rostroimg = null;
    public String nombreImagen = "";
    private Bitmap bitmapPerfil;
    private String fotoDetectada = "";
    ProgressDialog progressDialog;
    private UUID mFaceId0;
    private int cambio = 0;
    protected FaceListAdapter mFaceListAdapter0;
    private Bitmap mBitmap0;
    private int detectado = 0;

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
                publishProgress("Detectando Rostro");

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
                agregarLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            //agregarLog("Request: Detecting in image" + mIndex);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
            establecerInformacion(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            // Show the result on screen when detection is done.
            setUiAfterDetection(result, mIndex, mSucceed);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar);
        int contador = 0;
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from persona", null);
        String[] resultado;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String nombre_imagen = cursor.getString(cursor.getColumnIndex("imagen"));
                resultado = nombre_imagen.split("\\.");
                String id = cursor.getString(cursor.getColumnIndex("id"));
                File file = new File(new File("/sdcard/Aprende/"), resultado[0] + "_" + id + "." + resultado[1]);
                if (file.exists()) {
                    contador++;
                }
                cursor.moveToNext();
            }
        }
        db.close();
        if (contador == 0) {
            rbtFemenina = (RadioButton) findViewById(R.id.rbtFemenina);
            rbtMasculino = (RadioButton) findViewById(R.id.rbtMasculino);
            imageView = (ImageView) findViewById(R.id.imgPerfil);
            //evento del radio button en caso que eliga como genero niña
            rbtFemenina.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked && rbtFemenina.isChecked()) {
                        rbtMasculino.setChecked(false);
                    }
                }
            });
            //evento del radio button en caso que eliga como genero niño
            rbtMasculino.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked && rbtMasculino.isChecked()) {
                        rbtFemenina.setChecked(false);
                    }
                }
            });
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.progress_dialog_title));
        } else {
            regresar("Ya existe una foto registrada");
        }

    }

    //Este metodo regresa a la actividad principal
    public void regresar(String mensaje) {
        Intent intent = new Intent(Registrar.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    //evento onclick para cargar la foto
    public void cargarFoto(View view) {
        Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
        gallIntent.setType("image/*");

        startActivityForResult(Intent.createChooser(gallIntent, "Seleccione la imagen"), ELEGIR_IMAGEN);
    }
    //Obtener
    public String obtenerDireccionImagen(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    //Se ejecuta cuando se selecciona la imagen a cargar desde galeria
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ELEGIR_IMAGEN && resultCode == RESULT_OK && data != null && data.getData() != null) {
            File finalFile = new File(obtenerDireccionImagen(data.getData()));
            Uri uri = data.getData();
            nombreImagen = obtenerNombreImagen(uri);
            cargarImagen(data, uri, finalFile);

        }
    }

    //Este metodo se encarga de mostrar la imagen del usuario
    public void cargarImagen(Intent datos, Uri uri, File archivo) {
        // If image is selected successfully, set the image URI and bitmap.
        Bitmap bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                datos.getData(), getContentResolver());
        if (bitmap != null) {
            limpiarRostrosDetectados();
            // Add verification log.
            agregarLog("Imagen" + 0 + ": " + datos.getData() + " redimensionado a " + bitmap.getWidth()
                    + "x" + bitmap.getHeight());
        }
        try {
           // ImageView img = (ImageView) findViewById(R.id.rostro);
            //img.setImageBitmap(null);
            ExifInterface exif = new ExifInterface(archivo.getAbsolutePath());
            bitmapPerfil = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

            bitmapPerfil = orientacionImagen(orientation);//ExifInterface.ORIENTATION_NORMAL
            imageView = (ImageView) findViewById(R.id.imgPerfil);
            imageView.setImageBitmap(bitmapPerfil);
            mBitmap0 = bitmap;
            detectar(bitmapPerfil, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Se encarga de verifica la orientacion que tiene la imagen al ser cargada
    public Bitmap orientacionImagen(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                bitmapPerfil = rotateBitmap(bitmapPerfil, 90);
                cambio = 1;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                bitmapPerfil = rotateBitmap(bitmapPerfil, 180);
                cambio = 2;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                bitmapPerfil = rotateBitmap(bitmapPerfil, 270);
                cambio = 3;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                bitmapPerfil = rotateBitmap(bitmapPerfil, 270);
                cambio = 4;
            default:
                break;
        }
        return bitmapPerfil;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public String obtenerNombreImagen(Uri uri) {
        if (uri.getScheme().equals("file")) {
            nombreImagen = uri.getLastPathSegment();
        } else {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, new String[]{
                        MediaStore.Images.ImageColumns.DISPLAY_NAME
                }, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    nombreImagen = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
                }
            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return nombreImagen;
    }

    //Iniciar la detección en la imagen especificada por índice
    public void detectar(Bitmap bitmap, int index) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        // Inicie una tarea de fondo para detectar rostros en la imagen.
        new DetectionTask(index).execute(inputStream);
        establecerInformacion("Detectando rostro...");
    }

    // Establecer el panel de información en la pantalla..
    public boolean establecerInformacion(String info) {
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
        return true;
    }

    // Borra las caras detectadas indicadas por índice.
    public boolean limpiarRostrosDetectados() {
        ImageView imageView =
                (ImageView) findViewById(R.id.imgPerfil);
        imageView.setImageResource(android.R.color.transparent);
        return true;
    }

    // Agregar un elemento .
    private void agregarLog(String log) {
        LogHelper.addVerificationLog(log);
    }

    //metodo onclick para registrar
    public void registrar(View view) {
        Button btn = (Button) findViewById(R.id.btnRegistrar);
        btn.setEnabled(false);
        if (detectado == 0) {
            Toast.makeText(this, "Debes seleccionar una foto de perfil del niño", Toast.LENGTH_LONG).show();
            btn.setEnabled(true);
            return;
        } else if (!(validarSeleccionGenero(rbtMasculino, rbtFemenina))) {
            Toast.makeText(this, "Debes seleccionar el género", Toast.LENGTH_LONG).show();
            btn.setEnabled(true);
            return;
        }
        String id = registrarBaseDatos();
        guardarImagenDispositivo(id);
        Intent intent = new Intent(Registrar.this, MainActivity.class);
        btn.setEnabled(true);
        startActivity(intent);
    }

    //Insertar en la base de datos
    public String registrarBaseDatos() {
        String genero = "";
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        genero = (rbtMasculino.isChecked()) ? "M" : "F";
        ContentValues values = new ContentValues();
        values.put("genero", genero);
        values.put("imagen", nombreImagen);
        values.put("rostro", mFaceId0.toString());
        values.put("rostro_detectado", fotoDetectada.toString());
        long id = db.insert("persona", null, values);
        db.close();
        return Long.toString(id);
    }

    //guardar la imagen en los archivos de la aplicación del celular
    public boolean guardarImagenDispositivo(String id) {
        File direct = new File(Environment.getExternalStorageDirectory() + "/Aprende");
        if (!direct.exists()) {
            File wallpaperDirectory = new File("/sdcard/Aprende/");
            wallpaperDirectory.mkdirs();
        }
        String[] resultado;
        resultado = nombreImagen.split("\\.");
        File file = new File(new File("/sdcard/Aprende/"), resultado[0] + "_" + id + "." + resultado[1]);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmapPerfil.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //valida que se halla seleccionado el genero
    public boolean validarSeleccionGenero(RadioButton rbtMasculino, RadioButton rbtFemenina) {
        if (rbtMasculino.isChecked() || rbtFemenina.isChecked()) {
            return true;
        } else {
            return false;
        }
    }

    // Muestra el resultado en la pantalla cuando se realiza la detección en la imagen indicada por índice.
    private void setUiAfterDetection(Face[] result, int index, boolean succeed) {

        if (succeed) {
            detectado = 1;
            agregarLog("Response: Success. Detectado "
                    + result.length + " rostro(s) en imagen" + index);
            establecerInformacion(result.length + " rostro" + (result.length != 1 ? "s" : "") + " detectado");
            // Muestra la lista detallada de las caras detectadas.
            FaceListAdapter faceListAdapter = new FaceListAdapter(result, index);
            // Establezca el ID de cara predeterminado en el ID de la primera cara, si se detectan una o más caras.
            Bitmap bitmap = null;
            if (faceListAdapter.faces.size() != 0) {
                mFaceId0 = faceListAdapter.faces.get(0).faceId;
              //  rostroimg = (ImageView) findViewById(R.id.rostro);
                if (cambio == 1) {
                    bitmap = rotateBitmap(faceListAdapter.faceThumbnails.get(0), 90);
                } else if (cambio == 2) {
                    bitmap = rotateBitmap(faceListAdapter.faceThumbnails.get(0), 180);
                } else if (cambio == 3 || cambio == 4) {
                    bitmap = rotateBitmap(faceListAdapter.faceThumbnails.get(0), 270);
                }
                //rostroimg.setImageBitmap(bitmap);
                fotoDetectada = convertirBitmapAString(bitmap);
            }
            mFaceListAdapter0 = faceListAdapter;
            mBitmap0 = null;
        }else{
            detectado = 0;
        }
        if (result != null && result.length == 0) {
            establecerInformacion("No se pudo detectar el rostro, intenta con otra foto");
        }
        if (mBitmap0 == null) {
            progressDialog.dismiss();
        }
    }

    //Convierte de bitmap a string
    public String convertirBitmapAString(Bitmap bitmap) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
        return temp;

    }

    // El adaptador del GridView que contiene las miniaturas de las caras detectadas.
    private class FaceListAdapter extends BaseAdapter {
        List<Face> faces;
        int mIndex;
        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        // Inicializar con el resultado de la detección y el índice que indica en qué imagen se obtiene el resultado.
        FaceListAdapter(Face[] detectionResult, int index) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            mIndex = 0;
            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    try {
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(mBitmap0, face.faceRectangle));
                    } catch (IOException e) {
                        setInfo(e.getMessage());
                    }
                }
            }
        }


        // Set the information panel on screen.
        private void setInfo(String info) {
            Toast.makeText(Registrar.this, info, Toast.LENGTH_SHORT).show();
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
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face, parent, false);
            }
            convertView.setId(position);

            Bitmap thumbnailToShow = faceThumbnails.get(position);
            if (mIndex == 0 && faces.get(position).faceId.equals(mFaceId0)) {
                thumbnailToShow = ImageHelper.highlightSelectedFaceThumbnail(thumbnailToShow);
            }

            // Show the face thumbnail.
            ((ImageView) convertView.findViewById(R.id.image_face)).setImageBitmap(thumbnailToShow);
            return convertView;
        }
    }
}
