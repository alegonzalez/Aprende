package ale.aprende.aprende;

import android.animation.ObjectAnimator;
import android.content.ContentValues;
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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import org.apache.commons.lang3.ArrayUtils;

import ale.aprende.aprende.bd.DBHandler;

public class Relaciones_espaciales extends AppCompatActivity implements RecognitionListener {
    //Declaración de variables
    private int id_usuario;
    ImageView img;
    ImageButton opcion1, opcion2, opcion3;
    MediaPlayer pregunta = new MediaPlayer();
    AudioManager amanager;
    String id_subcategoria, audiogeneral, nombreSubcategoria = "";
    MediaPlayer respuesta = new MediaPlayer();
    private String id_pregunta;
    int pausa = 0;
    public SpeechRecognizer speech;
    private Intent recognizerIntent;

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
                respuesta.stop();
                respuesta.release();
                pausa = 2;
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        });
    }

    //Evento click del boton 1
    public void opcion1(View view) {
        verificarRespuesta((String) opcion1.getTag());
    }

    //Evento click del boton 2
    public void opcion2(View view) {
        verificarRespuesta((String) opcion2.getTag());
    }

    //Evento click del boton 3
    public void opcion3(View view) {
        verificarRespuesta((String) opcion3.getTag());
    }

    //Este metodo verifica si la respuesta es correcta
    private void verificarRespuesta(String direccion) {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        String resultado[] = direccion.split("\\.");
        String respuesta = resultado[0].substring(resultado[0].length() - 1);
        Cursor cursor = db.rawQuery("select cantidad_preguntas,cantidad_errores " + " from Progreso " +
                " where id_subcategoria = " + id_subcategoria + " and " + " id_persona= " + id_usuario, null);
        int cantidad_preguntas = 0;
        int cantidad_errores = 0;
        if (cursor.moveToFirst()) {
            cantidad_preguntas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("cantidad_preguntas")));
            cantidad_errores = Integer.parseInt(cursor.getString(cursor.getColumnIndex("cantidad_errores")));
        }
        if (respuesta.equals("v")) {
            ContentValues values = new ContentValues();
            values.put("id_persona", id_usuario);
            values.put("id_pregunta", id_pregunta);
            values.put("estado", true);
            db.insert("Persona_Pregunta", null, values);
            ocultarBotones();
            if (nombreSubcategoria.equals("derecha") || nombreSubcategoria.equals("izquierda")) {
                List<String> subcategoria = new ArrayList<>();
                subcategoria.add(id_subcategoria);
                subcategoria.add(nombreSubcategoria.substring(0, 1).toUpperCase() + nombreSubcategoria.substring(1));
                cantidad_preguntas -= 1;
                if (cantidad_preguntas == 0) {
                    if (cantidad_errores == 3) {
                        cantidad_preguntas = 3;
                        ponerErroresEstadisticas(cantidad_errores, 3, db);
                        cantidad_errores = 0;
                        abrirRelacionesEspaciales();
                    } else if (cantidad_errores == 2) {
                        cantidad_preguntas = 2;
                        ponerErroresEstadisticas(cantidad_errores, 2, db);
                        cantidad_errores = 0;
                        abrirRelacionesEspaciales();
                    } else if (cantidad_errores == 1) {
                        cantidad_preguntas = 1;
                        ponerErroresEstadisticas(cantidad_errores, 1, db);
                        cantidad_errores = 0;
                        abrirRelacionesEspaciales();
                    } else {
                        //Tema superado
                        actualizarEstadoProgreso(db);
                        ponerErroresEstadisticas(cantidad_errores, 0, db);
                        int rs = obtenerSiguienteSubctegoria(db);
                        insertarNuevaSubCategoria(rs, db);
                        Toast.makeText(this, "Felicidades tema superado", Toast.LENGTH_SHORT).show();
                        abrirRelacionesEspaciales();
                    }
                    actualizarProgreso(cantidad_preguntas, cantidad_errores, db);
                } else {
                    actualizarProgreso(cantidad_preguntas, cantidad_errores, db);
                    abrirRelacionesEspaciales();
                }
            } else {
                verificarErrores(cursor, db, cantidad_preguntas, cantidad_errores);
                cursor.close();
            }
        } else {
            //Reproducir audio para motivar al niño
            cantidad_errores += 1;
            actualizarProgreso(cantidad_preguntas, cantidad_errores, db);
            Toast.makeText(this, "Vamos intentalo nuevamente", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        db.close();
    }

    //Actualiza el progreso de las preguntas y cantidad de errores
    private void actualizarProgreso(int cantidad_preguntas, int cantidad_errores, SQLiteDatabase db) {
        String strSQL = "UPDATE Progreso SET cantidad_preguntas = " + cantidad_preguntas + ",cantidad_errores = " + cantidad_errores + " WHERE id_persona = "
                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria + " and estado= 0";
        db.execSQL(strSQL);
    }

    // segun el porcentaje que traiga se verifa para continuar con la cantidad de preguntas a realizar
    public void verificarErrores(Cursor cursor, SQLiteDatabase db, int cantidad_preguntas, int cantidad_errores) {
        if (cursor.moveToFirst()) {
            cantidad_preguntas -= 1;
            actualizarCantidadPreguntas(db, cantidad_preguntas);
            List<String> subcategoria = new ArrayList<>();
            subcategoria.add(id_subcategoria);
            subcategoria.add(nombreSubcategoria.substring(0, 1).toUpperCase() + nombreSubcategoria.substring(1));
            if (cantidad_preguntas <= 0) {
                if (cantidad_errores == 6 || cantidad_errores == 5) {
                    cantidad_preguntas = 3;
                    ponerErroresEstadisticas(cantidad_errores, 3, db);
                    cantidad_errores = 0;
                    abrirRelacionesEspaciales();
                } else if (cantidad_errores == 4 || cantidad_errores == 3) {
                    cantidad_preguntas = 2;
                    ponerErroresEstadisticas(cantidad_errores, 2, db);
                    cantidad_errores = 0;
                    abrirRelacionesEspaciales();
                } else if (cantidad_errores == 2 || cantidad_errores == 1) {
                    cantidad_preguntas = 1;
                    ponerErroresEstadisticas(cantidad_errores, 1, db);
                    cantidad_errores = 0;
                    abrirRelacionesEspaciales();
                } else {
                    //Excelente paso  la subcategoria
                    actualizarEstadoProgreso(db);
                    int resultado = obtenerSiguienteSubctegoria(db);
                    ponerErroresEstadisticas(cantidad_errores, 0, db);
                    if (resultado != 0) {
                        insertarNuevaSubCategoria(resultado, db);
                        abrirRelacionesEspaciales();
                        Toast.makeText(this, "Felicidades pequeño sigue asi", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intento = new Intent(Relaciones_espaciales.this, MenuJuego.class);
                        startActivity(intento);
                        finish();
                    }
                }
            } else {
                abrirRelacionesEspaciales();
            }
            actualizarProgreso(cantidad_preguntas, cantidad_errores, db);
        }
    }
    //Actualizar en la tabla de estadistica la cantidad de errores y preguntas que le pertenece a una subcategoria
    public void ponerErroresEstadisticas(int cantidad_errores, int cantidad_preguntas, SQLiteDatabase db) {
        Cursor estadistica = db.rawQuery("select cantidad_errores,cantidad_preguntas " + " from Estadistica " +
                " where id = " + id_subcategoria + " and " + " id_persona= " + id_usuario, null);
        if (estadistica.getCount() > 0 && estadistica.moveToFirst()) {
            cantidad_preguntas += Integer.parseInt(estadistica.getString(estadistica.getColumnIndex("cantidad_preguntas")));
            cantidad_errores += Integer.parseInt(estadistica.getString(estadistica.getColumnIndex("cantidad_errores")));
        }
        String strSQL = "UPDATE Estadistica SET cantidad_errores = " + cantidad_errores + ",cantidad_preguntas = " + cantidad_preguntas + " WHERE id_persona = "
                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
        db.execSQL(strSQL);
    }

    //Inserta en la tabla de progreso la nueva subcategoria
    public void insertarNuevaSubCategoria(int id_subcategoria, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("id_persona", id_usuario);
        values.put("id_subcategoria", id_subcategoria);
        values.put("estado", false);
        values.put("cantidad_preguntas", 3);
        values.put("cantidad_errores", 0);
        db.insert("Progreso", null, values);
    }

    //Este método se encarga de obtener la siguiente subcategoria
    public int obtenerSiguienteSubctegoria(SQLiteDatabase db) {
        String[] listaDisponible = new String[]{"1", "2", "3", "4", "5", "6", "7"};
        Cursor subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                " where id_subcategoria <= " + 7 + " and " + " id_persona= " + id_usuario, null);
        if (subcategoriasProgreso.getCount() > 0) {
            if (subcategoriasProgreso.moveToFirst()) {
                while (!subcategoriasProgreso.isAfterLast()) {
                    for (int i = 0; i < listaDisponible.length; i++) {
                        String id = subcategoriasProgreso.getString(subcategoriasProgreso.getColumnIndex("id_subcategoria"));
                        if (id.trim().equals(listaDisponible[i].trim())) {
                            listaDisponible[i] = "";
                        }
                    }
                    subcategoriasProgreso.moveToNext();
                }
            }
        }
        int cantidad = verificarCantidadArreglo(listaDisponible);
        listaDisponible = eliminarValoresArreglo(listaDisponible, cantidad);
        cantidad = verificarCantidadArreglo(listaDisponible);
        subcategoriasProgreso.close();
        if (cantidad == 0) {
            Toast.makeText(this, "Abrir otro tema", Toast.LENGTH_SHORT).show();
            return 0;
        } else {
            return realizarSigueteSubcategoria(listaDisponible);
        }
    }

    //Verificar la siguiente subcategoria
    public int realizarSigueteSubcategoria(String[] lista) {
        int resultado = sortear(lista.length);
        return Integer.parseInt(lista[resultado]);
    }

    //Este método actualiza el estado del progreso
    public void actualizarEstadoProgreso(SQLiteDatabase db) {
        String strSQL = "UPDATE Progreso SET estado = " + 1 + " WHERE id_persona = "
                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
        db.execSQL(strSQL);
    }

    //Abre la actividad de relaciones espaciales
    private void abrirRelacionesEspaciales() {
        Intent intento = new Intent(Relaciones_espaciales.this, Relaciones_espaciales.class);
        intento.putExtra("id_usuario", id_usuario);
        startActivity(intento);
        finish();
    }

    //Actualiza la cantidad de preguntas en base de datos
    public void actualizarCantidadPreguntas(SQLiteDatabase db, int cantidad_preguntas) {
        String strSQL = "UPDATE Progreso SET cantidad_preguntas = " + cantidad_preguntas + " WHERE id_persona = " + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
        db.execSQL(strSQL);
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
        String[] id_p = new String[11];
        int contador = 0;
        int pasada = 0;
        if (cursor1.getCount() > 0) {
            if (cursor.moveToFirst()) {
                for (int j = 0; j < cursor.getCount(); j++) {
                    nombreImagen[contador] = cursor.getString(cursor.getColumnIndex("nombre_imagen"));
                    audio[contador] = cursor.getString(cursor.getColumnIndex("audio"));
                    id_p[contador] = cursor.getString(cursor.getColumnIndex("id"));
                    contador++;
                    cursor.moveToNext();
                }
            }
            cursor1.moveToFirst();
            cursor.moveToFirst();
            while (!cursor1.isAfterLast()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    String id_pregunta = cursor.getString(cursor.getColumnIndex("id")).trim();
                    String id_persona_pregunta = cursor1.getString(cursor1.getColumnIndex("id_pregunta")).trim();
                    if (cursor.getString(cursor.getColumnIndex("id")).trim().equals(cursor1.getString(cursor1.getColumnIndex("id_pregunta")).trim())
                            && id_usuario == Integer.parseInt(cursor1.getString(cursor1.getColumnIndex("id_persona")))) {
                        nombreImagen[i] = "";
                        audio[i] = "";
                        id_p[i] = "";
                    }
                    cursor.moveToNext();
                }
                cursor.moveToFirst();
                cursor1.moveToNext();
            }

            cursor.close();
            cursor1.close();
        } else {

            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    id_p[contador] = cursor.getString(cursor.getColumnIndex("id"));
                    nombreImagen[contador] = cursor.getString(cursor.getColumnIndex("nombre_imagen"));
                    //  this.id_subcategoria = cursor.getString(cursor.getColumnIndex("id_subcategoria"));
                    audio[contador] = cursor.getString(cursor.getColumnIndex("audio"));
                    contador++;
                    cursor.moveToNext();
                }
                cursor.close();
                insertarEstadistica();
            }
        }
        int cantidad = verificarCantidadArreglo(nombreImagen);
        nombreImagen = eliminarValoresArreglo(nombreImagen, cantidad);
        audio = eliminarValoresArreglo(audio, cantidad);
        id_p = eliminarValoresArreglo(id_p, cantidad);
        int r = verificarCantidadArreglo(nombreImagen);
        //si r es igual a cero se acabaron las preguntas
        if (r == 0) {
            //Se elimina las preguntas realizadas y se inserta en la tabla de estadisticas
            eliminarPreguntasRealizada();
            abrirRelacionesEspaciales();
        } else {
            int numero = sortear(r);
            this.id_pregunta = id_p[numero];
            //Obtiene la imagen de la pregunta
            Drawable d = establecerImagen(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "relaciones_espaciales/"));
            img.setImageDrawable(d);

            reproducirAudio(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "relaciones_espaciales/audios_preguntas_relaciones_espaciales/"),
                    "", "", null);
            audiogeneral = audio[numero];
            nombreSubcategoria = nombre_subcategoria;
        }
    }

    //Elimina las preguntas realizadas al usuario en la tabla de persona_pregunta
    public void eliminarPreguntasRealizada() {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor estadistica = db.rawQuery("select id" + " from Pregunta " +
                " where id_subcategoria = " + id_subcategoria, null);
        if (estadistica.getCount() > 0 && estadistica.moveToFirst()) {
            while (!estadistica.isAfterLast()) {
                int id_pregunta = Integer.parseInt(estadistica.getString(estadistica.getColumnIndex("id")));
                String strSQL = "DELETE FROM Persona_Pregunta WHERE id_persona = "
                        + id_usuario + " and " + " id_pregunta= " + id_pregunta;
                db.execSQL(strSQL);
                estadistica.moveToNext();
            }
        }
        db.close();
    }

    //insertar en la tabla de estadistica para llevar el registro por subcategoria
    public void insertarEstadistica() {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_persona", id_usuario);
        values.put("id_subcategoria", id_subcategoria);
        values.put("cantidad_errores", 0);
        values.put("cantidad_preguntas", 3);
        values.put("porcentaje", 0);
        db.insert("Estadistica", null, values);
        db.close();
    }

    //verifica la cantidad de elementos que se encuentran en el arreglo
    public int verificarCantidadArreglo(String[] datos) {
        int cantidad = 0;
        for (int i = 0; i < datos.length; i++)
            if (datos[i] != null)
                cantidad++;


        return cantidad;
    }

    //Elimina los valores repetidos
    public String[] eliminarValoresArreglo(String[] array, int cantidad) {
        String[] resultado = new String[11];
        resultado = array;
        int i = 0;
        while (i < cantidad) {
            if (resultado[i].trim().equals("") && array[i] != null) {
                resultado = ArrayUtils.remove(resultado, i);
                cantidad -= 1;
            } else {
                i++;
            }
        }
        return resultado;
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
            String[] nombre = ((String) opcion1.getTag()).split("\\.");
            if (!nombre[0].equals("derecha") || !nombre[0].equals("izquierda")) {
                nombre = nombre[0].split("_");
                nombre[0] = nombre[1];
            }
            if (respuestaAudio[0].equals(nombre[0] + ".mp3")) {
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + nombre[0] + ".mp3", "r", (String) opcion1.getTag(), respuesta);
                respuesta = new MediaPlayer();
                esperar();
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + "izquierda" + ".mp3", "r", (String) opcion3.getTag(), respuesta);
                esperar();
            } else {
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + nombre[0] + ".mp3", "r", (String) opcion3.getTag(), respuesta);
                respuesta = new MediaPlayer();
                esperar();
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + "derecha" + ".mp3", "r", (String) opcion1.getTag(), respuesta);
                esperar();
            }
        } else {
            for (int i = 0; i < respuestaAudio.length; i++) {
                String numeroRespuesta = respuestaAudio[i].substring(0, 3);
                if (!(numeroRespuesta.equals("r10"))) {
                    numeroRespuesta = numeroRespuesta.substring(0, 2);
                }
                if (((String) opcion1.getTag()).trim().equals(respuestaAudio[i].trim())) {
                    reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + numeroRespuesta + "/" + respuestaAudio[i], "r", (String) opcion1.getTag(), respuesta);
                    respuesta = new MediaPlayer();
                    esperar();
                } else if (((String) opcion2.getTag()).trim().equals(respuestaAudio[i].trim())) {
                    reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + numeroRespuesta + "/" + respuestaAudio[i], "r", (String) opcion2.getTag(), respuesta);
                    respuesta = new MediaPlayer();
                    esperar();
                } else {
                    reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + numeroRespuesta + "/" + respuestaAudio[i], "r", (String) opcion3.getTag(), respuesta);
                    respuesta = new MediaPlayer();
                    esperar();
                }
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
        //amanager.setMode(AudioManager.STREAM_MUSIC);
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
                if (archivos.length > 0) {
                    direccionImagen += archivos[0];
                } else {
                    Log.d("", "HHHHHHHHHHHHHH" + direccion + nombreSubcategoria + "/" + audio);
                }


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
        id_subcategoria = subcategoria.get(0);
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
        Cursor cursor1 = db.rawQuery("select * from  Persona_Pregunta  " +
                " where id_persona = " + id_usuario, null);
        return cursor1;
    }

    //Evento si se realiza un retroceso desde el dispositivo
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Relaciones_espaciales.this, MenuJuego.class);
        startActivity(intent);
        respuesta.release();
        pregunta.release();
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

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        if (speech != null) {
            speech.destroy();
            hacerAudio();
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String texto = "";
        for (String result : matches)
            if (result.equals("Atras") || result.equals("atras") || result.equals("anterior") || result.equals("Anterior")) {
                Intent menu = new Intent(Relaciones_espaciales.this, MenuJuego.class);
                startActivity(menu);
            }
        hacerAudio();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    //Reconocimiento de vooz
    public SpeechRecognizer hacerAudio() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        speech.startListening(recognizerIntent);
        return speech;
    }

    @Override
    protected void onResume() {
        if (pausa == 2) {
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }
        speech = hacerAudio();
        super.onResume();
    }

    @Override
    protected void onPause() {
        speech.destroy();
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (speech != null) {
            speech.destroy();
        }
        super.onDestroy();
    }

}
