package ale.aprende.aprende;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import ale.aprende.aprende.bd.DBHandler;

public class Figuras_geometricas extends AppCompatActivity implements RecognitionListener, View.OnTouchListener {
    private int id_usuario = 0;
    private Boolean eventoTocar = false;
    private String genero, id_subcategoria, id_pregunta, audiogeneral, nombreSubcategoria = "";
    final MediaPlayer respuesta = new MediaPlayer();
    MediaPlayer audio = new MediaPlayer();
    MediaPlayer pregunta = new MediaPlayer();
    ImageView img;
    ImageButton opcion1, opcion2, opcion3;
    Relaciones_espaciales r = new Relaciones_espaciales();
    AudioManager amanager;
    int pausa = 0;
    private String estadoEstadistica = "1";
    public SpeechRecognizer speech;
    private Intent recognizerIntent;
    final Handler handler = new Handler();
    final Handler tocarPantalla = new Handler();
    Runnable met;
    Boolean finalPregunta = false;
    Colores c = new Colores();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_figuras_geometricas);
        id_usuario = getIntent().getExtras().getInt("id_usuario");
        genero = getIntent().getExtras().getString("genero");
        id_subcategoria = getIntent().getExtras().getString("id_subcategoria");
        img = (ImageView) findViewById(R.id.imgPregunta);
        opcion1 = (ImageButton) findViewById(R.id.imgBtnPrimeraOpcion);
        opcion2 = (ImageButton) findViewById(R.id.imgBtnSegundaOpcion);
        opcion3 = (ImageButton) findViewById(R.id.imgBtnTerceraOpcion);
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        ocultarBotones();
        DBHandler mdb = new DBHandler(getApplicationContext());
        List<String> subcategoria = r.obtenerProgreso(mdb);
        verificarTipoSubcategoria(subcategoria, mdb, getApplicationContext());
        //Este metodo se ejecuta cuando termina de reproducir el audio de la pregunta
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
                audio.reset();
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        });
    }

    //Metodo onclick del botón
    public void opcion1(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion1);
        opcion1.setEnabled(false);
        ((SQLiteDatabase) datos.get(2)).close();
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

    //Metodo onclick del botón
    public void opcion2(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion2);
        opcion2.setEnabled(false);
        ((SQLiteDatabase) datos.get(2)).close();
    }

    //Metodo onclick del botón
    public void opcion3(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion3);
        opcion3.setEnabled(false);
        ((SQLiteDatabase) datos.get(2)).close();
    }

    public void verificarRespuesta(List datos, ImageButton opcion) {
        if ((opcion.getTag().toString().split("_"))[0].equals(nombreSubcategoria)) {
            //Es correcta la opcion
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else {
            //Es incorrecta
            incorrecto((String) datos.get(0), (String) datos.get(1));
        }
    }

    //Este metodo es cuando el niño selecciona incorrecta la  opción
    public void incorrecto(String cantidad_preguntas, String cantidad_errores) {
        handler.removeCallbacksAndMessages(null);
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        if (estadoEstadistica.equals("0")) {
            r.ponerErroresEstadisticas(1, 0, db, id_subcategoria, id_usuario);
        }
        r.actualizarProgreso(Integer.parseInt(cantidad_preguntas), Integer.parseInt(cantidad_errores) + 1, db, id_subcategoria, id_usuario);
        String tipo_genero = (genero.equals("M")) ? "general/intentar_m.mp3" : "general/intentar_f.mp3";
       audio.reset();
        r.audioMostrar(tipo_genero, audio, amanager, this);
        tocarPantalla.removeCallbacksAndMessages(null);
        ejecutar();
        db.close();
    }

    public void verificarErrores(Cursor cursor, SQLiteDatabase db, int cantidad_preguntas, int cantidad_errores) {
        audio.reset();
        handler.removeCallbacksAndMessages(null);
        Cursor pregunta = db.rawQuery("select id_pregunta from Persona_Pregunta where id_persona = " + id_usuario, null);
        r.noRepetir(pregunta, db, id_usuario, id_pregunta);
        if (cursor.moveToFirst()) {
            cantidad_preguntas -= 1;
            List<String> subcategoria = new ArrayList<>();
            subcategoria.add(id_subcategoria);
            subcategoria.add(nombreSubcategoria.substring(0, 1).toUpperCase() + nombreSubcategoria.substring(1));
            if (cantidad_preguntas <= 0) {
                if (cantidad_errores >= 6 || cantidad_errores == 5) {
                    cantidad_preguntas = 3;
                    if (estadoEstadistica.equals("0")) {
                        r.ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirFigurasGeometricas();
                } else if (cantidad_errores == 4 || cantidad_errores == 3) {
                    cantidad_preguntas = 2;
                    if (estadoEstadistica.equals("0")) {
                        r.ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirFigurasGeometricas();
                } else if (cantidad_errores == 2 || cantidad_errores == 1) {
                    cantidad_preguntas = 1;
                    if (estadoEstadistica.equals("0")) {
                        r.ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirFigurasGeometricas();
                } else {
                    //Excelente paso  la subcategoria
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    r.actualizarEstadoProgreso(db, id_subcategoria, id_usuario, estadoEstadistica);
                    r.actualizarEstadisticaTema(db, id_subcategoria, id_usuario);
                    if (estadoEstadistica.equals("1")) {
                        String strSQL1 = "UPDATE Progreso SET repeticion = " + 1 + " WHERE id_persona = "
                                + id_usuario + " and " + " id_subcategoria= " + id_subcategoria;
                        db.execSQL(strSQL1);
                    }
                    int resultado = obtenerSiguienteSubctegoria(db);
                    if (resultado != 0) {
                        if (estadoEstadistica.equals("0")) {
                            r.insertarNuevaSubCategoria(resultado, db, id_usuario);
                        } else {
                            String strSQL = "UPDATE Progreso SET repeticion = " + 2 + " WHERE id_persona = "
                                    + id_usuario + " and " + " id_subcategoria= " + resultado;
                            db.execSQL(strSQL);
                        }
                        Cursor est = r.verificarDatosTablaEstadistica(db, id_subcategoria, id_usuario);
                        id_subcategoria = "" + resultado;
                        if (est.getCount() <= 0) {
                            DBHandler mdb = new DBHandler(getApplicationContext());
                            r.insertarEstadistica(mdb, id_subcategoria, id_usuario);
                        } else {
                            est.moveToFirst();
                            estadoEstadistica = est.getString(est.getColumnIndex("estado"));
                        }
                        String tipo_genero = (genero.equals("M")) ? "general/tema_superado_m.mp3" : "general/tema_superado_f.mp3";
                        r.audioMostrar(tipo_genero, audio, amanager, this);
                        met = new Runnable() {
                            public void run() {
                                abrirFigurasGeometricas();
                            }
                        };
                        handler.postDelayed(met, 5000);

                    } else {
                        String strSQL1 = "UPDATE Progreso SET  cantidad_errores= 0, cantidad_preguntas=3, repeticion = 0 WHERE id_persona = "
                                + id_usuario + " and " + " id_subcategoria >=44 and id_subcategoria <=47";
                        db.execSQL(strSQL1);
                        if (estadoEstadistica.equals("0")) {
                            String tipo_genero = (genero.equals("M")) ? "general/abecedario_m.mp3" : "general/abecedario_f.mp3";
                            audio.reset();
                            audioMostrar(tipo_genero, audio, amanager, this);
                            met = new Runnable() {
                                public void run() {
                                    DBHandler mdb = new DBHandler(getApplicationContext());
                                    SQLiteDatabase db1 = mdb.getWritableDatabase();
                                    abrirAbecedario(db1);
                                    db1.close();
                                }
                            };
                            handler.postDelayed(met, 5000);
                        } else {
                            procederAbecedario();
                        }
                        return;
                        //Insertar en la tabla de progreso  la primer subcategoria de colores
                    }
                }
            } else {
                r.actualizarProgreso(cantidad_preguntas, cantidad_errores, db, id_subcategoria, id_usuario);
                abrirFigurasGeometricas();
            }
        }
    }

    //Este metodo se encarga de abrir las colores y de actualizar los datos
    private void procederAbecedario() {
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        Intent numeroActividad = new Intent(Figuras_geometricas.this, Abecedario.class);
        numeroActividad.putExtra("id_usuario", id_usuario);
        numeroActividad.putExtra("genero", genero);
        numeroActividad.putExtra("id_subcategoria", 48);
        startActivity(numeroActividad);
        finish();
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

    //Este metodo se encarga de abrir el tema del abecedario
    private void abrirAbecedario(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("id_persona", id_usuario);
        values.put("id_subcategoria", 48);
        values.put("cantidad_preguntas", 3);
        values.put("estado", false);
        values.put("cantidad_errores", 0);
        values.put("repeticion", 0);
        db.insert("Progreso", null, values);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        Intent geometria = new Intent(Figuras_geometricas.this, Abecedario.class);
        geometria.putExtra("id_usuario", id_usuario);
        geometria.putExtra("genero", genero);
        geometria.putExtra("id_subcategoria", 48);
        startActivity(geometria);
        finish();
    }

    //Obtiene la siguiente subcategoria
    private int obtenerSiguienteSubctegoria(SQLiteDatabase db) {
        int resultado = 0;

        List datos = obtener(db, nombreSubcategoria);
        String[] listaDisponible = (String[]) datos.get(1);
        int cantidad = r.verificarCantidadArreglo(listaDisponible);
        listaDisponible = r.eliminarValoresArreglo((String[]) datos.get(1), cantidad);
        cantidad = r.verificarCantidadArreglo(listaDisponible);

        if (cantidad == 0) {
            return 0;
        } else {
            resultado = Integer.parseInt(listaDisponible[0]);
        }
        return resultado;
    }

    private List obtener(SQLiteDatabase db, String nombreSubcategoria) {
        String[] listaDisponible = new String[]{"44", "45", "46", "47"};
        Cursor subcategoriasProgreso = null;
        if (estadoEstadistica.equals("1")) {
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria >= " + 44 + " and " + " id_subcategoria <= 47" + " and " + " id_persona= " + id_usuario + " and repeticion= 1", null);
        } else {
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria >= " + 44 + " and id_subcategoria <= 47 " + " and " + " id_persona= " + id_usuario, null);
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
        List datos = new ArrayList();
        datos.add(0, subcategoriasProgreso);
        datos.add(1, listaDisponible);
        subcategoriasProgreso.close();
        return datos;
    }

    //Ejecuta las respuestas
    public void ejecutarReproduccionAudio() {
        establecerRespuesta(audiogeneral, nombreSubcategoria);
        //obtiene los audios de las respuestas de la pregunta
        String[] respuestaAudio = obtenerAudiosRespuesta("figuras_geometricas/audio_respuesta_figuras_geometricas/", audiogeneral, nombreSubcategoria);
        //Reproduce cada unos de los audios de las respuestas
        establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, -1);
    }

    //Reproducción de audios de respuesta
    public void establecerAudiosRespuesta(final String[] respuestaAudio, final String nombreSubcategoria, final int contador) {
        int i = contador;
        i++;
        final int c = i;
        met = new Runnable() {
            public void run() {
                reproducirAudio("figuras_geometricas/audio_respuesta_figuras_geometricas/" + respuestaAudio[c], "r", respuesta, amanager);
                if (c != 2) {
                    establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, c);
                    handler.postDelayed(met, 3000);
                } else {
                    met = new Runnable() {
                        public void run() {
                            hacerAudio();
                            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            finalPregunta = false;
                            ejecutar();
                        }
                    };
                    handler.postDelayed(met, 2500);
                }
            }
        };
        if (c == 0) {
            handler.postDelayed(met, 3000);
        }

    }

    //Este metodo se encarga de reproducir los audios de las respuestas
    public String[] obtenerAudiosRespuesta(String tema, String audio, String nombreSubcategoria) {
        String[] direccionAudios = new String[3];
        String[] respuesta = opcion1.getTag().toString().split("_");
        direccionAudios[0] = respuesta[0] + ".mp3";
        respuesta = opcion2.getTag().toString().split("_");
        direccionAudios[1] = respuesta[0] + ".mp3";
        respuesta = opcion3.getTag().toString().split("_");
        direccionAudios[2] = respuesta[0] + ".mp3";
        return direccionAudios;
    }

    //Establece los colores de los botones de las respuestas
    public void establecerRespuesta(String audio, String nombreSubcategoria) {
        String[] respuestas = new String[3];
        String[] temas = new String[]{"circulo", "cuadrado", "triangulo", "rectangulo"};
        String[] listaColores = new String[]{"amarillo", "azul", "rojo", "anaranjado", "verde"};
        String[] resultadoTemas = new String[4];
        String[] resultadoColores = new String[4];
        String[] resultadoNumeros = new String[32];
        String[] resultado = new String[4];
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select count(*) as resultado" + " from Progreso " +
                " where id_subcategoria >= 13 and id_subcategoria <= 43 and " + " id_persona= " + id_usuario, null);
        int cantidad = 0;
        if (cursor.moveToFirst()) {
            cantidad = Integer.parseInt(cursor.getString(cursor.getColumnIndex("resultado")));
        }
        cursor.close();
        db.close();
        for (int i = 0; i < temas.length; i++) {
            if (nombreSubcategoria.equals(temas[i])) {
                temas[i] = "";
            }
        }
        int elementos = r.verificarCantidadArreglo(temas);
        temas = r.eliminarValoresArreglo(temas, elementos);
        //hacer el random para lo temas
        resultadoTemas = r.obtenerNumeros(2);
        //hacer el random para los colores
        resultadoColores = r.obtenerNumeros(4);
        if (cantidad == 10) {
            resultadoNumeros = r.obtenerNumeros(9);
        } else if (cantidad == 20) {
            resultadoNumeros = r.obtenerNumeros(19);
        } else if (cantidad == 31) {
            resultadoNumeros = r.obtenerNumeros(30);
        }
        resultado[0] = "figuras_geometricas/respuesta_" + temas[Integer.parseInt(resultadoTemas[0])] + "/" + temas[Integer.parseInt(resultadoTemas[0])] + "_" + listaColores[Integer.parseInt(resultadoColores[0])] + "_" + resultadoNumeros[0] + ".png";
        resultado[1] = "figuras_geometricas/respuesta_" + temas[Integer.parseInt(resultadoTemas[1])] + "/" + temas[Integer.parseInt(resultadoTemas[1])] + "_" + listaColores[Integer.parseInt(resultadoColores[1])] + "_" + resultadoNumeros[1] + ".png";
        resultado[2] = "figuras_geometricas/respuesta_" + nombreSubcategoria + "/" + nombreSubcategoria + "_" + listaColores[Integer.parseInt(resultadoColores[2])] + "_" + resultadoNumeros[2] + ".png";
        respuestas = r.obtenerNumeros(2);
        Drawable imagen = null;
        imagen = establecerImagen(resultado[Integer.parseInt(respuestas[0])].trim(), this);
        String[] r = resultado[Integer.parseInt(respuestas[0])].split("/");
        opcion1.setImageDrawable(imagen);
        opcion1.setTag(r[2]);
        imagen = establecerImagen(resultado[Integer.parseInt(respuestas[1])].trim(), this);
        r = resultado[Integer.parseInt(respuestas[1])].split("/");
        opcion2.setImageDrawable(imagen);
        opcion2.setTag(r[2]);
        r = resultado[Integer.parseInt(respuestas[2])].split("/");
        imagen = establecerImagen(resultado[Integer.parseInt(respuestas[2])].trim(), this);
        opcion3.setImageDrawable(imagen);
        opcion3.setTag(r[2]);
        mostrarBotones();
    }

    //Realiza las preguntas segun tema
    public void realizarPreguntas(Cursor cursor, Cursor cursor1, String nombre_subcategoria, DBHandler mdb, Context contexto) {
        String[] nombreImagen = new String[11];
        String[] audio = new String[11];
        String[] id_p = new String[11];
        int contador = 0;
        MediaPlayer pregunta = new MediaPlayer();
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

        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor est = r.verificarDatosTablaEstadistica(db, this.id_subcategoria, id_usuario);
        if (est.getCount() <= 0) {
            r.insertarEstadistica(mdb, id_subcategoria, id_usuario);
            estadoEstadistica = "0";
        } else {
            est.moveToFirst();
            estadoEstadistica = est.getString(est.getColumnIndex("estado"));
        }
        db.close();
        est.close();
        int cantidad = r.verificarCantidadArreglo(nombreImagen);
        nombreImagen = r.eliminarValoresArreglo(nombreImagen, cantidad);
        audio = r.eliminarValoresArreglo(audio, cantidad);
        id_p = r.eliminarValoresArreglo(id_p, cantidad);
        int d = r.verificarCantidadArreglo(nombreImagen);
        //si r es igual a cero se acabaron las preguntas
        if (d == 0) {
            //Se elimina las preguntas realizadas y se inserta en la tabla de estadisticas
            r.eliminarPreguntasRealizada(mdb, id_subcategoria, id_usuario);
            abrirFigurasGeometricas();
        } else {
            int numero = r.sortear(d);
            this.id_pregunta = id_p[numero];
            //Obtiene la imagen de la pregunta
            Drawable resultado = establecerImagen(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "figuras_geometricas/", contexto), contexto);
            img.setImageDrawable(resultado);

            pregunta = reproducirAudio(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "figuras_geometricas/audios_preguntas_figuras_geometricas/", contexto),
                    "", null, this.amanager);
            audiogeneral = audio[numero];
            nombreSubcategoria = nombre_subcategoria;
        }
    }

    //Reproduce el audio de la pregunta de la imagen
    public MediaPlayer reproducirAudio(String audio, String tipo, MediaPlayer respuesta, AudioManager amanager) {
        this.amanager = amanager;
        this.amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        if (respuesta == null) {
//            pregunta = new MediaPlayer();
            try {
                AssetFileDescriptor afd = this.getAssets().openFd(audio.trim());
                pregunta.setDataSource(
                        afd.getFileDescriptor(),
                        afd.getStartOffset(),
                        afd.getLength()
                );
                afd.close();
                pregunta.prepare();
                pregunta.setVolume(1, 1);
                pregunta.start();
                return pregunta;
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else {

            try {
                respuesta = new MediaPlayer();
                AssetFileDescriptor recurso = this.getAssets().openFd(audio.trim());
                respuesta.setDataSource(
                        recurso.getFileDescriptor(),
                        recurso.getStartOffset(),
                        recurso.getLength()
                );

                recurso.close();
                respuesta.prepare();
                respuesta.setVolume(1, 1);
                respuesta.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return respuesta;
        }
        return pregunta;
    }

    //Este metodo estable en el image view la imagen
    public Drawable establecerImagen(String nombreImagen, Context c) {
        Drawable d = null;
        try {
            InputStream ims = getAssets().open(nombreImagen);
            d = Drawable.createFromStream(ims, null);
            ims.close();
        } catch (IOException ex) {

        }
        return d;
    }

    public String obtenerArchivoPregunta(String nombreImagen, String audio, String nombreSubcategoria, String direccion, Context c) {
        String direccionImagen = "";
        if (direccion.equals("figuras_geometricas/")) {
            direccionImagen = direccion + "pregunta_" + nombreSubcategoria + "/" + nombreImagen;
        } else {
            direccionImagen = direccion + nombreSubcategoria + "/" + audio + ".mp3";
        }
        return direccionImagen;
    }

    //Vuelve abrir la actividad de figuras geometricas
    private void abrirFigurasGeometricas() {
        Intent figuras_geometricas = new Intent(Figuras_geometricas.this, Figuras_geometricas.class);
        figuras_geometricas.putExtra("id_usuario", id_usuario);
        figuras_geometricas.putExtra("genero", genero);
        figuras_geometricas.putExtra("id_subcategoria", id_subcategoria);
        startActivity(figuras_geometricas);
        finish();
    }

    private String volverJugarTema(SQLiteDatabase db) {
        String nombreSubcategoria = "";
        Cursor rep = db.rawQuery("select id from  Progreso " +
                " where id_subcategoria >=44 and id_subcategoria <=47 and " + "id_persona = "
                + id_usuario + " and repeticion= 1", null);
        Cursor repeticion = db.rawQuery("select id_subcategoria from  Progreso " +
                " where id_subcategoria >=44 and id_subcategoria <=47 and " + "id_persona = "
                + id_usuario + " and repeticion= 2", null);
        if (repeticion.getCount() <= 0) {
            if (rep.getCount() == 7) {
                String strSQL1 = "UPDATE Progreso SET cantidad_preguntas = 3, cantidad_errores = 0, repeticion = 0 WHERE id_persona = "
                        + id_usuario + " and " + " id_subcategoria >=44 and id_subcategoria <=47";
                db.execSQL(strSQL1);
            }
            int id = r.sortear(3);
            if (id == 0) {
                id_subcategoria = "44";
                nombreSubcategoria = "Circulo";
            } else if (id == 1) {
                id_subcategoria = "45";
                nombreSubcategoria = "Triangulo";
            } else if (id == 2) {
                id_subcategoria = "46";
                nombreSubcategoria = "Rectangulo";
            } else if (id == 3) {
                id_subcategoria = "47";
                nombreSubcategoria = "Cuadrado";
            }
            String strSQL = "UPDATE Progreso SET cantidad_preguntas = 3, cantidad_errores = 0,repeticion=2 WHERE id_persona = "
                    + id_usuario + " and " + " id_subcategoria= " + id_subcategoria + "";
            db.execSQL(strSQL);
        } else {
            if (repeticion.moveToFirst()) {
                id_subcategoria = repeticion.getString(repeticion.getColumnIndex("id_subcategoria"));
                if (id_subcategoria.equals("44")) {
                    nombreSubcategoria = "Circulo";
                } else if (id_subcategoria.equals("45")) {
                    nombreSubcategoria = "Triangulo";
                } else if (id_subcategoria.equals("46")) {
                    nombreSubcategoria = "Rectangulo";
                } else if (id_subcategoria.equals("47")) {
                    nombreSubcategoria = "Cuadrado";
                }
            }
        }
        rep.close();
        repeticion.close();
        return nombreSubcategoria;
    }

    //Verificar el tipo de subcategoria
    public void verificarTipoSubcategoria(List<String> subcategoria, DBHandler mdb, Context contexto) {
        SQLiteDatabase db = mdb.getWritableDatabase();
        if (subcategoria.size() != 0) {
            id_subcategoria = subcategoria.get(0);
            if (Integer.parseInt(id_subcategoria) >= 44 && Integer.parseInt(id_subcategoria) <= 47) {
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
        if ((subcategoria.get(1)).trim().equals("Circulo")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "circulo", mdb, contexto);
        } else if ((subcategoria.get(1)).trim().equals("Triangulo")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "triangulo", mdb, contexto);
        } else if ((subcategoria.get(1)).trim().equals("Rectangulo")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "rectangulo", mdb, contexto);
        } else if ((subcategoria.get(1)).trim().equals("Cuadrado")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "cuadrado", mdb, contexto);
        }
        db.close();
    }

    //Evento si se realiza un retroceso desde el dispositivo
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Figuras_geometricas.this, MenuJuego.class);
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
    protected void onResume() {
        super.onResume();
        if (finalPregunta == true) {
            ejecutarReproduccionAudio();
        }  else if (pausa == 3) {
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

    //Reconocimiento de vooz
    public SpeechRecognizer hacerAudio() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        speech.startListening(recognizerIntent);
        return speech;
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
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String texto = "";
        for (String result : matches)
            if (result.equals("Atras") || result.equals("atras") || result.equals("anterior") || result.equals("Anterior")) {
                Intent menu = new Intent(Figuras_geometricas.this, MenuJuego.class);
                menu.putExtra("id_usuario", id_usuario);
                menu.putExtra("genero", genero);
                startActivity(menu);
                finish();
            } else {
                verificarReconocimientoVoz(result);
            }
        speech.destroy();
        hacerAudio();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    public static String limpiarString(String texto) {
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return texto;
    }

    //Este metodo se encarga de validar el reconocimiento de voz
    private void verificarReconocimientoVoz(String texto) {
        String nombreColor = "";
        String numero = "";
        texto = limpiarString(texto);
        if (opcion1.getTag().toString().split("_")[0].equals(nombreSubcategoria)) {
            nombreColor = opcion1.getTag().toString().split("_")[1];
            numero = opcion1.getTag().toString().split("_")[2];
        } else if (opcion2.getTag().toString().split("_")[0].equals(nombreSubcategoria)) {
            nombreColor = opcion2.getTag().toString().split("_")[1];
            numero = opcion2.getTag().toString().split("_")[2];
        } else if (opcion3.getTag().toString().split("_")[0].equals(nombreSubcategoria)) {
            nombreColor = opcion3.getTag().toString().split("_")[1];
            numero = opcion3.getTag().toString().split("_")[2];
        }
        if (nombreSubcategoria.equals(texto) || nombreColor.equals(texto) || numero.equals(texto + ".png")) {
            List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
            //Correcto
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else if (opcion1.getTag().toString().split("_")[0].equals(texto) || opcion1.getTag().toString().split("_")[1].equals(texto) || opcion1.getTag().toString().split("_")[2].equals(texto + ".png")
                || opcion2.getTag().toString().split("_")[0].equals(texto) || opcion2.getTag().toString().split("_")[1].equals(texto) || opcion2.getTag().toString().split("_")[2].equals(texto + ".png")
                || opcion3.getTag().toString().split("_")[0].equals(texto) || opcion3.getTag().toString().split("_")[1].equals(texto) || opcion3.getTag().toString().split("_")[2].equals(texto + ".png")) {
            //incorrecto
            List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
            incorrecto((String) datos.get(0), (String) datos.get(1));
        }
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

    //Reproduce el audio si el usuario no toca la pantalla en 12 segundos
    private void reproducir(String direccion) {
        audio.reset();
        try {
            AssetFileDescriptor afd = getAssets().openFd(direccion);
            audio.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            audio.prepare();
            audio.setVolume(1, 1);
            audio.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //animari botones
    private void animar(ImageButton btn) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(2000);
        //fadeIn.setRepeatCount(ValueAnimator.INFINITE);
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setStartOffset(2000);
        fadeOut.setDuration(1000);
        fadeOut.setRepeatCount(ValueAnimator.INFINITE);
        AnimationSet animation = new AnimationSet(true);
        animation.addAnimation(fadeIn);
        animation.addAnimation(fadeOut);
        btn.startAnimation(animation);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int eventaction = event.getAction();
        if (eventaction == MotionEvent.ACTION_DOWN) {
            eventoTocar = true;
        }
        return true;

    }

    @Override
    protected void onDestroy() {
        tocarPantalla.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    //Metodo detecta cuando cambia de horientación
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            handler.removeCallbacks(met);
            //respuesta.release();
            abrirFigurasGeometricas();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            handler.removeCallbacks(met);
            //respuesta.release();
            abrirFigurasGeometricas();
        }
    }
}
