package ale.aprende.aprende;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ale.aprende.aprende.bd.DBHandler;

public class Colores extends AppCompatActivity implements RecognitionListener {
    //declaración de variables
    private int id_usuario = 0;
    private String genero, id_subcategoria, id_pregunta, audiogeneral, nombreSubcategoria = "";
    final MediaPlayer respuesta = new MediaPlayer();
    MediaPlayer audio = new MediaPlayer();
    MediaPlayer pregunta = new MediaPlayer();
    ImageView img;
    Button opcion1, opcion2, opcion3;
    Relaciones_espaciales r = new Relaciones_espaciales();
    AudioManager amanager;
    int pausa = 0;
    private String estadoEstadistica = "1";
    public SpeechRecognizer speech;
    private Intent recognizerIntent;
    final Handler handler = new Handler();
    Runnable met;
    Boolean finalPregunta = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colores);
        id_usuario = getIntent().getExtras().getInt("id_usuario");
        genero = getIntent().getExtras().getString("genero");
        id_subcategoria = getIntent().getExtras().getString("id_subcategoria");
        img = (ImageView) findViewById(R.id.imgPregunta);
        opcion1 = (Button) findViewById(R.id.BtnPrimeraOpcion);
        opcion2 = (Button) findViewById(R.id.BtnSegundaOpcion);
        opcion3 = (Button) findViewById(R.id.BtnTerceraOpcion);
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        List datos = new ArrayList<>();
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
                // amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        });
    }

    //Evento click de la primera opcion
    public void opcion1(View view) {
        List datos = obtenerDatos();
        verificarRespuesta(datos, opcion1);
        opcion1.setEnabled(false);
    }

    //Evento click de la primera opcion
    public void opcion2(View view) {
        List datos = obtenerDatos();
        verificarRespuesta(datos, opcion2);
        opcion2.setEnabled(false);
    }

    //Evento click de la primera opcion
    public void opcion3(View view) {
        List datos = obtenerDatos();
        verificarRespuesta(datos, opcion3);
        opcion3.setEnabled(false);
    }

    //Este verifica si la respuesta esta incorrecta o correcta
    public void verificarRespuesta(List datos, Button opcion) {
        Drawable drawable = opcion.getBackground();
        int color = ((ColorDrawable) drawable).getColor();
        if (nombreSubcategoria.equals("rojo") && color == -1618884) {
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else if (nombreSubcategoria.equals("azul") && color == -11309570) {
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else if (nombreSubcategoria.equals("amarillo") && color == -932849) {
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else if (nombreSubcategoria.equals("anaranjado") && color == -39424) {
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else if (nombreSubcategoria.equals("verde") && color == -14176672) {
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else {
            incorrecto((String) datos.get(0), (String) datos.get(1));
        }
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

    // segun el porcentaje que traiga se verifa para continuar con la cantidad de preguntas a realizar
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
                    abrirColores();
                } else if (cantidad_errores == 4 || cantidad_errores == 3) {
                    cantidad_preguntas = 2;
                    if (estadoEstadistica.equals("0")) {
                        r.ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirColores();
                } else if (cantidad_errores == 2 || cantidad_errores == 1) {
                    cantidad_preguntas = 1;
                    if (estadoEstadistica.equals("0")) {
                        r.ponerErroresEstadisticas(0, cantidad_preguntas, db, id_subcategoria, id_usuario);
                    }
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    abrirColores();
                } else {
                    //Excelente paso  la subcategoria
                    r.actualizarProgreso(cantidad_preguntas, 0, db, id_subcategoria, id_usuario);
                    r.actualizarEstadoProgreso(db, id_subcategoria, id_usuario);
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
                                abrirColores();
                            }
                        };
                        handler.postDelayed(met, 5000);

                    } else {
                        Toast.makeText(this, "Insertar números", Toast.LENGTH_SHORT).show();
                        return;
                        //Insertar en la tabla de progreso  la primer subcategoria de colores
                    }
                }
            } else {
                r.actualizarProgreso(cantidad_preguntas, cantidad_errores, db, id_subcategoria, id_usuario);
                abrirColores();
            }
        }
    }

    //Obtiene la cantidad de preguntas y cantidad de errores
    public List obtenerDatos() {
        List datos = new ArrayList();
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select cantidad_preguntas,cantidad_errores " + " from Progreso " +
                " where id_subcategoria = " + id_subcategoria + " and " + " id_persona= " + id_usuario, null);
        if (cursor.moveToFirst()) {
            datos.add(0, cursor.getString(cursor.getColumnIndex("cantidad_preguntas")));
            datos.add(1, cursor.getString(cursor.getColumnIndex("cantidad_errores")));
        }
        datos.add(2, db);
        datos.add(3, cursor);
        return datos;
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
            abrirColores();
        } else {
            int numero = r.sortear(d);
            this.id_pregunta = id_p[numero];
            //Obtiene la imagen de la pregunta
            Drawable resultado = establecerImagen(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "colores/", contexto), contexto);
            img.setImageDrawable(resultado);

            pregunta = reproducirAudio(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "colores/audios_preguntas_colores/", contexto),
                    "", null, this.amanager);
            audiogeneral = audio[numero];
            nombreSubcategoria = nombre_subcategoria;
        }
    }

    //Este método se encarga de obtener la siguiente subcategoria
    public int obtenerSiguienteSubctegoria(SQLiteDatabase db) {
        String[] listaDisponible = new String[]{"8", "9", "10", "11", "12"};
        Cursor subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                " where id_subcategoria >= " + 8 + " and id_subcategoria <= 12 " + " and " + " id_persona= " + id_usuario, null);
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
        int cantidad = r.verificarCantidadArreglo(listaDisponible);
        listaDisponible = r.eliminarValoresArreglo(listaDisponible, cantidad);
        cantidad = r.verificarCantidadArreglo(listaDisponible);
        subcategoriasProgreso.close();
        if (cantidad == 0) {
            return 0;
        } else {
            return r.realizarSigueteSubcategoria(listaDisponible);
        }
    }

    //Esta actividad se encarga de abrir colores
    private void abrirColores() {
        Intent colores = new Intent(Colores.this, Colores.class);
        colores.putExtra("id_usuario", id_usuario);
        colores.putExtra("genero", genero);
        colores.putExtra("id_subcategoria", id_subcategoria);
        startActivity(colores);
    }

    public String obtenerArchivoPregunta(String nombreImagen, String audio, String nombreSubcategoria, String direccion, Context c) {
        AssetManager assetManager = c.getAssets();
        String direccionImagen = "";
        String[] archivos = new String[1];
        if (direccion.equals("colores/")) {
            direccionImagen = direccion + nombreSubcategoria + "/" + nombreImagen + ".png";
        } else {
            direccionImagen = direccion + nombreSubcategoria + "/" + audio + ".mp3";
        }
        return direccionImagen;
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

    //Este metodo estable en el image view la imagen
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

    //Reproducción de audios de respuesta
    public void establecerAudiosRespuesta(final String[] respuestaAudio, final String nombreSubcategoria, final int contador) {
        String[] n = r.obtenerNumeros(2);
        final List nombre = verificarElColor();
        int p = nombre.size();
        int i = contador;
        i++;
        final int c = i;
        met = new Runnable() {
            public void run() {

                reproducirAudio("colores/audios_respuesta_colores/" + nombre.get(c) + ".mp3", "r", respuesta, amanager);
                if (c != 2) {
                    establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, c);
                    handler.postDelayed(met, 3000);
                } else {
                    met = new Runnable() {
                        public void run() {
                            hacerAudio();
                      //      amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "HOAOAOAOA", Toast.LENGTH_SHORT).show();
            handler.removeCallbacks(met);
            //respuesta.release();
            abrirColores();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            handler.removeCallbacks(met);
            //respuesta.release();
            abrirColores();
        }
    }


    //Verifica que color es el que tiene el botón
    private List verificarElColor() {
        Drawable drawable;
        List nombre = new ArrayList<>();
        Button[] botones = new Button[]{opcion1, opcion2, opcion3};
        int contador = 0;
        for (int i = 0; i < botones.length; i++) {
            drawable = botones[i].getBackground();
            int color = ((ColorDrawable) drawable).getColor();
            if (color == -1618884) {
                nombre.add(contador, "rojo");
                contador++;
            } else if (color == -39424) {
                nombre.add(contador, "anaranjado");
                contador++;
            } else if (color == -11309570) {
                nombre.add(contador, "azul");
                contador++;
            } else if (color == -932849) {
                nombre.add(contador, "amarillo");
                contador++;
            } else if (color == -14176672) {
                nombre.add(contador, "verde");
                contador++;
            }
        }
        return nombre;
    }

    //Establece los colores de los botones de las respuestas
    public void establecerRespuesta(String audio, String nombreSubcategoria) {
        String[] respuestas = new String[3];
        String[] colores = new String[]{
                "azul", "amarillo", "rojo", "anaranjado", "verde"
        };
        String[] listaColores = new String[4];
        if (nombreSubcategoria.equals(colores[0])) {
            colores[0] = "amarillo";
            colores[1] = "rojo";
            colores[2] = "anaranjado";
            colores[3] = "verde";
        } else if ((nombreSubcategoria.equals(colores[1]))) {
            colores[0] = "azul";
            colores[1] = "rojo";
            colores[2] = "anaranjado";
            colores[3] = "verde";
        } else if ((nombreSubcategoria.equals(colores[2]))) {
            colores[0] = "azul";
            colores[1] = "amarillo";
            colores[2] = "anaranjado";
            colores[3] = "verde";
        } else if ((nombreSubcategoria.equals(colores[3]))) {
            colores[0] = "azul";
            colores[1] = "amarillo";
            colores[2] = "rojo";
            colores[3] = "verde";
        } else if ((nombreSubcategoria.equals(colores[4]))) {
            colores[0] = "azul";
            colores[1] = "amarillo";
            colores[2] = "rojo";
            colores[3] = "anaranjado";
        }
        String[] resultado = new String[4];
        resultado = r.obtenerNumeros(3);
        listaColores[0] = colores[Integer.parseInt(resultado[0])];
        listaColores[1] = colores[Integer.parseInt(resultado[1])];
        listaColores[2] = nombreSubcategoria;
        resultado = r.obtenerNumeros(2);
        String direccion = "colores/" + nombreSubcategoria;
        //  lista = r.obtenerImagenRespuestas(direccion, "respuesta");
        respuestas = r.obtenerNumeros(2);
        verificarColor(opcion1, listaColores[Integer.parseInt(resultado[0])]);
        verificarColor(opcion2, listaColores[Integer.parseInt(resultado[1])]);
        verificarColor(opcion3, listaColores[Integer.parseInt(resultado[2])]);
        mostrarBotones();
    }

    //Verifica el color a asignar de los botones
    public void verificarColor(Button opcion, String color) {
        if (color.equals("amarillo")) {
            opcion.setBackgroundColor(Color.parseColor("#f1c40f"));
        } else if (color.equals("azul")) {
            opcion.setBackgroundColor(Color.parseColor("#536dfe"));
        } else if (color.equals("rojo")) {
            opcion.setBackgroundColor(Color.parseColor("#e74c3c"));
        } else if (color.equals("anaranjado")) {
            opcion.setBackgroundColor(Color.parseColor("#ff6600"));
        } else if (color.equals("verde")) {
            opcion.setBackgroundColor(Color.parseColor("#27ae60"));
        }
    }

    //Verificar el tipo de subcategoria
    public void verificarTipoSubcategoria(List<String> subcategoria, DBHandler mdb, Context contexto) {
        SQLiteDatabase db = mdb.getWritableDatabase();
        id_subcategoria = subcategoria.get(0);
        List datos = new ArrayList<>();
        if ((subcategoria.get(1)).trim().equals("Azul")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "azul", mdb, contexto);
        } else if ((subcategoria.get(1)).trim().equals("Amarillo")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "amarillo", mdb, contexto);
        } else if ((subcategoria.get(1)).trim().equals("Rojo")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "rojo", mdb, contexto);
        } else if ((subcategoria.get(1)).trim().equals("Anaranjado")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "anaranjado", mdb, contexto);
        } else if ((subcategoria.get(1)).trim().equals("Verde")) {
            Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)), mdb);
            Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
            realizarPreguntas(cursor, cursor1, "verde", mdb, contexto);
        }
        db.close();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Colores.this, MenuJuego.class);
        intent.putExtra("id_usuario", id_usuario);
        intent.putExtra("genero", genero);
        startActivity(intent);
        respuesta.release();
        pregunta.release();
        finish();
    }

