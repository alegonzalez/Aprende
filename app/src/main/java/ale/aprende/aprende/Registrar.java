package ale.aprende.aprende;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Registrar extends AppCompatActivity {
    //Declaración de variables
    private final int ELEGIR_IMAGEN = 1;
    public RadioButton rbtFemenina, rbtMasculino;
    public ImageView imageView = null;
    public String nombreImagen = "";
    private Bitmap bitmapPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar);
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
    }

    //evento onclick para cargar la foto
    public void cargarFoto(View view) {
        Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
        gallIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(gallIntent, "Seleccione la imagen"), ELEGIR_IMAGEN);
    }

    //Se ejecuta cuando se selecciona la imagen a cargar desde galeria
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ELEGIR_IMAGEN && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String url = obtenerUrlImagen(uri);
            nombreImagen = url.substring(url.lastIndexOf("/") + 1);
            try {
                bitmapPerfil = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageView = (ImageView) findViewById(R.id.imgPerfil);
                imageView.setImageBitmap(bitmapPerfil);

                //detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Este metodo retorna la url de la imagen
    public String obtenerUrlImagen(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    //metodo onclick para registrar
    public void registrar(View view) {
        if (!(validarSeleccionImagen(imageView))) {
            Toast.makeText(this, "Debes seleccionar una foto de perfil del niño", Toast.LENGTH_LONG).show();
            return;
        } else if (!(validarSeleccionGenero(rbtMasculino,rbtFemenina))) {
            Toast.makeText(this, "Debes seleccionar el género", Toast.LENGTH_LONG).show();
            return;
        }
        guardarImagenDispositivo();
        registrarBaseDatos();
    }

    public boolean registrarBaseDatos() {
        String genero = "";
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        genero = (rbtMasculino.isChecked()) ? "M" : "F";
        ContentValues values = new ContentValues();
        values.put("genero", genero);
        values.put("imagen", nombreImagen);
        long id = db.insert("persona", null, values);
        db.close();
        Intent intent = new Intent(Registrar.this, MainActivity.class);
        startActivity(intent);
        return true;
    }

    //guardar la imagen en los archivos de la aplicación del celular
    private boolean guardarImagenDispositivo() {
        File direct = new File(Environment.getExternalStorageDirectory() + "/Aprende");

        if (!direct.exists()) {
            File wallpaperDirectory = new File("/sdcard/Aprende/");
            wallpaperDirectory.mkdirs();
        }

        File file = new File(new File("/sdcard/Aprende/"), nombreImagen);
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

    //valida que se halla seleccionado la imagen del perfil del niño
    public boolean validarSeleccionImagen(ImageView imageView) {
        if (null != imageView.getDrawable()) {
            return true;
        } else {
            return false;
        }
    }

    //valida que se halla seleccionado el genero
    public boolean validarSeleccionGenero(RadioButton rbtMasculino,RadioButton rbtFemenina) {
        if (rbtMasculino.isChecked() || rbtFemenina.isChecked()) {
            return true;
        } else {
            return false;
        }
    }


}
