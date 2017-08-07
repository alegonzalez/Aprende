package ale.aprende.aprende;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;


import com.beardedhen.androidbootstrap.BootstrapButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import ale.aprende.aprende.bd.DBHandler;

public class Relaciones_espaciales extends AppCompatActivity implements RecognitionListener, View.OnTouchListener {
    //Declaración de variables
    private int id_usuario;
    private Boolean eventoTocar = false;
    ImageView img;
    ImageButton opcion1, opcion2, opcion3;
    MediaPlayer pregunta = new MediaPlayer();
    AudioManager amanager;
    String id_subcategoria, audiogeneral, nombreSubcategoria, genero = "";
    MediaPlayer respuesta = new MediaPlayer();
    MediaPlayer audio = new MediaPlayer();
    private String id_pregunta;
    int pausa = 0;
    public SpeechRecognizer speech;
    private Intent recognizerIntent;
    private String estadoEstadistica = "1";
    final Handler handler = new Handler();
    final Handler tocarPantalla = new Handler();
    Runnable met;
    Boolean finalPregunta = false;

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
        genero = getIntent().getExtras().getString("genero");
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        DBHandler mdb = new DBHandler(getApplicationContext());
        List<String> subcategoria = obtenerProgreso(mdb);
        verificarTipoSubcategoria(subcategoria, mdb);
        pregunta.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {


            @Override
            public void onCompletion(MediaPlayer mp) {
                pregunta.stop();
                pregunta.release();
                ejecutarReproduccionAudio();
                finalPregunta = true;

            }
        });
        respuesta.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                respuesta.stop();
                respuesta.release();
                pausa = 2;
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                hacerAudio();
            }
        });
        audio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // audio.stop();
                audio.reset();
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
//                audio = new MediaPlayer();
            }
        });
    }

    private void ejecutar() {
        //Ejecicion del método en cierto tiempo
        tocarPantalla.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                verificarNoTocaPantalla();
            }
        }, 12000);
    }

    //Ejecuta las respuestas
    public void ejecutarReproduccionAudio() {
        establecerRespuesta(audiogeneral, nombreSubcategoria);
        //obtiene los audios de las respuestas de la pregunta
        String[] respuestaAudio = obtenerAudiosRespuesta("relaciones_espaciales/audios_respuesta_relaciones_espaciales/", audiogeneral, nombreSubcategoria);
        //Reproduce cada unos de los audios de las respuestas
        establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, respuesta);
    }

    //Este metodo se encarga de reproducir cuando hay un error o aprueba un tema
    public void audioMostrar(String direccion, MediaPlayer audio, AudioManager amanager, Context contexto) {
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        audio.reset();
        try {
            AssetFileDescriptor afd = contexto.getAssets().openFd(direccion);
            audio.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            audio.prepare();
            audio.setVolume(1, 1);
            audio.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Evento click del boton 1
    public void opcion1(View view) {
        verificarRespuesta((String) opcion1.getTag());
        opcion1.setEnabled(false);
    }

    //Evento click del boton 2
    public void opcion2(View view) {
        verificarRespuesta((String) opcion2.getTag());
        opcion2.setEnabled(false);
    }

    //Evento click del boton 3
    public void opcion3(View view) {
        verificarRespuesta((String) opcion3.getTag());
        opcion3.setEnabled(false);
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
            ocultarBotones();
            verificar(cantidad_preguntas, cantidad_errores, db, cursor);

        } else {
            handler.removeCallbacksAndMessages(null);
            //Reproducir audio para motivar al niño
            tocarPantalla.removeCallbacksAndMessages(null);
            String tipo_genero = (genero.equals("M")) ? "general/intentar_m.mp3" : "general/intentar_f.mp3";
            audioMostrar(tipo_genero, audio, amanager, this);
            ejecutar();
            cantidad_errores += 1;
            if (estadoEstadistica.equals("0")) {
                ponerErroresEstadisticas(1, 0, db, id_subcategoria, id_usuario);
            }
            actualizarProgreso(cantidad_preguntas, cantidad_errores, db, id_subcategoria, id_usuario);
        }
        cursor.close();
        db.close();
    }

    //verifica que no se inserte una pregunta que ya este en la tabla
    public void noRepetir(Cursor cursor, SQLiteDatabase db, int id_usuario, String id_pregunta) {
        int pasada = 0;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                if (id_pregunta.equals(cursor.getString(cursor.getColumnIndex("id_pregunta")))) {
                    pasada = 1;
                }
                cursor.moveToNext();
            }
        }
        if (pasada != 1) {
            ContentValues values = new ContentValues();
            values.put("id_persona", id_usuario);
            values.put("id_pregunta", id_pregunta);
            values.put("estado", true);
            db.insert("Persona_Pregunta", null, values);
        }
    }

    //valida la respuesta del niño
    public void verificar(int cantidad_preguntas, int cantidad_errores, SQLiteDatabase db, Cursor cursor) {
        audio.reset();
        handler.removeCallbacksAndMessages(null);
        Cursor pregunta = db.rawQuery("select id_pregunta from Persona_Pregunta where id_persona = " + id_usuario, null);
        noRepetir(pregunta, db, id_usuario, id_pregunta);
        if (nombreSubcategoria.equals("derecha") || nombreSubcategoria.equals("izquierda")) {
            List<String> subcategoria = new ArrayList<>();
            subcategoria.add(id_subcategoria);
            subcategoria.add(nombreSubcategoria.substring(0, 1).toUpperCase() + nombreSubcategoria.substring(1));
            cantidad_preguntas -= 1;
            if (cantidad_preguntas == 0) {
                if (cantidad_errores >= 3) {
                    cantidad_preguntas = 3;
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    if (estadoEstadistica.equals("0")) {
                        ponerErroresEstadisticas(0, 3, db, id_subcategoria, id_usuario);
                    }
                    abrirRelacionesEspaciales();
                } else if (cantidad_errores == 2) {
                    cantidad_preguntas = 2;
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    if (estadoEstadistica.equals("0")) {
                        ponerErroresEstadisticas(0, 2, db, id_subcategoria, id_usuario);
                    }
                    abrirRelacionesEspaciales();
                } else if (cantidad_errores == 1) {
                    cantidad_preguntas = 1;
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    if (estadoEstadistica.equals("0")) {
                        ponerErroresEstadisticas(0, 1, db, id_subcategoria, id_usuario);
                    }
                    abrirRelacionesEspaciales();
                } else {
                    //Tema superado
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    actualizarEstadoProgreso(db, id_subcategoria, id_usuario, estadoEstadistica);
                    if (estadoEstadistica.equals("0")) {
                        ponerErroresEstadisticas(0, 0, db, id_subcategoria, id_usuario);
                    }
                    actualizarEstadisticaTema(db, id_subcategoria, id_usuario);
                    if (estadoEstadistica.equals("1")) {
                        String strSQL1 = "UPDATE Progreso SET repeticion = " + 1 + " WHERE id_persona = "
                                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
                        db.execSQL(strSQL1);
                    }
                    int rs = obtenerSiguienteSubctegoria(db);
                    if (rs != 0) {
                        if (estadoEstadistica.equals("0")) {
                            insertarNuevaSubCategoria(rs, db, id_usuario);
                        } else {
                            String strSQL = "UPDATE Progreso SET repeticion = " + 2 + " WHERE id_persona = "
                                    + id_usuario + " and " + " id_subcategoria= " + rs;
                            db.execSQL(strSQL);
                        }
                        Cursor est = verificarDatosTablaEstadistica(db, id_subcategoria, id_usuario);
                        if (est.getCount() <= 0) {
                            DBHandler mdb = new DBHandler(getApplicationContext());
                            insertarEstadistica(mdb, id_subcategoria, id_usuario);
                        } else {
                            est.moveToFirst();
                            estadoEstadistica = est.getString(est.getColumnIndex("estado"));
                        }
                        String tipo_genero = (genero.equals("M")) ? "general/tema_superado_m.mp3" : "general/tema_superado_f.mp3";
                        audioMostrar(tipo_genero, audio, amanager, this);
                        met = new Runnable() {
                            public void run() {
                                abrirRelacionesEspaciales();
                            }
                        };
                        handler.postDelayed(met, 5000);
                    } else {
                        String strSQL1 = "UPDATE Progreso SET  cantidad_errores= 0, cantidad_preguntas=3, repeticion = 0 WHERE id_persona = "
                                + id_usuario + " and " + " id_subcategoria >=1 and id_subcategoria <=7";
                        db.execSQL(strSQL1);
                        if (estadoEstadistica.equals("0")) {
                            String tipo_genero = (genero.equals("M")) ? "general/colores_m.mp3" : "general/colores_f.mp3";
                            audio.reset();
                            audioMostrar(tipo_genero, audio, amanager, this);
                            met = new Runnable() {
                                public void run() {
                                    DBHandler mdb = new DBHandler(getApplicationContext());
                                    SQLiteDatabase db1 = mdb.getWritableDatabase();
                                    insertarColores(db1);
                                    db1.close();
                                }
                            };
                            handler.postDelayed(met, 5000);
                        } else {

                            procederColores();
                        }
                        return;
                        //Insertar en la tabla de progreso  la primer subcategoria de colores
                    }
                    //abrirRelacionesEspaciales();
                }
                // actualizarProgreso(cantidad_preguntas, cantidad_errores, db);
                actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
            } else {
                actualizarProgreso(cantidad_preguntas, cantidad_errores, db, id_subcategoria, id_usuario);
                abrirRelacionesEspaciales();
            }
        } else {
            verificarErrores(cursor, db, cantidad_preguntas, cantidad_errores);
            cursor.close();
        }
    }

    //Este metodo actualiza el estado en la tabla de estadisticas
    public void actualizarEstadisticaTema(SQLiteDatabase db, String id_subcategoria, int id_usuario) {
        String strSQL = "UPDATE Estadistica SET estado = 1 WHERE id_persona = "
                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria + "";
        db.execSQL(strSQL);
    }

    //Verifica si ya se inserto en la tabla de estadistica, si no para insertar
    public Cursor verificarDatosTablaEstadistica(SQLiteDatabase db, String id_subCategoria, int id_usuario) {
        Cursor estadistica = db.rawQuery("select id_subcategoria,estado from Estadistica where id_subcategoria = " + id_subCategoria + " and id_persona= " + id_usuario, null);
        return estadistica;
    }

    //Actualiza el progreso de las preguntas y cantidad de errores
    public void actualizarProgreso(int cantidad_preguntas, int cantidad_errores, SQLiteDatabase db, String id_subcategoria, int id_usuario) {
        String strSQL = "UPDATE Progreso SET cantidad_preguntas = " + cantidad_preguntas + ",cantidad_errores = " + cantidad_errores + " WHERE id_persona = "
                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
        db.execSQL(strSQL);
    }

    // segun el porcentaje que traiga se verifa para continuar con la cantidad de preguntas a realizar
    public void verificarErrores(Cursor cursor, SQLiteDatabase db, int cantidad_preguntas, int cantidad_errores) {
        if (cursor.moveToFirst()) {
            cantidad_preguntas -= 1;
            List<String> subcategoria = new ArrayList<>();
            subcategoria.add(id_subcategoria);
            subcategoria.add(nombreSubcategoria.substring(0, 1).toUpperCase() + nombreSubcategoria.substring(1));
            if (cantidad_preguntas <= 0) {
                if (cantidad_errores >= 6 || cantidad_errores == 5) {
                    cantidad_preguntas = 3;
                    if (estadoEstadistica.equals("0")) {
                        ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirRelacionesEspaciales();
                } else if (cantidad_errores == 4 || cantidad_errores == 3) {
                    cantidad_preguntas = 2;
                    if (estadoEstadistica.equals("0")) {
                        ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirRelacionesEspaciales();
                } else if (cantidad_errores == 2 || cantidad_errores == 1) {
                    cantidad_preguntas = 1;
                    if (estadoEstadistica.equals("0")) {
                        ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirRelacionesEspaciales();
                } else {
                    //Excelente paso  la subcategoria
                    actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    actualizarEstadoProgreso(db, id_subcategoria, id_usuario, estadoEstadistica);
                    actualizarEstadisticaTema(db, id_subcategoria, id_usuario);
                    if (estadoEstadistica.equals("1")) {
                        String strSQL1 = "UPDATE Progreso SET repeticion = " + 1 + " WHERE id_persona = "
                                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
                        db.execSQL(strSQL1);
                    }
                    int resultado = obtenerSiguienteSubctegoria(db);
                    if (resultado != 0) {
                        if (estadoEstadistica.equals("0")) {
                            insertarNuevaSubCategoria(resultado, db, id_usuario);
                        } else {
                            String strSQL = "UPDATE Progreso SET repeticion = " + 2 + " WHERE id_persona = "
                                    + id_usuario + " and " + " id_subcategoria= " + resultado;
                            db.execSQL(strSQL);
                        }
                        Cursor est = verificarDatosTablaEstadistica(db, id_subcategoria, id_usuario);
                        if (est.getCount() <= 0) {
                            DBHandler mdb = new DBHandler(getApplicationContext());
                            insertarEstadistica(mdb, id_subcategoria, id_usuario);
                        } else {
                            est.moveToFirst();
                            estadoEstadistica = est.getString(est.getColumnIndex("estado"));
                        }
                        String tipo_genero = (genero.equals("M")) ? "general/tema_superado_m.mp3" : "general/tema_superado_f.mp3";
                        audio.reset();
                        audioMostrar(tipo_genero, audio, amanager, this);
                        met = new Runnable() {
                            public void run() {
                                abrirRelacionesEspaciales();
                            }
                        };
                        handler.postDelayed(met, 5000);
                    } else {
                        String strSQL1 = "UPDATE Progreso SET  cantidad_errores= 0, cantidad_preguntas=3, repeticion = 0 WHERE id_persona = "
                                + id_usuario + " and " + " id_subcategoria >=1 and id_subcategoria <=7";
                        db.execSQL(strSQL1);
                        if (estadoEstadistica.equals("0")) {
                            String tipo_genero = (genero.equals("M")) ? "general/colores_m.mp3" : "general/colores_f.mp3";
                            audio.reset();
                            audioMostrar(tipo_genero, audio, amanager, this);
                            met = new Runnable() {
                                public void run() {
                                    DBHandler mdb = new DBHandler(getApplicationContext());
                                    SQLiteDatabase db1 = mdb.getWritableDatabase();
                                    insertarColores(db1);
                                    db1.close();
                                }
                            };
                            handler.postDelayed(met, 5000);
                        } else {
                            procederColores();
                        }
                        return;
                        //Insertar en la tabla de progreso  la primer subcategoria de colores
                    }
                }
            } else {
                actualizarProgreso(cantidad_preguntas, cantidad_errores, db, id_subcategoria, id_usuario);
                abrirRelacionesEspaciales();
            }
        }
    }

    //Este metodo se encarga de abrir las colores y de actualizar los datos
    private void procederColores() {
        Colores col = new Colores();
        int numero = sortear(4);
        numero++;
        numero = col.obtenerNumeroSubcategoria(numero);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        Intent color = new Intent(Relaciones_espaciales.this, Colores.class);
        color.putExtra("id_usuario", id_usuario);
        color.putExtra("genero", genero);
        color.putExtra("id_subcategoria", numero);
        startActivity(color);
        finish();
    }



    //Insert los colores por primera vez
    private void insertarColores(SQLiteDatabase db) {
        int numero = sortear(4);
        numero++;
        if (numero == 1) {
            numero = 8;
        } else if (numero == 2) {
            numero = 9;
        } else if (numero == 3) {
            numero = 10;
        } else if (numero == 4) {
            numero = 11;
        } else if (numero == 5) {
            numero = 12;
        }
        ContentValues values = new ContentValues();
        values.put("id_persona", id_usuario);
        values.put("id_subcategoria", numero);
        values.put("cantidad_preguntas", 3);
        values.put("estado", false);
        values.put("cantidad_errores", 0);
        values.put("repeticion", 0);
        db.insert("Progreso", null, values);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        Intent color = new Intent(Relaciones_espaciales.this, Colores.class);
        color.putExtra("id_usuario", id_usuario);
        color.putExtra("genero", genero);
        color.putExtra("id_subcategoria", numero);
        startActivity(color);
        finish();
    }

    //Actualizar en la tabla de estadistica la cantidad de errores y preguntas que le pertenece a una subcategoria
    public void ponerErroresEstadisticas(int cantidad_errores, int cantidad_preguntas, SQLiteDatabase db, String id_subcategoria, int id_usuario) {
        Cursor estadistica = db.rawQuery("select cantidad_errores,cantidad_preguntas " + " from Estadistica " +
                " where id_subcategoria = " + id_subcategoria + " and " + " id_persona= " + id_usuario, null);
        if (estadistica.getCount() > 0 && estadistica.moveToFirst()) {
            cantidad_preguntas += Integer.parseInt(estadistica.getString(estadistica.getColumnIndex("cantidad_preguntas")));
            cantidad_errores += Integer.parseInt(estadistica.getString(estadistica.getColumnIndex("cantidad_errores")));
        }
        String strSQL = "UPDATE Estadistica SET cantidad_errores = " + cantidad_errores + ",cantidad_preguntas = " + cantidad_preguntas + " WHERE id_persona = "
                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
        db.execSQL(strSQL);
    }

    //Inserta en la tabla de progreso la nueva subcategoria
    public void insertarNuevaSubCategoria(int id_subcategoria, SQLiteDatabase db, int id_usuario) {
        ContentValues values = new ContentValues();
        values.put("id_persona", id_usuario);
        values.put("id_subcategoria", id_subcategoria);
        values.put("estado", false);
        values.put("cantidad_preguntas", 3);
        values.put("cantidad_errores", 0);
        values.put("repeticion", 0);
        db.insert("Progreso", null, values);
    }

    //Este método se encarga de obtener la siguiente subcategoria
    public int obtenerSiguienteSubctegoria(SQLiteDatabase db) {
        String[] listaDisponible = new String[]{"1", "2", "3", "4", "5", "6", "7"};
        Cursor subcategoriasProgreso = null;
        if (estadoEstadistica.equals("1")) {
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria <= " + 7 + " and " + " id_persona= " + id_usuario + " and repeticion=1", null);
        } else {
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria <= " + 7 + " and " + " id_persona= " + id_usuario, null);
        }

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
    public void actualizarEstadoProgreso(SQLiteDatabase db, String id_subcategoria, int id_usuario, String estadoEstadistica) {
        if (estadoEstadistica.equals("1")) {
            String strSQL = "UPDATE Progreso SET repeticion = " + 1 + ", estado= 1  WHERE id_persona = "
                    + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
        } else {
            String strSQL = "UPDATE Progreso SET estado = " + 1 + " WHERE id_persona = "
                    + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
            db.execSQL(strSQL);
        }
    }

    //Abre la actividad de relaciones espaciales
    private void abrirRelacionesEspaciales() {
        Intent intento = new Intent(Relaciones_espaciales.this, Relaciones_espaciales.class);
        intento.putExtra("id_usuario", id_usuario);
        intento.putExtra("genero", genero);
        startActivity(intento);
        finish();
    }

    // Obtiene el progreso acerca del tema de las preguntas que no se han realizado
    public List obtenerProgreso(DBHandler mdb) {
        List<String> subcategoria = new ArrayList<String>();

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
    public Cursor obtenerPreguntasRealizadas(int id_subcategoria, DBHandler mdb) {
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
            }
        }

        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor est = verificarDatosTablaEstadistica(db, id_subcategoria, id_usuario);

        if (est.getCount() <= 0) {
            insertarEstadistica(mdb, id_subcategoria, id_usuario);
        } else {
            est.moveToFirst();
            estadoEstadistica = est.getString(est.getColumnIndex("estado"));
        }
        db.close();
        est.close();
        int cantidad = verificarCantidadArreglo(nombreImagen);
        nombreImagen = eliminarValoresArreglo(nombreImagen, cantidad);
        audio = eliminarValoresArreglo(audio, cantidad);
        id_p = eliminarValoresArreglo(id_p, cantidad);
        int r = verificarCantidadArreglo(nombreImagen);
        //si r es igual a cero se acabaron las preguntas
        if (r == 0) {
            //Se elimina las preguntas realizadas y se inserta en la tabla de estadisticas
            eliminarPreguntasRealizada(mdb, id_subcategoria, id_usuario);
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
    public void eliminarPreguntasRealizada(DBHandler mdb, String id_subcategoria, int id_usuario) {

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
    public String insertarEstadistica(DBHandler mdb, String id_subcategoria, int id_usuario) {
        SQLiteDatabase db = mdb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_persona", id_usuario);
        values.put("id_subcategoria", id_subcategoria);
        values.put("cantidad_errores", 0);
        values.put("cantidad_preguntas", 3);
        values.put("porcentaje", 0);
        values.put("estado", "0");
        db.insert("Estadistica", null, values);
        db.close();
        estadoEstadistica = "0";
        return estadoEstadistica;
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

    //Reproducción de audios de respuesta
    public void establecerAudiosRespuesta(String[] respuestaAudio, final String nombreSubcategoria, MediaPlayer respuesta) {
        final String nombreSub = nombreSubcategoria;
        final MediaPlayer resp = respuesta;
        final String[] respAudio = respuestaAudio;
        if (nombreSubcategoria.trim().equals("derecha") || nombreSubcategoria.trim().equals("izquierda")) {
            String[] resultado = ((String) opcion1.getTag()).split("_");
            String[] nombre = ((String) opcion1.getTag()).split("\\.");

            if (!nombre[0].equals("derecha") || !nombre[0].equals("izquierda")) {
                nombre = nombre[0].split("_");
                nombre[0] = nombre[1];
            }
            if (respuestaAudio[0].equals(nombre[0] + ".mp3")) {
                final String nombreN = nombre[0];
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + nombreN + ".mp3", "r", (String) opcion1.getTag(), resp);
                met = new Runnable() {
                    public void run() {
                        reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + "izquierda" + ".mp3", "r", (String) opcion3.getTag(), resp);
                    }
                };
                handler.postDelayed(met, 3000);
            } else {
                final String nombree = nombre[0];
                reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + nombree + ".mp3", "r", (String) opcion3.getTag(), resp);
                met = new Runnable() {
                    public void run() {
                        reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + "derecha" + ".mp3", "r", (String) opcion1.getTag(), resp);
                    }
                };
                handler.postDelayed(met, 3000);
            }
            met = new Runnable() {
                public void run() {
                    hacerAudio();
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    finalPregunta = false;
                    ejecutar();
                }
            };
            handler.postDelayed(met, 5000);
        } else {
            final String[] n = obtenerNumeros(2);
            for (int i = 0; i < respuestaAudio.length; i++) {
                final int posicion = i;
                String numeroRespuesta = respuestaAudio[Integer.parseInt(n[i])].substring(0, 3);
                if (!(numeroRespuesta.equals("r10"))) {
                    numeroRespuesta = numeroRespuesta.substring(0, 2);
                }
                final String numRes = numeroRespuesta;
                if (n[i].equals("0")) {
                    met = new Runnable() {
                        public void run() {
                            reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + numRes + "/" + respAudio[Integer.parseInt(n[posicion])], "r", (String) opcion1.getTag(), resp);
                        }
                    };
                    handler.postDelayed(met, 3000);
                } else if (n[i].equals("1")) {
                    met = new Runnable() {
                        public void run() {
                            reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + numRes + "/" + respAudio[Integer.parseInt(n[posicion])], "r", (String) opcion2.getTag(), resp);
                        }
                    };
                    handler.postDelayed(met, 6000);
                } else {
                    met = new Runnable() {
                        public void run() {
                            reproducirAudio("relaciones_espaciales/audios_respuesta_relaciones_espaciales/" + nombreSubcategoria + "/" + numRes + "/" + respAudio[Integer.parseInt(n[posicion])], "r", (String) opcion3.getTag(), resp);
                        }
                    };
                    handler.postDelayed(met, 9000);
                }
            }
            met = new Runnable() {
                public void run() {
                    hacerAudio();
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    finalPregunta = false;
                    ejecutar();
                }
            };
            handler.postDelayed(met, 12000);
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
        String[] listaNumeros = new String[31];
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
                respuesta = new MediaPlayer();
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
    public Drawable establecerImagen(String nombreImagen) {
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

    //Este metodo se encarga de verificar cuando el usuario quiere volver a jugar un tema ya superado
    private String volverJugarTema(SQLiteDatabase db) {
        String nombreSubcategoria = "";
        Cursor rep = db.rawQuery("select id from  Progreso " +
                " where id_subcategoria >=1 and id_subcategoria <=7 and " + "id_persona = "
                + id_usuario + " and repeticion= 1", null);
        Cursor repeticion = db.rawQuery("select id_subcategoria from  Progreso " +
                " where id_subcategoria >=1 and id_subcategoria <=7 and " + "id_persona = "
                + id_usuario + " and repeticion= 2", null);
        if (repeticion.getCount() <= 0) {
            if (rep.getCount() == 7) {
                String strSQL1 = "UPDATE Progreso SET cantidad_preguntas = 3, cantidad_errores = 0, repeticion = 0 WHERE id_persona = "
                        + id_usuario + " and " + " id_subcategoria >=1 and id_subcategoria <=7";
                db.execSQL(strSQL1);
            }
            int id = sortear(6);
            id++;
            id_subcategoria = "" + id;
            String strSQL = "UPDATE Progreso SET cantidad_preguntas = 3, cantidad_errores = 0,repeticion=2 WHERE id_persona = "
                    + id_usuario + " and " + " id_subcategoria= " + id_subcategoria + "";
            db.execSQL(strSQL);
        } else {
            if (repeticion.moveToFirst()) {
                id_subcategoria = repeticion.getString(repeticion.getColumnIndex("id_subcategoria"));
            }
        }
        if (id_subcategoria.equals("1")) {
            nombreSubcategoria = "Abajo";
        } else if (id_subcategoria.equals("2")) {
            nombreSubcategoria = "Adelante";
        } else if (id_subcategoria.equals("3")) {
            nombreSubcategoria = "Arriba";
        } else if (id_subcategoria.equals("4")) {
            nombreSubcategoria = "Atras";
        } else if (id_subcategoria.equals("5")) {
            nombreSubcategoria = "Centro";
        } else if (id_subcategoria.equals("6")) {
            nombreSubcategoria = "Derecha";
        } else if (id_subcategoria.equals("7")) {
            nombreSubcategoria = "Izquierda";
        }
        rep.close();
        repeticion.close();
        return nombreSubcategoria;
    }

    //Verificar el tipo de subcategoria
    public void verificarTipoSubcategoria(List<String> subcategoria, DBHandler mdb) {
        SQLiteDatabase db = mdb.getWritableDatabase();
        if (subcategoria.size() != 0) {
            id_subcategoria = subcategoria.get(0);
            if (Integer.parseInt(id_subcategoria) >= 1 && Integer.parseInt(id_subcategoria) <= 7) {
            } else {
                //Llamar al metodo
                subcategoria.add(0, id_subcategoria);
                subcategoria.add(1, volverJugarTema(db));
            }
        } else {
            //LLamar al metodo
            subcategoria.add(0, id_subcategoria);
            subcategoria.add(1, volverJugarTema(db));
        }
        List datos = new ArrayList<>();
        if ((subcategoria.get(1)).trim().equals("Abajo")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "abajo");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Adelante")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "adelante");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Arriba")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "arriba");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Atras")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "atras");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Centro")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "centro");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Derecha")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "derecha");
            cursor.close();
            cursor1.close();
        } else if ((subcategoria.get(1)).trim().equals("Izquierda")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "izquierda");
            cursor.close();
            cursor1.close();
        }
        db.close();
    }

    //Este metodo obtiene la información de la tabla persona_pregunta
    public Cursor obtenerTablaPersona_pregunta(SQLiteDatabase db, int id_usuario) {
        Cursor cursor1 = db.rawQuery("select * from  Persona_Pregunta  " +
                " where id_persona = " + id_usuario, null);
        return cursor1;
    }

    //Evento si se realiza un retroceso desde el dispositivo
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Relaciones_espaciales.this, MenuJuego.class);
        intent.putExtra("id_usuario", id_usuario);
        intent.putExtra("genero", genero);
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
        speech.destroy();
        hacerAudio();
    }

    @Override
    public void unregisterForContextMenu(View view) {
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String texto = "";
        for (String result : matches)
            if (result.equals("Atras") || result.equals("atras") || result.equals("anterior") || result.equals("Anterior")) {
                Intent menu = new Intent(Relaciones_espaciales.this, MenuJuego.class);
                menu.putExtra("id_usuario", id_usuario);
                menu.putExtra("genero", genero);
                startActivity(menu);
            } else {
                verificarPreguntaYrespuesta(result);
            }
        hacerAudio();
    }

    public void incorrecto(SQLiteDatabase db, Cursor cursor) {
        handler.removeCallbacksAndMessages(null);
        int cantidad_preguntas = 0;
        int cantidad_errores = 0;
        if (cursor.moveToFirst()) {
            cantidad_preguntas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("cantidad_preguntas")));
            cantidad_errores = Integer.parseInt(cursor.getString(cursor.getColumnIndex("cantidad_errores")));
        }
        if (estadoEstadistica.equals("0")) {
            ponerErroresEstadisticas(1, 0, db, id_subcategoria, id_usuario);
        }
        actualizarProgreso(cantidad_preguntas, cantidad_errores + 1, db, id_subcategoria, id_usuario);
        String tipo_genero = (genero.equals("M")) ? "general/intentar_m.mp3" : "general/intentar_f.mp3";
        audioMostrar(tipo_genero, audio, amanager, this);
        tocarPantalla.removeCallbacksAndMessages(null);
        ejecutar();
    }

    //Verifica las respuestas de las preguntas segun la subcategoria
    public void verificarPreguntaYrespuesta(String texto) {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select cantidad_preguntas,cantidad_errores " + " from Progreso " +
                " where id_subcategoria = " + id_subcategoria + " and " + " id_persona= " + id_usuario, null);
        int cantidad_preguntas = 0;
        int cantidad_errores = 0;
        if (cursor.moveToFirst()) {
            cantidad_preguntas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("cantidad_preguntas")));
            cantidad_errores = Integer.parseInt(cursor.getString(cursor.getColumnIndex("cantidad_errores")));
        }
        if (id_pregunta.equals("1") && id_subcategoria.equals("1")) {
            if (texto.equals("Gato") || texto.equals("gato") || texto.equals("sombrero") || texto.equals("Sombrero")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Perro") || texto.equals("perro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("2") && id_subcategoria.equals("1")) {
            if (texto.equals("Radio") || texto.equals("radio") || texto.equals("Micrófono") || texto.equals("micrófono")
                    || texto.equals("Microfono") || texto.equals("microfono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Sombrero") || texto.equals("sombrero")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("3") && id_subcategoria.equals("1")) {
            if (texto.equals("Medalla") || texto.equals("medalla") || texto.equals("Cronómetro") || texto.equals("cronómetro")
                    || texto.equals("Cronometro") || texto.equals("cronometro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Balón") || texto.equals("balón") || texto.equals("Balon") || texto.equals("balon")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("4") && id_subcategoria.equals("1")) {
            if (texto.equals("Estrella") || texto.equals("estrella") || texto.equals("Corazón") || texto.equals("corazón")
                    || texto.equals("Corazon") || texto.equals("corazon")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("círculo") || texto.equals("Círculo") || texto.equals("circulo") || texto.equals("Circulo")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("5") && id_subcategoria.equals("1")) {
            if (texto.equals("Cuadro") || texto.equals("cuadro") || texto.equals("Cuadrado") || texto.equals("cuadrado")
                    || texto.equals("Círculo") || texto.equals("círculo") || texto.equals("Circulo") || texto.equals("circulo")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("triángulo") || texto.equals("Triángulo") || texto.equals("triangulo") || texto.equals("Triangulo")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("6") && id_subcategoria.equals("1")) {
            if (texto.equals("Bolso") || texto.equals("bolso") || texto.equals("Micrófono") || texto.equals("micrófono")
                    || texto.equals("Microfono") || texto.equals("microfono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Botella") || texto.equals("botella")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("7") && id_subcategoria.equals("1")) {
            if (texto.equals("Conejo") || texto.equals("conejo") || texto.equals("Pájaro") || texto.equals("pájaro")
                    || texto.equals("Pajaro") || texto.equals("pajaro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Tortuga") || texto.equals("tortuga")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("8") && id_subcategoria.equals("1")) {
            if (texto.equals("Estrella") || texto.equals("estrella") || texto.equals("Triángulo") || texto.equals("triángulo")
                    || texto.equals("Triangulo") || texto.equals("triangulo")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Cuadro") || texto.equals("Cuadrado") || texto.equals("cuadrado") || texto.equals("cuadro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("9") && id_subcategoria.equals("1")) {
            if (texto.equals("Pájaro") || texto.equals("pájaro") || texto.equals("Pajaro") || texto.equals("pajaro")
                    || texto.equals("Gato") || texto.equals("gato")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Perro") || texto.equals("perro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("10") && id_subcategoria.equals("1")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("Conejo") || texto.equals("conejo")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Pájaro") || texto.equals("pájaro") || texto.equals("Pajaro") || texto.equals("pajaro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("11") && id_subcategoria.equals("2")) {
            if (texto.equals("Mono") || texto.equals("mono") || texto.equals("Perro") || texto.equals("perro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Pájaro") || texto.equals("pájaro") || texto.equals("Pajaro") || texto.equals("pajaro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("12") && id_subcategoria.equals("2")) {
            if (texto.equals("Tortuga") || texto.equals("tortuga") || texto.equals("Mono") || texto.equals("mono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Perro") || texto.equals("perro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("13") && id_subcategoria.equals("2")) {
            if (texto.equals("Cangrejo") || texto.equals("cangrejo") || texto.equals("Mono") || texto.equals("mono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Perro") || texto.equals("perro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("14") && id_subcategoria.equals("2")) {
            if (texto.equals("Sombrero") || texto.equals("sombrero") || texto.equals("Anteojos") || texto.equals("anteojos")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Bolso") || texto.equals("bolso")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("15") && id_subcategoria.equals("2")) {
            if (texto.equals("Lupa") || texto.equals("lupa") || texto.equals("Llave") || texto.equals("llave")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("micrófono") || texto.equals("Micrófono") || texto.equals("microfono") || texto.equals("Microfono")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("16") && id_subcategoria.equals("2")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("Conejo") || texto.equals("conejo")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Pájaro") || texto.equals("pájaro") || texto.equals("Pajaro") || texto.equals("pajaro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("17") && id_subcategoria.equals("2")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("Mono") || texto.equals("mono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Gusano") || texto.equals("gusano")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("18") && id_subcategoria.equals("2")) {
            if (texto.equals("Gato") || texto.equals("gato") || texto.equals("Pájaro") || texto.equals("pájaro") || texto.equals("Pajaro") || texto.equals("pajaro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Conejo") || texto.equals("conejo")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("19") && id_subcategoria.equals("2")) {
            if (texto.equals("Llave") || texto.equals("llave") || texto.equals("Bolso") || texto.equals("bolso")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Carro") || texto.equals("carro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("20") && id_subcategoria.equals("3")) {
            if (texto.equals("Cangrejo") || texto.equals("cangrejo") || texto.equals("Perro") || texto.equals("perro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Pájaro") || texto.equals("pájaro") || texto.equals("Pajaro") || texto.equals("pajaro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("21") && id_subcategoria.equals("3")) {
            if (texto.equals("Lápiz") || texto.equals("lápiz") || texto.equals("Lapiz") || texto.equals("lapiz")
                    || texto.equals("Lapicero") || texto.equals("lapicero")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Regla") || texto.equals("regla")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("22") && id_subcategoria.equals("3")) {
            if (texto.equals("Gusano") || texto.equals("gusano") || texto.equals("Perro") || texto.equals("perro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Mono") || texto.equals("mono")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("23") && id_subcategoria.equals("3")) {
            if (texto.equals("Flor") || texto.equals("flor")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Sol") || texto.equals("sol")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("24") && id_subcategoria.equals("3")) {
            if (texto.equals("Lagartija") || texto.equals("lagartija") || texto.equals("Gato") || texto.equals("gato")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Perro") || texto.equals("perro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("25") && id_subcategoria.equals("3")) {
            if (texto.equals("Reloj") || texto.equals("reloj") || texto.equals("Llave") || texto.equals("llave")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Libro") || texto.equals("libro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("26") && id_subcategoria.equals("3")) {
            if (texto.equals("Balón") || texto.equals("balón") || texto.equals("Balon") || texto.equals("balon")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Guitarra") || texto.equals("guitarra")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("27") && id_subcategoria.equals("3")) {
            if (texto.equals("Computadora") || texto.equals("computadora") || texto.equals("Tijeras") || texto.equals("tijeras")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Lupa") || texto.equals("lupa")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("28") && id_subcategoria.equals("3")) {
            if (texto.equals("Balón") || texto.equals("balón") || texto.equals("Balon") || texto.equals("balon") || texto.equals("Medalla") || texto.equals("medalla")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Llave") || texto.equals("llave")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("29") && id_subcategoria.equals("3")) {
            if (texto.equals("Zanahoria") || texto.equals("zanahoria") || texto.equals("Manzana") || texto.equals("manzana")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Huevo") || texto.equals("huevo")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("30") && id_subcategoria.equals("4")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("Gusano") || texto.equals("gusano")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Conejo") || texto.equals("conejo")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("31") && id_subcategoria.equals("4")) {
            if (texto.equals("Pingüino") || texto.equals("pingüino") || texto.equals("Mono") || texto.equals("mono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Perro") || texto.equals("perro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("32") && id_subcategoria.equals("4")) {
            if (texto.equals("Canasta") || texto.equals("canasta") || texto.equals("Sombrero") || texto.equals("sombrero")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Queque") || texto.equals("queque")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("33") && id_subcategoria.equals("4")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("pájaro") || texto.equals("Pájaro")
                    || texto.equals("pajaro") || texto.equals("Pajaro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Mono") || texto.equals("mono")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("34") && id_subcategoria.equals("4")) {
            if (texto.equals("Sombrero") || texto.equals("sombrero") || texto.equals("Anteojos") || texto.equals("anteojos")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Helado") || texto.equals("helado")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("35") && id_subcategoria.equals("4")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("Mono") || texto.equals("mono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("pájaro") || texto.equals("Pájaro") || texto.equals("pajaro") || texto.equals("Pajaro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("36") && id_subcategoria.equals("4")) {
            if (texto.equals("Conejo") || texto.equals("conejo") || texto.equals("Tortuga") || texto.equals("tortuga")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Jirafa") || texto.equals("jirafa")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("37") && id_subcategoria.equals("4")) {
            if (texto.equals("Anteojos") || texto.equals("anteojos") || texto.equals("Sombrero") || texto.equals("sombrero")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Huevo") || texto.equals("huevo")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("38") && id_subcategoria.equals("4")) {
            if (texto.equals("Árbol") || texto.equals("árbol") || texto.equals("Arbol") || texto.equals("arbol") || texto.equals("Llave") || texto.equals("llave")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Casa") || texto.equals("casa")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("39") && id_subcategoria.equals("4")) {
            if (texto.equals("Mono") || texto.equals("mono") || texto.equals("Perro") || texto.equals("perro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Tortuga") || texto.equals("tortuga")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("40") && id_subcategoria.equals("5")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("Jirafa") || texto.equals("jirafa")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Mono") || texto.equals("mono")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("41") && id_subcategoria.equals("5")) {
            if (texto.equals("Perro") || texto.equals("perro") || texto.equals("Gato") || texto.equals("gato")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Sombrero") || texto.equals("sombrero")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("42") && id_subcategoria.equals("5")) {
            if (texto.equals("Gusano") || texto.equals("gusano") || texto.equals("Mono") || texto.equals("mono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Pájaro") || texto.equals("pájaro") || texto.equals("Pajaro") || texto.equals("pajaro")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("43") && id_subcategoria.equals("5")) {
            if (texto.equals("Muñeco") || texto.equals("muñeco") || texto.equals("Cangrejo") || texto.equals("cangrejo")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Queque") || texto.equals("queque")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("44") && id_subcategoria.equals("5")) {
            if (texto.equals("Pingüino") || texto.equals("pingüino") || texto.equals("Perro") || texto.equals("perro")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Tucán") || texto.equals("Tucan") || texto.equals("tucan") || texto.equals("tucán")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("45") && id_subcategoria.equals("5")) {
            if (texto.equals("Gorra") || texto.equals("gorra") || texto.equals("Helado") || texto.equals("helado")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Anteojos") || texto.equals("anteojos")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("46") && id_subcategoria.equals("5")) {
            if (texto.equals("Muñeco") || texto.equals("muñeco") || texto.equals("Canasta") || texto.equals("canasta")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Queque") || texto.equals("queque")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("47") && id_subcategoria.equals("5")) {
            if (texto.equals("Flamingo") || texto.equals("flamingo") || texto.equals("Mono") || texto.equals("mono")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Tortuga") || texto.equals("tortuga")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("48") && id_subcategoria.equals("5")) {
            if (texto.equals("Lapicero") || texto.equals("lapicero") || texto.equals("Regla") || texto.equals("regla")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto(db, cursor);
            } else if (texto.equals("Pizarra") || texto.equals("pizarra")) {
                verificar(cantidad_preguntas, cantidad_errores, db, cursor);
            }
        } else if (id_pregunta.equals("49") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("50") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("51") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("52") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("53") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("54") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("55") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("56") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("57") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("58") && id_subcategoria.equals("6")) {
            derecha(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("59") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("60") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("61") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("62") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("63") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("64") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("65") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("66") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("67") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        } else if (id_pregunta.equals("68") && id_subcategoria.equals("7")) {
            izquerda(texto, db, cursor, cantidad_preguntas, cantidad_errores);
        }
    }

    //Este metodo valida por la voz si dijo derecha el niño
    public void derecha(String texto, SQLiteDatabase db, Cursor cursor, int cantidad_preguntas, int cantidad_errores) {
        if (texto.equals("Izquierda") || texto.equals("izquierda")) {
            //Incorrecto vamos intentalo nuevamente
            incorrecto(db, cursor);
        } else if (texto.equals("Derecha") || texto.equals("derecha")) {
            verificar(cantidad_preguntas, cantidad_errores, db, cursor);
        }
    }

    public void izquerda(String texto, SQLiteDatabase db, Cursor cursor, int cantidad_preguntas, int cantidad_errores) {
        if (texto.equals("Derecha") || texto.equals("derecha")) {
            //Incorrecto vamos intentalo nuevamente
            incorrecto(db, cursor);
        } else if (texto.equals("Izquierda") || texto.equals("izquierda")) {
            verificar(cantidad_preguntas, cantidad_errores, db, cursor);
        }
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
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speech.startListening(recognizerIntent);
        return speech;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (finalPregunta == true) {
            ejecutarReproduccionAudio();
        } else if (pausa == 3) {
            pregunta.start();
        }else if(pausa == 1){
            audio.reset();
            animar(opcion1);
            animar(opcion2);
            animar(opcion3);
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            ejecutar();
        }
    }

    @Override
    protected void onPause() {
        pausa = 1;
        if (speech != null) {
            speech.destroy();
        }
        try {
            if (pregunta.isPlaying()) {
                pregunta.pause();
                pausa = 3;
            }
        } catch (Exception e) {
        }

        handler.removeCallbacksAndMessages(null);
        tocarPantalla.removeCallbacksAndMessages(null);
        audio.reset();
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speech != null) {
            speech.destroy();
        }
        tocarPantalla.removeCallbacksAndMessages(null);
        audio.reset();
    }

    //Este metodo verifica que si la pantalla no es tocada en cierto limite de tiempo
    public void verificarNoTocaPantalla() {
        if (!eventoTocar) {
            eventoTocar = false;
            tocarPantalla.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    verificarNoTocaPantalla();
                }
            }, 12000);

            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            String direccionAudio = (genero.equals("M")) ? "general/ejercicios_m.mp3" : "general/ejercicios_f.mp3";
            reproducir(direccionAudio);
            animar(opcion1);
            animar(opcion2);
            animar(opcion3);
            tocarPantalla.removeCallbacksAndMessages(null);
        } else {
            tocarPantalla.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    verificarNoTocaPantalla();

                }
            }, 12000);
            eventoTocar = false;
        }
    }

    private void reproducir(String direccion) {
        audio.reset();
        try {
            AssetFileDescriptor afd = getAssets().openFd(direccion);
            audio.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            audio.prepare();
            audio.setVolume(1, 1);
            audio.start();
//            pasada = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //animari botones
    private void animar(ImageButton btn) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(btn, "alpha", 1f, .3f);
        fadeOut.setDuration(1000);
        fadeOut.setRepeatCount(ValueAnimator.INFINITE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(btn, "alpha", .3f, 1f);
        fadeIn.setDuration(1000);
        fadeIn.setRepeatCount(ValueAnimator.INFINITE);
        final AnimatorSet mAnimationSet = new AnimatorSet();

        mAnimationSet.play(fadeIn).after(fadeOut);
        mAnimationSet.start();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int eventaction = event.getAction();
        if (eventaction == MotionEvent.ACTION_DOWN) {
            eventoTocar = true;
        }
        return true;

    }
    //Metodo detecta cuando cambia de horientación
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            handler.removeCallbacks(met);
            //respuesta.release();
            abrirRelacionesEspaciales();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            handler.removeCallbacks(met);
            //respuesta.release();
            abrirRelacionesEspaciales();
        }
    }
}
