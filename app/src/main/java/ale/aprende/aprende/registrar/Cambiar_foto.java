package ale.aprende.aprende.registrar;

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

import ale.aprende.aprende.Ingresar.Ingresar;
import ale.aprende.aprende.R;
import ale.aprende.aprende.bd.DBHandler;
import ale.aprende.aprende.helper.ImageHelper;
import ale.aprende.aprende.helper.LogHelper;
import ale.aprende.aprende.helper.SampleApp;
import de.hdodenhof.circleimageview.CircleImageView;

public class Cambiar_foto extends AppCompatActivity {
    RadioButton rbtMasculino, rbtFemenina;
    private final int ELEGIR_IMAGEN = 1;
    private String nombreImagen = "";
    private UUID mFaceId0;
    public Bitmap bitmapPerfil;
    public CircleImageView imageView;
    private Bitmap mBitmap0;
    public String fotoDetectada = "";
    private String nombreBasedatosImagen = "";
    private int idBasedatos = 0;
    private int noEligio = 1;
    private int cambio = 0;
    ProgressDialog progressDialog;
    protected FaceListAdapter mFaceListAdapter0;
    DBHandler mdb;

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
        setContentView(R.layout.activity_cambiar_foto);
        imageView = (CircleImageView) findViewById(R.id.imgPerfil);
        rbtMasculino = (RadioButton) findViewById(R.id.rbtMasculino);
        rbtFemenina = (RadioButton) findViewById(R.id.rbtFemenina);
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
        mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        //db.close();
        obtenerImagen(db);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.progress_dialog_title));
    }

    //Obtiene la imagen del dispositivo y ademas carga los datos del usuario
    public void obtenerImagen(SQLiteDatabase db) {
        String[] resultado;
        String genero = "";
        File file = new File("/sdcard/Aprende/");
        File list[] = file.listFiles();
        if (list.length > 0) {
            Bitmap myBitmap = BitmapFactory.decodeFile(list[0].getAbsolutePath());
            ImageView myImage = (ImageView) findViewById(R.id.imgPerfil);
            myImage.setImageBitmap(myBitmap);
        }
        nombreBasedatosImagen = list[0].getName();
        resultado = list[0].getName().split("\\.");
        String str = new String(resultado[0]);
        str = str.substring(str.length() - 1, str.length());
        Cursor cursor = db.rawQuery("select * from Persona where id = '" + str.trim() + "'", null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String nombre_imagen = cursor.getString(cursor.getColumnIndex("rostro_detectado"));
                genero = cursor.getString(cursor.getColumnIndex("genero"));
                String id = cursor.getString(cursor.getColumnIndex("id"));
                if (file.exists()) {
                    idBasedatos = Integer.parseInt(id);
                    // String rostro = cursor.getString(cursor.getColumnIndex("rostro"));
                    Bitmap rostroBitmap = convertitStringABitmap(nombre_imagen);
                    //ImageView imgagen_Perfil = (ImageView) findViewById(R.id.rostro);
                    //imgagen_Perfil.setImageBitmap(rostroBitmap);
                }
                cursor.moveToNext();
            }

        }
        marcarGenero(genero);


    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Cambiar_foto.this, Ingresar.class);
        startActivity(intent);
        finish();
        super.onBackPressed();

    }

    //Este metodo se encarga de marcar el genero en el radiobutton
    private boolean marcarGenero(String genero) {
        if (genero.trim().equals("M")) {
            rbtMasculino.setChecked(true);
        } else if ((genero.trim().equals("F"))) {
            rbtFemenina.isChecked();
            rbtFemenina.setChecked(true);
        } else {
            return false;
        }
        return true;
    }

    //Este metodo convierte el string que viene de base de datos a Bitmap
    private Bitmap convertitStringABitmap(String imagen) {
        try {
            byte[] encodeByte = Base64.decode(imagen, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    //Metodo onclick para actualizar los datos del usuario
    public void actualizar(View view) {
        String genero = (rbtFemenina.isChecked()) ? "F" : "M";
        if (noEligio == 1) {
            if (!(ActualizarDatosGenero(genero))) {
                Toast.makeText(this, "No se pudo actualizar el genero", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Cambiar_foto.this, Ingresar.class);
                startActivity(intent);
                finish();
            }
        } else {
            File imagenEliminar = new File("/sdcard/Aprende/" + nombreBasedatosImagen);
            if (eliminarArchivo(imagenEliminar)) {
                guardarImagen();
            } else {
                Toast.makeText(this, "La imagen actual de perfil no se pudo eliminar", Toast.LENGTH_SHORT).show();
            }
            if (!(guardarImagen())) {
                Toast.makeText(this, "La imagen no se pudo guardar", Toast.LENGTH_SHORT).show();
            } else if (ActualizarDatos(genero)) {
                Intent intent = new Intent(this, Ingresar.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "La imagen colocada no se pudo guardar correctamente ", Toast.LENGTH_SHORT).show();
            }

        }
    }

    //Elimina la imagen que esta en la ruta especificada
    public boolean eliminarArchivo(File imagenEliminar) {
        return (imagenEliminar.delete()) ? true : false;
    }

    //Guarda la imagen en el dispositivo
    private boolean guardarImagen() {
        String[] resultado;
        resultado = nombreImagen.split("\\.");
        File imagen = new File(new File("/sdcard/Aprende/"), resultado[0] + "_" + idBasedatos + "." + resultado[1]);
        try {
            FileOutputStream out = new FileOutputStream(imagen);
            bitmapPerfil.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //actualizar solamente el genero
    public boolean ActualizarDatosGenero(String genero) {
        mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("genero", genero);
        db.update("Persona", cv, "id=" + idBasedatos, null);
        db.close();
        return true;
    }

    //actualiza los datos en base de datos
    public boolean ActualizarDatos(String genero) {
        if (mFaceId0.toString() != null) {
            mdb = new DBHandler(getApplicationContext());
            SQLiteDatabase db = mdb.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("genero", genero);
            cv.put("imagen", nombreImagen);
            cv.put("rostro", mFaceId0.toString());
            cv.put("rostro_detectado", fotoDetectada.toString());
            db.update("Persona", cv, "id=" + idBasedatos, null);
            db.close();
        }
        return true;
    }

    //evento onclick para cargar la foto
    public void cargarFoto(View view) {
        Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
        gallIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(gallIntent, "Seleccione la imagen"), ELEGIR_IMAGEN);
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
                bitmapPerfil = rotateBitmap(bitmapPerfil, 0);
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
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
    //Este metodo se ejecuta despues de seleccionar la imagen o no elegir la imagen
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ELEGIR_IMAGEN && resultCode == RESULT_OK && data != null && data.getData() != null) {
            noEligio = 0;
            Uri uri = data.getData();
            /*
            Cursor cursor1 = getContentResolver().query(uri, null, null, null, null);
            cursor1.moveToFirst();
            int idx = cursor1.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            String ruta = cursor1.getString(idx);
            cursor1.close();
            */
            File archivoFoto = new File(getPath(this,uri));
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

            // Si la imagen se selecciona correctamente, se configure la URI y la imagen de mapa de bits.
            Bitmap bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                    data.getData(), getContentResolver());
            if (bitmap != null) {
                limpiarRostrosDetectados();
                agregarLog("Imagen" + 0 + ": " + data.getData() + " redimensionado a " + bitmap.getWidth()
                        + "x" + bitmap.getHeight());
            }
            try {
                ExifInterface exif = new ExifInterface(archivoFoto.getAbsolutePath());
                bitmapPerfil = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90);
                bitmapPerfil = orientacionImagen(orientation);
                // ImageView img = (ImageView) findViewById(R.id.rostro);
                //img.setImageBitmap(null);
                //bitmapPerfil = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageView = (CircleImageView) findViewById(R.id.imgPerfil);
                imageView.setImageBitmap(bitmapPerfil);
                mBitmap0 = bitmap;
                detectar(bitmapPerfil, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Borra las caras detectadas indicadas por índice.
    private void limpiarRostrosDetectados() {
        ImageView imageView =
                (ImageView) findViewById(R.id.imgPerfil);
        imageView.setImageResource(android.R.color.transparent);
    }

    // Agregar un elemento .
    private void agregarLog(String log) {
        LogHelper.addVerificationLog(log);
    }

    //Iniciar la detección en la imagen especificada por índice
    public void detectar(Bitmap bitmap, int index) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        // Inicie una tarea de fondo para detectar rostros en la imagen.
        new Cambiar_foto.DetectionTask(index).execute(inputStream);
        establecerInformacion("Detectando rostro...");
    }

    // Establecer el panel de información en la pantalla..
    private void establecerInformacion(String info) {
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
    }

    // Muestra el resultado en la pantalla cuando se realiza la detección en la imagen indicada por índice.
    private void setUiAfterDetection(Face[] result, int index, boolean succeed) {

        if (succeed) {
            agregarLog("Response: Success. Detectado "
                    + result.length + " rostro(s) en imagen" + index);
            establecerInformacion(result.length + " rostro" + (result.length != 1 ? "s" : "") + " detectado");
            // Muestra la lista detallada de las caras detectadas.
            Cambiar_foto.FaceListAdapter faceListAdapter = new Cambiar_foto.FaceListAdapter(result, index);
            // Establezca el ID de cara predeterminado en el ID de la primera cara, si se detectan una o más caras.
            Bitmap bitmap = null;
            if (faceListAdapter.faces.size() != 0) {
                mFaceId0 = faceListAdapter.faces.get(0).faceId;
                //rostroimg = (ImageView) findViewById(R.id.rostro);
                int ancho = faceListAdapter.faceThumbnails.get(0).getWidth();
                int alto = faceListAdapter.faceThumbnails.get(0).getHeight();
                bitmap = bitmapPerfil;
                bitmap = Bitmap.createScaledBitmap(bitmap, alto, ancho, true);
                ;
                if (cambio == 1) {
                    bitmap = rotateBitmap(faceListAdapter.faceThumbnails.get(0), 90);
                } else if (cambio == 2) {
                    bitmap = rotateBitmap(faceListAdapter.faceThumbnails.get(0), 180);
                } else if (cambio == 3 || cambio == 4) {
                    bitmap = rotateBitmap(faceListAdapter.faceThumbnails.get(0), 270);
                }
                // rostroimg.setImageBitmap(bitmap);
                fotoDetectada = convertirBitmapAString(bitmap);
            }
            mFaceListAdapter0 = faceListAdapter;
            mBitmap0 = null;
        }
        if (result != null && result.length == 0) {
            establecerInformacion("No se pudo detectar el rostro, intenta con otra foto");
        }
        if (mBitmap0 == null) {
            progressDialog.dismiss();
        }
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

    // Set the information panel on screen.
    private void setInfo(String info) {
        Toast.makeText(Cambiar_foto.this, info, Toast.LENGTH_SHORT).show();
    }

    //Convierte de bitmap a string
    public String convertirBitmapAString(Bitmap bitmap) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
        return temp;

    }

}