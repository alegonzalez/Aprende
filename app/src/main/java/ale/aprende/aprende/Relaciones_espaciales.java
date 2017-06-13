package ale.aprende.aprende;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ale.aprende.aprende.bd.DBHandler;

public class Relaciones_espaciales extends AppCompatActivity {
    //Declaración de variables
    private int id_usuario;
    ImageView img;
    ImageButton opcion1, opcion2, opcion3;
    MediaPlayer m = new MediaPlayer();
    AudioManager amanager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relaciones_espaciales);
        img = (ImageView) findViewById(R.id.imgPregunta);
        opcion1 = (ImageButton) findViewById(R.id.imgBtnPrimeraOpcion);
        opcion2 = (ImageButton) findViewById(R.id.imgBtnSegundaOpcion);
        opcion3 = (ImageButton) findViewById(R.id.imgBtnTerceraOpcion);
        id_usuario = getIntent().getExtras().getInt("id_usuario");
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        List<String> subcategoria = obtenerProgreso();
        verificarTipoSubcategoria(subcategoria);
        m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        });
    }

    // Obtiene el progreso acerca del tema de las preguntas que no se han realizado
    public List obtenerProgreso() {
        List<String> subcategoria = new ArrayList<String>();
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select p.id_subcategoria,p.cantidad_preguntas,sub.nombre " +
                " from Progreso p, SubCategoria sub " +
                "where p.id_subcategoria = sub.id and " +
                " p.estado= " + 0, null);
        if (cursor.moveToFirst()) {
            subcategoria.add(cursor.getString(cursor.getColumnIndex("id_subcategoria")));
            subcategoria.add(cursor.getString(cursor.getColumnIndex("nombre")));
        }
        db.close();
        return subcategoria;
    }

    //Verifica las preguntas realizada segun la subcategoria
    private Cursor obtenerPreguntasRealizadas(int id_subcategoria) {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select id,nombre_imagen,audio from  Pregunta " +
                " where id_subcategoria= " + id_subcategoria, null);
        return cursor;
    }


    //Realiza las preguntas segun tema
    public void realizarPreguntas(Cursor cursor, Cursor cursor1, String nombre_subcategoria) {
        String[] nombreImagen = new String[11];
        String[] audio = new String[11];
        Toast.makeText(this, "ENTROOOOOOO", Toast.LENGTH_SHORT).show();
        int contador = 0;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String id = cursor.getString(cursor.getColumnIndex("id"));
                String id_pregunta = "";
                String estado = "0";
                if (cursor1.getCount() > 0) {
                    id_pregunta = cursor1.getString(cursor1.getColumnIndex("id_pregunta"));
                    estado = cursor1.getString(cursor1.getColumnIndex("estado"));
                    if (id.equals(id_pregunta) && estado.equals("0")) {
                        nombreImagen[contador] = cursor.getString(cursor.getColumnIndex("nombre_imagen"));
                        audio[contador] = cursor.getString(cursor.getColumnIndex("audio"));
                        contador++;
                    }
                } else {
                    nombreImagen[contador] = cursor.getString(cursor.getColumnIndex("nombre_imagen"));
                    audio[contador] = cursor.getString(cursor.getColumnIndex("audio"));
                    contador++;
                }

                cursor.moveToNext();
                cursor1.moveToNext();
            }

        }

        int numero = sortearPregunta(nombreImagen.length);
        //Obtiene la imagen de la pregunta
        establecerImagen(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "relaciones_espaciales/"));
        reproducirAudio(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "relaciones_espaciales/audios_preguntas_relaciones_espaciales/"));

        //ArrayUtils.remove(test, 2);
    }

    //Reproduce el audio de la pregunta de la imagen
    public void reproducirAudio(String audio) {
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        try {

            AssetFileDescriptor afd = getApplicationContext().getAssets().openFd(audio);
            m.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength()
            );
            afd.close();
            m.prepare();
            m.setVolume(1, 1);
            m.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
    }

    //Este metodo estable en el image view la imagen
    private void establecerImagen(String nombreImagen) {
        try {
            InputStream ims = getAssets().open(nombreImagen);
            Drawable d = Drawable.createFromStream(ims, null);
            img.setImageDrawable(d);
            ims.close();
        } catch (IOException ex) {
            return;
        }

    }

    public String obtenerArchivoPregunta(String nombreImagen, String audio, String nombreSubcategoria, String direccion) {
        AssetManager assetManager = getApplicationContext().getAssets();
        String[] archivos;
        String direccionImagen = "";
        try {
            if (direccion.equals("relaciones_espaciales/")) {
                direccionImagen = direccion + nombreSubcategoria + "/" + audio;
                archivos = assetManager.list(direccion + nombreSubcategoria + "/" + audio);
                direccionImagen += "/" + archivos[0];
            } else {
                direccionImagen = direccion + nombreSubcategoria + "/" + audio + ".mp4";
                //archivos = assetManager.list(direccion + nombreSubcategoria + "/" + audio);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return direccionImagen;
    }

    //Mediante este metodo obtiene la pregunta a mostrar
    public int sortearPregunta(int cantidad_preguntas) {
        Random aleatorio = new Random(System.currentTimeMillis());
        // Producir nuevo int aleatorio entre 0 y 99
        return aleatorio.nextInt(cantidad_preguntas);
    }

    //Verificar el tipo de subcategoria
    public void verificarTipoSubcategoria(List<String> subcategoria) {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        if ((subcategoria.get(1)).trim().equals("Abajo")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            Cursor cursor1 = db.rawQuery("select id_pregunta,estado from  Persona_Pregunta  " +
                    " where id_persona = " + id_usuario, null);
            realizarPreguntas(cursor, cursor1, "abajo");
        } else if ((subcategoria.get(1)).trim().equals("Adelante")) {

        } else if ((subcategoria.get(1)).trim().equals("Arriba")) {

        } else if ((subcategoria.get(1)).trim().equals("Atras")) {

        } else if ((subcategoria.get(1)).trim().equals("Centro")) {

        } else if ((subcategoria.get(1)).trim().equals("Derecha")) {

        } else if ((subcategoria.get(1)).trim().equals("Izquierda")) {

        }
    }


    //Evento si se realiza un retroceso desde el dispositivo
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Relaciones_espaciales.this, MenuJuego.class);
        startActivity(intent);
        super.onBackPressed();
    }
}