//Metodos de reconocimiento de voz

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
                Intent menu = new Intent(Colores.this, MenuJuego.class);
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

    //Este metodo se encarga de verificar por recinocimeinto de voz la respuesta
    private void verificarReconocimientoVoz(String texto) {
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
        List datos = obtenerDatos();
        if (Integer.parseInt(id_pregunta) >= 69 && Integer.parseInt(id_pregunta) <= 77 && id_subcategoria.equals("8")) {
            if (texto.equals("Azul") || texto.equals("azul")) {
                verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
            } else if (texto.equals("Rojo") || texto.equals("rojo") || texto.equals("Amarillo") || texto.equals("amarillo") ||
                    texto.equals("Anaranjado") || texto.equals("anaranjado") || texto.equals("Verde") || texto.equals("verde")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto("" + cantidad_preguntas, "" + cantidad_errores);
            }
        } else if (Integer.parseInt(id_pregunta) >= 78 && Integer.parseInt(id_pregunta) <= 87 && id_subcategoria.equals("9")) {
            if (texto.equals("Amarillo") || texto.equals("amarillo")) {
                verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
            } else if (texto.equals("Rojo") || texto.equals("rojo") || texto.equals("Azul") || texto.equals("azul") ||
                    texto.equals("Anaranjado") || texto.equals("anaranjado") || texto.equals("Verde") || texto.equals("verde")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto("" + cantidad_preguntas, "" + cantidad_errores);
            }
        } else if (Integer.parseInt(id_pregunta) >= 88 && Integer.parseInt(id_pregunta) <= 96 && id_subcategoria.equals("10")) {
            if (texto.equals("Rojo") || texto.equals("rojo")) {
                verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
            } else if (texto.equals("Azul") || texto.equals("azul") || texto.equals("Amarillo") || texto.equals("amarillo") ||
                    texto.equals("Anaranjado") || texto.equals("anaranjado") || texto.equals("Verde") || texto.equals("verde")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto("" + cantidad_preguntas, "" + cantidad_errores);
            }
        } else if (Integer.parseInt(id_pregunta) >= 97 && Integer.parseInt(id_pregunta) <= 106 && id_subcategoria.equals("11")) {
            if (texto.equals("Anaranjado") || texto.equals("anaranjado")) {
                verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
            } else if (texto.equals("Rojo") || texto.equals("rojo") || texto.equals("Amarillo") || texto.equals("amarillo") ||
                    texto.equals("Azul") || texto.equals("azul") || texto.equals("Verde") || texto.equals("verde")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto("" + cantidad_preguntas, "" + cantidad_errores);
            }
        } else if (Integer.parseInt(id_pregunta) >= 107 && Integer.parseInt(id_pregunta) <= 115 && id_subcategoria.equals("12")) {
            if (texto.equals("Verde") || texto.equals("verde")) {
                verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
            } else if (texto.equals("Rojo") || texto.equals("rojo") || texto.equals("Amarillo") || texto.equals("amarillo") ||
                    texto.equals("Anaranjado") || texto.equals("anaranjado") || texto.equals("Azul") || texto.equals("azul")) {
                //Incorrecto vamos intentalo nuevamente
                incorrecto("" + cantidad_preguntas, "" + cantidad_errores);
            }
        }
    }

    //Ejecuta las respuestas
    public void ejecutarReproduccionAudio() {
        establecerRespuesta(audiogeneral, nombreSubcategoria);
        //obtiene los audios de las respuestas de la pregunta
        String[] respuestaAudio = obtenerAudiosRespuesta("colores/audios_respuesta_colores/", audiogeneral, nombreSubcategoria);
        //Reproduce cada unos de los audios de las respuestas
        establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, -1);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speech != null) {
            speech.destroy();
        }
    }
}

