package ale.aprende.aprende;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ale.aprende.aprende.bd.DBHandler;

public class Relaciones_espaciales extends AppCompatActivity {
    //Declaración de variables
    private int id_usuario;
    ImageView img;
    ImageButton opcion1, opcion2, opcion3;
    MediaPlayer pregunta = new MediaPlayer();
    AudioManager amanager;
    String audiogeneral, nombreSubcategoria = "";
    MediaPlayer respuesta = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relaciones_espaciales);
        img = (ImageView) findViewById(R.id.imgPregunta);
        opcion1 = (ImageButton) findViewById(R.id.imgBtnPrimeraOpcion);
        opcion2 = (ImageButton) findViewById(R.id.imgBtnSegundaOpcion);
        opcion3 = (ImageButton) findViewById(R.id.imgBtnTerceraOpcion);
        ocultarBotones();
        id_usuario = getIntent().getExtras().getInt("id_usuario");
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        List<String> subcategoria = obtenerProgreso();
        verificarTipoSubcategoria(subcategoria);
        pregunta.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                pregunta.stop();
                pregunta.release();
                establecerRespuesta(audiogeneral, nombreSubcategoria);
                //obtiene los audios de las respuestas de la pregunta
                String[] respuestaAudio = obtenerAudiosRespuesta("relaciones_espaciales/audios_respuesta_relaciones_espaciales/", audiogeneral, nombreSubcategoria);
                //Reproduce cada unos de los audios de las respuestas
                establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, respuesta);
            }
        });
        respuesta.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                respuesta.release();
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
        //db.close();
        return cursor;
    }

    //Realiza las preguntas segun tema
    public void realizarPreguntas(Cursor cursor, Cursor cursor1, String nombre_subcategoria) {
        String[] nombreImagen = new String[11];
        String[] audio = new String[11];
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
        cursor.close();
        cursor1.close();
        int numero = sortear(nombreImagen.length);
        //Obtiene la imagen de la pregunta
        Drawable d = establecerImagen(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "relaciones_espaciales/"));
        img.setImageDrawable(d);
        reproducirAudio(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "relaciones_espaciales/audios_preguntas_relaciones_espaciales/"), "", "", null);
        audiogeneral = audio[numero];
        nombreSubcategoria = nombre_subcategoria;

    }

    //Esta funcion se detiene por unos segundo para mientra reproduce el audio
    public void esperar() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Reproducción de audios de respuesta
    public void establecerAudiosRespuesta(String[] respuestaAudio, String nombreSubcategoria, MediaPlayer respuesta) {

        if (nombreSubcategoria.trim().equals("derecha") || nombreSubcategoria.trim().equals("izquierda")) {
            String[] resultado = ((String) opcion1.getTag()).split("_");
            String[] nombre = resultado[1].split("\\.");
            if (respuestaAudio[0].equals(nombre[0] + ".mp3")) {
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + nombre[0] + ".mp3", "r", (String) opcion1.getTag(), respuesta);
                respuesta = new MediaPlayer();
                esperar();
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + "izquierda" + ".mp3", "r", (String) opcion3.getTag(), respuesta);
            } else {
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + nombre[0] + ".mp3", "r", (String) opcion3.getTag(), respuesta);
                respuesta = new MediaPlayer();
                esperar();
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + "derecha" + ".mp3", "r", (String) opcion1.getTag(), respuesta);
            }
        }
        for (int i = 0; i < respuestaAudio.length; i++) {
            String numeroRespuesta = respuestaAudio[i].substring(0,2);
            if (((String) opcion1.getTag()).trim().equals(respuestaAudio[i].trim())) {
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/"+numeroRespuesta+ "/" + respuestaAudio[i], "r", (String) opcion1.getTag(), respuesta);
                respuesta = new MediaPlayer();
                esperar();
            } else if (((String) opcion2.getTag()).trim().equals(respuestaAudio[i].trim())) {
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria+ "/"+numeroRespuesta+ "/" + respuestaAudio[i], "r", (String) opcion2.getTag(), respuesta);
                respuesta = new MediaPlayer();
                esperar();
            } else {
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/"+numeroRespuesta+ "/" + respuestaAudio[i], "r", (String) opcion3.getTag(), respuesta);
                respuesta = new MediaPlayer();
                esperar();
            }
        }


    }

    //Este metodo se encarga de reproducir los audios de las respuestas
    public String[] obtenerAudiosRespuesta(String tema, String audio, String nombreSubcategoria) {
        String direccion = "";
        if (nombreSubcategoria.equals("derecha") || nombreSubcategoria.equals("izquierda")) {
            direccion = tema + nombreSubcategoria;
        } else {
            direccion = tema + nombreSubcategoria + "/" + "r" + audio.substring(1);
        }

        AssetManager assetManager = getApplicationContext().getAssets();
        String[] respuestas = new String[0];
        try {
            respuestas = assetManager.list(direccion);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return respuestas;
    }

    //Establece las posibles respuestas de la pregunta
    public void establecerRespuesta(String audio, String nombreSubcategoria) {
        String[] lista = new String[0];
        String[] respuestas = new String[3];
        String direccion = "relaciones_espaciales/" + nombreSubcategoria + "/" + "r" + audio.substring(1);
        lista = obtenerImagenRespuestas(direccion, "respuesta");
        if (nombreSubcategoria.equals("derecha") || nombreSubcategoria.equals("izquierda")) {
            respuestas = obtenerNumeros(1);
            Drawable imagenpt1 = establecerImagen(direccion + "/" + lista[Integer.parseInt(respuestas[0])]);
            imagenpt1 = establecertamaño(imagenpt1);
            opcion1.setTag(lista[Integer.parseInt(respuestas[0])]);
            opcion1.setImageDrawable(imagenpt1);
            opcion2.setTag("");
            Drawable imagenpt3 = establecerImagen(direccion + "/" + lista[Integer.parseInt(respuestas[1])]);
            imagenpt3 = establecertamaño(imagenpt3);
            opcion3.setImageDrawable(imagenpt3);
            opcion3.setTag(lista[Integer.parseInt(respuestas[1])]);
            mostrarBotones();
        } else {
            respuestas = obtenerNumeros(2);
            Drawable imagenpt1 = establecerImagen(direccion + "/" + lista[Integer.parseInt(respuestas[0])]);
            imagenpt1 = establecertamaño(imagenpt1);
            opcion1.setImageDrawable(imagenpt1);
            opcion1.setTag(lista[Integer.parseInt(respuestas[0])]);
            Drawable imagenpt2 = establecerImagen(direccion + "/" + lista[Integer.parseInt(respuestas[1])]);
            imagenpt2 = establecertamaño(imagenpt2);
            opcion2.setImageDrawable(imagenpt2);
            opcion2.setTag(lista[Integer.parseInt(respuestas[1])]);
            Drawable imagenpt3 = establecerImagen(direccion + "/" + lista[Integer.parseInt(respuestas[2])]);
            imagenpt3 = establecertamaño(imagenpt3);
            opcion3.setImageDrawable(imagenpt3);
            opcion3.setTag(lista[Integer.parseInt(respuestas[2])]);
            mostrarBotones();
        }

    }

    //colocar tamaño a las imagenes
    public Drawable establecertamaño(Drawable imagen) {
        Bitmap bitmap = ((BitmapDrawable) imagen).getBitmap();
        Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 125, 125, true));
        return d;
    }

    //Realiza random sin repetir la combinación de números
    public String[] obtenerNumeros(int numero) {
        String[] listaNumeros = new String[3];
        ArrayList<Integer> list = new ArrayList<Integer>(numero);
        for (int i = 0; i <= numero; i++) {
            list.add(i);
        }

        Random rand = new Random();
        int contador = 0;
        while (list.size() > 0) {
            int index = rand.nextInt(list.size());
            listaNumeros[contador] = list.remove(index).toString();
            contador++;
        }
        return listaNumeros;
    }

    //Esta imagen obtiene los archivos que pertenece a las respuestas de las preguntas
    public String[] obtenerImagenRespuestas(String direccion, String tipo) {
        AssetManager assetManager = getApplicationContext().getAssets();
        String[] archivos = new String[0];
        try {
            archivos = (tipo.equals("respuesta")) ? assetManager.list(direccion) : assetManager.list(direccion);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return archivos;
    }

    //Reproduce el audio de la pregunta de la imagen
    public void reproducirAudio(String audio, String tipo, String nombreImagenBoton, MediaPlayer respuesta) {
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        amanager.setMode(AudioManager.STREAM_MUSIC);
        if (respuesta == null) {
            pregunta = new MediaPlayer();
            try {
                AssetFileDescriptor afd = getApplicationContext().getAssets().openFd(audio.trim());
                pregunta.setDataSource(
                        afd.getFileDescriptor(),
                        afd.getStartOffset(),
                        afd.getLength()
                );
                afd.close();
                pregunta.prepare();
                pregunta.setVolume(1, 1);
                pregunta.start();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else {

            try {
                AssetFileDescriptor recurso = getApplicationContext().getAssets().openFd(audio.trim());
                respuesta.setDataSource(
                        recurso.getFileDescriptor(),
                        recurso.getStartOffset(),
                        recurso.getLength()
                );

                recurso.close();
                respuesta.prepare();
                respuesta.setVolume(1, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tipo.equals("r") && ((String) opcion1.getTag()).equals(nombreImagenBoton)) {
                respuesta.start();
            } else if (tipo.equals("r") && ((String) opcion2.getTag()).equals(nombreImagenBoton)) {
                respuesta.start();
            } else if (tipo.equals("r") && ((String) opcion3.getTag()).equals(nombreImagenBoton)) {
                respuesta.start();
            }

        }
    }


    //Este metodo estable en el image view la imagen
    private Drawable establecerImagen(String nombreImagen) {
        Drawable d = null;
        try {
            InputStream ims = getAssets().open(nombreImagen);
            d = Drawable.createFromStream(ims, null);
            ims.close();
        } catch (IOException ex) {

        }
        return d;
    }

    public String obtenerArchivoPregunta(String nombreImagen, String audio, String nombreSubcategoria, String direccion) {
        AssetManager assetManager = getApplicationContext().getAssets();
        String direccionImagen = "";
        try {
            String[] archivos = new String[1];
            if (direccion.equals("relaciones_espaciales/")) {
                direccionImagen = direccion + nombreSubcategoria + "/" + audio;
                archivos = assetManager.list(direccion + nombreSubcategoria + "/" + audio);
                direccionImagen += "/";
                direccionImagen += archivos[0];

            } else {
                direccionImagen = direccion + nombreSubcategoria + "/" + audio + ".mp4";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return direccionImagen;
    }

    //Mediante este metodo obtiene la pregunta a mostrar
    public int sortear(int cantidad_preguntas) {
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
            Cursor cursor1 = obtenerTablaPersona_pregunta(db);
            realizarPreguntas(cursor, cursor1, "abajo");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Adelante")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            Cursor cursor1 = obtenerTablaPersona_pregunta(db);
            realizarPreguntas(cursor, cursor1, "adelante");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Arriba")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            Cursor cursor1 = obtenerTablaPersona_pregunta(db);
            realizarPreguntas(cursor, cursor1, "arriba");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Atras")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            Cursor cursor1 = obtenerTablaPersona_pregunta(db);
            realizarPreguntas(cursor, cursor1, "atras");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Centro")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            Cursor cursor1 = obtenerTablaPersona_pregunta(db);
            realizarPreguntas(cursor, cursor1, "centro");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Derecha")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            Cursor cursor1 = obtenerTablaPersona_pregunta(db);
            realizarPreguntas(cursor, cursor1, "derecha");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Izquierda")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            Cursor cursor1 = obtenerTablaPersona_pregunta(db);
            realizarPreguntas(cursor, cursor1, "izquierda");
            cursor.close();
            cursor1.close();
        }
        db.close();
    }

    //Este metodo obtiene la información de la tabla persona_pregunta
    public Cursor obtenerTablaPersona_pregunta(SQLiteDatabase db) {
        Cursor cursor1 = db.rawQuery("select id_pregunta,estado from  Persona_Pregunta  " +
                " where id_persona = " + id_usuario, null);
        return cursor1;
    }

    //Evento si se realiza un retroceso desde el dispositivo
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Relaciones_espaciales.this, MenuJuego.class);
        startActivity(intent);
        finish();
    }

    //Este metodo se encarga de ocultar los botones
    public void ocultarBotones() {
        opcion1.setVisibility(View.INVISIBLE);
        opcion2.setVisibility(View.INVISIBLE);
        opcion3.setVisibility(View.INVISIBLE);
    }

    //Muestra los botones de la respuesta
    public void mostrarBotones() {
        if (nombreSubcategoria.equals("derecha") || nombreSubcategoria.equals("izquierda")) {
            opcion1.setVisibility(View.VISIBLE);
            opcion3.setVisibility(View.VISIBLE);
        } else {
            opcion2.setVisibility(View.VISIBLE);
            opcion1.setVisibility(View.VISIBLE);
            opcion3.setVisibility(View.VISIBLE);
        }
        ObjectAnimator opt1 = ObjectAnimator.ofFloat(opcion1, "alpha", .3f, 1f);
        ObjectAnimator opt2 = ObjectAnimator.ofFloat(opcion2, "alpha", .3f, 1f);
        ObjectAnimator opt3 = ObjectAnimator.ofFloat(opcion3, "alpha", .3f, 1f);
        opt1.setDuration(1000);
        opt2.setDuration(1000);
        opt3.setDuration(1000);
    }
}
