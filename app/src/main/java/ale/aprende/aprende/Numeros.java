package ale.aprende.aprende;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ale.aprende.aprende.bd.DBHandler;

public class Numeros extends AppCompatActivity implements RecognitionListener {
    //Declaración de variables
    private int id_usuario = 0;
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
    Runnable met;
    Boolean finalPregunta = false;
    Colores c = new Colores();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_numeros);
        id_usuario = getIntent().getExtras().getInt("id_usuario");
        genero = getIntent().getExtras().getString("genero");
        id_subcategoria = getIntent().getExtras().getString("id_subcategoria");
        img = (ImageView) findViewById(R.id.imgPregunta);
        opcion1 = (ImageButton) findViewById(R.id.imgBtnPrimeraOpcion);
        opcion2 = (ImageButton) findViewById(R.id.imgBtnSegundaOpcion);
        opcion3 = (ImageButton) findViewById(R.id.imgBtnTerceraOpcion);
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
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

    //Evento click de la primera opcion
    public void opcion1(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion1);
        opcion1.setEnabled(false);
    }

    //Evento click de la primera opcion
    public void opcion2(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion2);
        opcion2.setEnabled(false);
    }

    //Evento click de la primera opcion
    public void opcion3(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion3);
        opcion3.setEnabled(false);
    }

    //Este metodo verifica si la opcion seleccionada es la correcta
    private void verificarRespuesta(List datos, ImageButton opcion) {
        String[] nombre = opcion.getTag().toString().split("_");
        if (nombre[1].equals(nombreSubcategoria)) {
            //Es correcta la opcion
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else {
            //Es incorrecta
            incorrecto((String) datos.get(0), (String) datos.get(1));
        }
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
                    abrirNumeros();
                } else if (cantidad_errores == 4 || cantidad_errores == 3) {
                    cantidad_preguntas = 2;
                    if (estadoEstadistica.equals("0")) {
                        r.ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirNumeros();
                } else if (cantidad_errores == 2 || cantidad_errores == 1) {
                    cantidad_preguntas = 1;
                    if (estadoEstadistica.equals("0")) {
                        r.ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirNumeros();
                } else {
                    //Excelente paso  la subcategoria
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    r.actualizarEstadoProgreso(db, id_subcategoria, id_usuario,estadoEstadistica);
                    r.actualizarEstadisticaTema(db, id_subcategoria, id_usuario);
                    int resultado = obtenerSiguienteSubctegoria(db);
                    if (resultado != 0) {
                        r.insertarNuevaSubCategoria(resultado, db, id_usuario);
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
                                abrirNumeros();
                            }
                        };
                        handler.postDelayed(met, 5000);

                    } else {
                        Toast.makeText(this, "Insertar figuras geometricas", Toast.LENGTH_SHORT).show();
                        return;
                        //Insertar en la tabla de progreso  la primer subcategoria de colores
                    }
                }
            } else {
                r.actualizarProgreso(cantidad_preguntas, cantidad_errores, db, id_subcategoria, id_usuario);
                abrirNumeros();
            }
        }
    }

    //Obtiene la siguiente subcategoria
    private int obtenerSiguienteSubctegoria(SQLiteDatabase db) {
        int resultado = 0;
        if (nombreSubcategoria.equals("9") || nombreSubcategoria.equals("19")) {
            //Aplica la logica
            Cursor datosEstadisticas = null;
            if (nombreSubcategoria.equals("9")) {
                datosEstadisticas = db.rawQuery("select sum(cantidad_preguntas) as cantidad_preguntas" + " from Estadistica " +
                        " where id_subcategoria >= " + 13 + " and id_subcategoria <= 22 " + " and " + " id_persona= " + id_usuario, null);
            } else {
                datosEstadisticas = db.rawQuery("select sum(cantidad_preguntas) as cantidad_preguntas " + " from Estadistica " +
                        " where id_subcategoria >= " + 23 + " and id_subcategoria <= 32 " + " and " + " id_persona= " + id_usuario, null);
            }
            if (datosEstadisticas.moveToFirst()) {
                int cantidad_preguntas = Integer.parseInt(datosEstadisticas.getString(datosEstadisticas.getColumnIndex("cantidad_preguntas")));

                int sobra = cantidad_preguntas - 30;
                double total = sobra * 3.33;
                total = 100 - total;
                if (total < 70) {
                    return resultado;
                } else {
                    int id = Integer.parseInt(nombreSubcategoria) + 1;
                    List datos = obtener(db, id + "");
                    String[] listaDisponible = (String[]) datos.get(1);
                    int cantidad = r.verificarCantidadArreglo(listaDisponible);
                    listaDisponible = r.eliminarValoresArreglo((String[]) datos.get(1), cantidad);
                    cantidad = r.verificarCantidadArreglo(listaDisponible);

                    if (cantidad == 0) {
                        return resultado;
                    } else {
                        resultado = Integer.parseInt(listaDisponible[0]);
                    }
                }
            }


        } else {
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
        }

        return resultado;
    }

    private List obtener(SQLiteDatabase db, String nombreSubcategoria) {
        String[] listaDisponible = new String[13];
        Cursor subcategoriasProgreso = null;
        if (Integer.parseInt(nombreSubcategoria) <= 9) {
            //13 a la 22
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria >= " + 13 + " and id_subcategoria <= 22 " + " and " + " id_persona= " + id_usuario, null);
            listaDisponible = new String[]{"13", "14", "15", "16", "17", "18", "19", "20", "21", "22"};
        } else if (Integer.parseInt(nombreSubcategoria) > 9 && Integer.parseInt(nombreSubcategoria) <= 19) {
            //23 a la 32
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria >= " + 23 + " and id_subcategoria <= 32 " + " and " + " id_persona= " + id_usuario, null);
            listaDisponible = new String[]{"23", "24", "25", "26", "27", "28", "29", "30", "31", "32"};
        } else if (Integer.parseInt(nombreSubcategoria) > 19) {
            //33 a la 43
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria >= " + 33 + " and id_subcategoria <= 43 " + " and " + " id_persona= " + id_usuario, null);
            listaDisponible = new String[]{"33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43"};
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

    //Este metodo es cuando el niño selecciona incorrecta la  opción
    public void incorrecto(String cantidad_preguntas, String cantidad_errores) {
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        if (estadoEstadistica.equals("0")) {
            r.ponerErroresEstadisticas(1, 0, db, id_subcategoria, id_usuario);
        }
        r.actualizarProgreso(Integer.parseInt(cantidad_preguntas), Integer.parseInt(cantidad_errores) + 1, db, id_subcategoria, id_usuario);
        String tipo_genero = (genero.equals("M")) ? "general/intentar_m.mp3" : "general/intentar_f.mp3";
        r.audioMostrar(tipo_genero, audio, amanager, this);
    }

    //Establece los colores de los botones de las respuestas
    public void establecerRespuesta(String audio, String nombreSubcategoria) {
        String[] ordenBotones = new String[3];
        String[] nombre = new String[3];
        String[] listaColores = {"azul", "amarillo", "rojo", "verde", "naranja"};
        String[] combinacionColores = r.obtenerNumeros(4);
        ordenBotones = r.obtenerNumeros(2);

        Drawable[] archivo = new Drawable[3];
        int numero = r.sortear(3);
        int incorrectas = 0;
        String resultado = "";
        archivo[0] = obtenerImagenRespuestas("numero/r" + nombreSubcategoria + "/" + listaColores[Integer.parseInt(combinacionColores[0])] + "_" + nombreSubcategoria + ".png");
        nombre[0] = listaColores[Integer.parseInt(combinacionColores[0])] + "_" + nombreSubcategoria;
        String[] lista = r.obtenerNumeros(9);
        for (int i = 1; i <= 2; i++) {
            if (Integer.parseInt(nombreSubcategoria) <= 9) {
                if (lista[i].equals(nombreSubcategoria)) {
                    lista[i] = "";
                    int cantidad = r.verificarCantidadArreglo(lista);
                    lista = r.eliminarValoresArreglo(lista, cantidad);
                    i--;
                } else {
                    numero = r.sortear(3);
                    incorrectas = Integer.parseInt(lista[i]);
                    resultado = "" + incorrectas;
                    archivo[i] = obtenerImagenRespuestas("numero/r" + resultado + "/" + listaColores[Integer.parseInt(combinacionColores[i])] + "_" + resultado + ".png");
                    nombre[i] = listaColores[Integer.parseInt(combinacionColores[i])] + "_" + resultado;
                }
            } else if (Integer.parseInt(nombreSubcategoria) > 9 && Integer.parseInt(nombreSubcategoria) <= 19) {
                if (lista[i].equals(nombreSubcategoria)) {
                    lista[i] = "";
                    int cantidad = r.verificarCantidadArreglo(lista);
                    lista = r.eliminarValoresArreglo(lista, cantidad);
                    i--;
                } else {
                    numero = r.sortear(3);
                    incorrectas = Integer.parseInt(lista[i]);
                    resultado = "1" + incorrectas;
                    archivo[i] = obtenerImagenRespuestas("numero/r" + resultado + "/" + listaColores[Integer.parseInt(combinacionColores[i])] + "_" + resultado + ".png");
                    nombre[i] = listaColores[Integer.parseInt(combinacionColores[i])] + "_" + resultado;
                }
            } else if (Integer.parseInt(nombreSubcategoria) > 19 && Integer.parseInt(nombreSubcategoria) <= 30) {
                if (lista[i].equals(nombreSubcategoria)) {
                    lista[i] = "";
                    int cantidad = r.verificarCantidadArreglo(lista);
                    lista = r.eliminarValoresArreglo(lista, cantidad);
                    i--;
                } else {
                    numero = r.sortear(3);
                    incorrectas = Integer.parseInt(lista[i]);
                    resultado = (incorrectas == 10) ? "30" : "2" + incorrectas;
                    archivo[i] = obtenerImagenRespuestas("numero/r" + resultado + "/" + listaColores[Integer.parseInt(combinacionColores[i])] + "_" + resultado + ".png");
                    nombre[i] = listaColores[Integer.parseInt(combinacionColores[i])] + "_" + resultado;
                }

            }
        }

        opcion1.setImageDrawable(archivo[Integer.parseInt(ordenBotones[0])]);
        opcion1.setTag(nombre[Integer.parseInt(ordenBotones[0])]);
        opcion2.setImageDrawable(archivo[Integer.parseInt(ordenBotones[1])]);
        opcion2.setTag(nombre[Integer.parseInt(ordenBotones[1])]);
        opcion3.setImageDrawable(archivo[Integer.parseInt(ordenBotones[2])]);
        opcion3.setTag(nombre[Integer.parseInt(ordenBotones[2])]);
        mostrarBotones();
    }

    //Esta imagen obtiene los archivos que pertenece a las respuestas de las preguntas
    public Drawable obtenerImagenRespuestas(String direccion) {
        AssetManager assetManager = getApplicationContext().getAssets();
        String[] archivos = new String[0];
        Drawable d = null;
        try {
            InputStream ims = getAssets().open(direccion);
            d = Drawable.createFromStream(ims, null);
            //opcion1.setImageDrawable(d);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return d;
    }

    //Ejecuta las respuestas
    public void ejecutarReproduccionAudio() {
        establecerRespuesta(audiogeneral, nombreSubcategoria);
        //obtiene los audios de las respuestas de la pregunta
        String[] respuestaAudio = obtenerAudiosRespuesta("numero/audios_respuesta_numeros/", audiogeneral, nombreSubcategoria);
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
                reproducirAudio("numero/audios_respuestas_numeros/" + respuestaAudio[c], "r", respuesta, amanager);
                if (c != 2) {
                    establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, c);
                    handler.postDelayed(met, 3000);
                } else {
                    met = new Runnable() {
                        public void run() {
                            hacerAudio();
                            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            finalPregunta = false;
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
        String[] resultado = opcion1.getTag().toString().split("_");
        //direccionAudios[0] = "r" + resultado[0].substring(1, resultado.length) + ".mp3";
        direccionAudios[0] = "r" + resultado[1] + ".mp3";
        String[] resultado1 = opcion2.getTag().toString().split("_");
        direccionAudios[1] = "r" + resultado1[1] + ".mp3";
        //direccionAudios[1] = "r" + resultado1[0].substring(1, resultado1.length) + ".mp3";
        String[] resultado2 = opcion3.getTag().toString().split("_");
        // direccionAudios[2] = "r" + resultado2[0].substring(1, resultado2.length) + ".mp3";
        direccionAudios[2] = "r" + resultado2[1] + ".mp3";
        return direccionAudios;
    }

    //Verificar el tipo de subcategoria
    public void verificarTipoSubcategoria(List<String> subcategoria, DBHandler mdb, Context
            contexto) {
        SQLiteDatabase db = mdb.getWritableDatabase();
        id_subcategoria = subcategoria.get(0);
        List datos = new ArrayList<>();
        for (int i = 0; i <= 30; i++) {
            if (subcategoria.get(1).trim().equals("" + i)) {
                Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)), mdb);
                Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
                realizarPreguntas(cursor, cursor1, "" + i, mdb, contexto);
                break;
            }
        }
        db.close();
    }

    //Realiza las preguntas segun tema
    public void realizarPreguntas(Cursor cursor, Cursor cursor1, String
            nombre_subcategoria, DBHandler mdb, Context contexto) {
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
            abrirNumeros();
        } else {
            int numero = r.sortear(d);
            this.id_pregunta = id_p[numero];
            //Obtiene la imagen de la pregunta
            Drawable resultado = establecerImagen(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "numero/"), contexto);
            img.setImageDrawable(resultado);

            pregunta = reproducirAudio(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "numero/audio_pregunta_numero/"),
                    "", null, this.amanager);
            audiogeneral = audio[numero];
            nombreSubcategoria = nombre_subcategoria;
        }
    } //Este metodo estable en el image view la imagen

    public Drawable establecerImagen(String nombreImagen, Context c) {
        Drawable d = null;
        try {
            InputStream ims = c.getAssets().open(nombreImagen);
            d = Drawable.createFromStream(ims, null);
            ims.close();
        } catch (IOException ex) {

        }
        return d;
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

    public String obtenerArchivoPregunta(String nombreImagen, String audio, String
            nombreSubcategoria, String direccion) {
        AssetManager assetManager = getApplicationContext().getAssets();
        String direccionImagen = "";

        if (direccion.equals("numero/")) {
            direccionImagen = direccion + "p" + nombreSubcategoria + "/" + nombreImagen + ".png";

        } else {
            direccionImagen = direccion + audio + ".mp3";
        }
        return direccionImagen;
    }

    //Abre la actividad de numeros
    private void abrirNumeros() {
        Intent abrir = new Intent(Numeros.this, Numeros.class);
        abrir.putExtra("id_usuario", id_usuario);
        abrir.putExtra("genero", genero);
        abrir.putExtra("id_subcategoria", id_subcategoria);
        startActivity(abrir);
    }

    //Este metodo se encarga de ocultar los botones
    public void ocultarBotones() {
        opcion1.setVisibility(View.INVISIBLE);
        opcion2.setVisibility(View.INVISIBLE);
        opcion3.setVisibility(View.INVISIBLE);
    }

    //Evento si se realiza un retroceso desde el dispositivo
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Numeros.this, MenuJuego.class);
        intent.putExtra("id_usuario", id_usuario);
        intent.putExtra("genero", genero);
        startActivity(intent);
        respuesta.release();
        pregunta.release();
        finish();
    }


    //Muestra los botones de la respuesta
    public void mostrarBotones() {
        opcion2.setVisibility(View.VISIBLE);
        opcion1.setVisibility(View.VISIBLE);
        opcion3.setVisibility(View.VISIBLE);
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
        } else if (pausa == 1) {
            pregunta.start();
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
            }
        } catch (Exception e) {
        }

        handler.removeCallbacksAndMessages(null);
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
                Intent menu = new Intent(Numeros.this, MenuJuego.class);
                menu.putExtra("id_usuario", id_usuario);
                menu.putExtra("genero", genero);
                startActivity(menu);
                finish();
            } else {
                verificarReconocimientoVoz(result);
            }
        hacerAudio();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    private void verificarReconocimientoVoz(String texto) {
        String nombreColor = "";
        String[] respuesta1 = opcion1.getTag().toString().split("_");
        if (texto.equals("anaranjado")) {
            texto = "naranja";
        }
        if (respuesta1[1].equals(nombreSubcategoria)) {
            nombreColor = respuesta1[0];
        }
        String[] respuesta2 = opcion2.getTag().toString().split("_");
        if (respuesta2[1].equals(nombreSubcategoria)) {
            nombreColor = respuesta2[0];
        }
        String[] respuesta3 = opcion3.getTag().toString().split("_");
        if (respuesta3[1].equals(nombreSubcategoria)) {
            nombreColor = respuesta3[0];
        }
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        if (texto.equals(nombreColor) || texto.equals(nombreSubcategoria)) {
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else if (texto.equals(respuesta1[0]) || texto.equals(respuesta1[1]) || texto.equals(respuesta2[0]) || texto.equals(respuesta2[1]) || texto.equals(respuesta3[0]) || texto.equals(respuesta3[1])) {
            incorrecto((String) datos.get(0), (String) datos.get(1));
        }
    }
}