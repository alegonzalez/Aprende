package ale.aprende.aprende;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
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
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ale.aprende.aprende.bd.DBHandler;

public class Abecedario extends AppCompatActivity implements RecognitionListener {
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
        setContentView(R.layout.activity_abecedario);
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

    //Metodo onclick del botón
    public void opcion1(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion1);
        opcion1.setEnabled(false);
    }

    //Metodo onclick del botón
    public void opcion2(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion2);
        opcion2.setEnabled(false);
    }

    //Metodo onclick del botón
    public void opcion3(View view) {
        List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
        verificarRespuesta(datos, opcion3);
        opcion3.setEnabled(false);
    }

    public void verificarRespuesta(List datos, ImageButton opcion) {
        String hola = nombreSubcategoria.substring(1, 2);
        if ((opcion.getTag().toString().substring(0, 1).equals(nombreSubcategoria.substring(1, 2)))) {
            //Es correcta la opcion
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else {
            //Es incorrecta
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
                        }else{
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
                                + id_usuario + " and " + " id_subcategoria >=48 ";
                        db.execSQL(strSQL1);
                        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                        Intent numeroActividad = new Intent(Abecedario.this, MenuJuego.class);
                        numeroActividad.putExtra("id_usuario", id_usuario);
                        numeroActividad.putExtra("genero", genero);
                        startActivity(numeroActividad);
                        finish();
                        return;
                    }
                }
            } else {
                r.actualizarProgreso(cantidad_preguntas, cantidad_errores, db, id_subcategoria, id_usuario);
                abrirFigurasGeometricas();
            }
        }
    }

    //Obtiene la siguiente subcategoria
    private int obtenerSiguienteSubctegoria(SQLiteDatabase db) {
        int resultado = 0;

        List datos = obtener(db);
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

    private List obtener(SQLiteDatabase db) {
        String[] listaDisponible = new String[]{"48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69",
                "70", "71", "72", "73", "74"};
        Cursor subcategoriasProgreso = null;
        if (estadoEstadistica.equals("1")) {
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria >= " + 48+ " and " + " id_persona= " + id_usuario + " and repeticion= 1", null);
        }else{
            subcategoriasProgreso = db.rawQuery("select id_subcategoria " + " from Progreso " +
                    " where id_subcategoria >= " + 48 + " and " + " id_persona= " + id_usuario, null);
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

    //Este metodo se encarga de reproducir los audios de las respuestas
    public String[] obtenerAudiosRespuesta(String tema, String audio, String nombreSubcategoria) {
        String[] direccionAudios = new String[3];
        if (opcion1.getTag().toString().substring(0, 2).equals("nn")) {
            direccionAudios[0] = "nn.mp3";
        } else {
            direccionAudios[0] = (opcion1.getTag().toString()).substring(0, 1) + ".mp3";
        }
        if (opcion2.getTag().toString().substring(0, 2).equals("nn")) {
            direccionAudios[1] = "nn.mp3";
        } else {
            direccionAudios[1] = (opcion2.getTag().toString()).substring(0, 1) + ".mp3";
        }
        if (opcion3.getTag().toString().substring(0, 2).equals("nn")) {
            direccionAudios[2] = "nn.mp3";
        } else {
            direccionAudios[2] = (opcion3.getTag().toString()).substring(0, 1) + ".mp3";
        }
        return direccionAudios;
    }

    //Reproducción de audios de respuesta
    public void establecerAudiosRespuesta(final String[] respuestaAudio, final String nombreSubcategoria, final int contador) {
        int i = contador;
        i++;
        final int c = i;
        met = new Runnable() {
            public void run() {
                reproducirAudio("abecedario/audios_respuesta_abecedario/" + respuestaAudio[c], "r", respuesta, amanager);
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

    //Ejecuta las respuestas
    public void ejecutarReproduccionAudio() {
        establecerRespuesta(audiogeneral, nombreSubcategoria);
        //obtiene los audios de las respuestas de la pregunta
        String[] respuestaAudio = obtenerAudiosRespuesta("figuras_geometricas/audio_respuesta_figuras_geometricas/", audiogeneral, nombreSubcategoria);
        //Reproduce cada unos de los audios de las respuestas
        establecerAudiosRespuesta(respuestaAudio, nombreSubcategoria, -1);
    }

    //Establece los colores de los botones de las respuestas
    public void establecerRespuesta(String audio, String nombreSubcategoria) {
        String[] respuestas = new String[3];
        String[] temas = new String[]{"Aa", "Bb", "Cc", "Dd", "Ee", "Ff", "Gg", "Hh", "Ii", "Jj", "Kk", "Ll", "Mm", "Nn", "Ññ", "Oo", "Pp", "Qq", "Rr", "Ss",
                "Tt", "Uu", "Vv", "Ww", "Xx", "Yy", "Zz"};
        String[] figuras_geometricas = new String[]{"circulo", "cuadrado", "triangulo", "rectangulo"};
        String[] listaColores = new String[]{"amarillo", "azul", "rojo", "anaranjado", "verde"};
        String[] resultadoTemas = new String[4];
        String[] resultadoColores = new String[4];
        String[] resultadoNumeros = new String[32];
        String[] resultadoFigurasGeometricas = new String[4];
        String[] resultado = new String[4];
        String archivoNumero = "";
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select count(*) as resultado" + " from Progreso " +
                " where id_subcategoria >= 13 and id_subcategoria <= 43 and " + " id_persona= " + id_usuario, null);
        int cantidad = 0;
        if (cursor.moveToFirst()) {
            cantidad = Integer.parseInt(cursor.getString(cursor.getColumnIndex("resultado")));
        }
        cursor.close();
        for (int i = 0; i < temas.length; i++) {
            if (nombreSubcategoria.equals(temas[i])) {
                temas[i] = "";
            }
        }
        int elementos = r.verificarCantidadArreglo(temas);
        temas = r.eliminarValoresArreglo(temas, elementos);
        //hacer el random para lo temas
        resultadoTemas = r.obtenerNumeros(25);
        //hacer el random para los colores
        resultadoColores = r.obtenerNumeros(4);
        resultadoFigurasGeometricas = r.obtenerNumeros(3);
        if (cantidad == 10) {
            resultadoNumeros = r.obtenerNumeros(9);
        } else if (cantidad == 20) {
            resultadoNumeros = r.obtenerNumeros(19);
        } else if (cantidad == 31) {
            resultadoNumeros = r.obtenerNumeros(30);
        }
        archivoNumero = verificarCategoriaNumero((Integer.parseInt(resultadoNumeros[0])));
        String nombreTema = "";
        nombreTema = verificarLetra(temas[Integer.parseInt(resultadoTemas[0])].substring(1));
        resultado[0] = "abecedario/" + nombreTema + "/" + nombreTema + "_" + archivoNumero + "/" + nombreTema + "_" + resultadoNumeros[0] + "_" + figuras_geometricas[Integer.parseInt(resultadoFigurasGeometricas[0])] + "_" + listaColores[Integer.parseInt(resultadoColores[0])] + ".png";
        archivoNumero = verificarCategoriaNumero((Integer.parseInt(resultadoNumeros[1])));
        nombreTema = verificarLetra(temas[Integer.parseInt(resultadoTemas[1])].substring(1));
        resultado[1] = "abecedario/" + nombreTema + "/" + nombreTema + "_" + archivoNumero + "/" + nombreTema + "_" + resultadoNumeros[1] + "_" + figuras_geometricas[Integer.parseInt(resultadoFigurasGeometricas[1])] + "_" + listaColores[Integer.parseInt(resultadoColores[1])] + ".png";
        archivoNumero = verificarCategoriaNumero((Integer.parseInt(resultadoNumeros[2])));
        nombreTema = verificarLetra(nombreSubcategoria.substring(1));
        resultado[2] = "abecedario/" + nombreTema + "/" + nombreTema + "_" + archivoNumero + "/" + nombreTema + "_" + resultadoNumeros[2] + "_" + figuras_geometricas[Integer.parseInt(resultadoFigurasGeometricas[2])] + "_" + listaColores[Integer.parseInt(resultadoColores[2])] + ".png";

        respuestas = r.obtenerNumeros(2);
        Drawable imagen = null;
        imagen = establecerImagen(resultado[Integer.parseInt(respuestas[0])].trim(), this);
        String[] r = resultado[Integer.parseInt(respuestas[0])].split("/");
        opcion1.setImageDrawable(imagen);
        opcion1.setTag(r[3]);
        imagen = establecerImagen(resultado[Integer.parseInt(respuestas[1])].trim(), this);
        r = resultado[Integer.parseInt(respuestas[1])].split("/");
        opcion2.setImageDrawable(imagen);
        opcion2.setTag(r[3]);
        r = resultado[Integer.parseInt(respuestas[2])].split("/");
        imagen = establecerImagen(resultado[Integer.parseInt(respuestas[2])].trim(), this);
        opcion3.setImageDrawable(imagen);
        opcion3.setTag(r[3]);
        mostrarBotones();
    }

    //Verifica si  la letra es una ñ para sustituirla por nn
    private String verificarLetra(String letra) {
        return (letra).equals("ñ") ? "nn" : letra;
    }

    //Este metodo se encarga de verificar el rango del numero
    private String verificarCategoriaNumero(int numero) {
        String resultado = "";
        if (numero <= 9) {
            resultado = "9";
        } else if (numero <= 19) {
            resultado = "19";
        } else if (numero <= 30) {
            resultado = "30";
        }
        return resultado;
    }

    //Verificar el tipo de subcategoria
    public void verificarTipoSubcategoria(List<String> subcategoria, DBHandler mdb, Context contexto) {
        SQLiteDatabase db = mdb.getWritableDatabase();
        String nombreSubcategoria = "";
        if (subcategoria.size() != 0) {
            id_subcategoria = subcategoria.get(0);
            nombreSubcategoria = subcategoria.get(1);
        } else {
            Cursor rep = db.rawQuery("select id from  Progreso " +
                    " where id_subcategoria >=48 and id_subcategoria <=74 and " + "id_persona = "
                    + id_usuario + " and repeticion= 1", null);
            Cursor repeticion = db.rawQuery("select p.id_subcategoria,s.nombre from  Progreso p,Subcategoria s " +
                    " where p.id_subcategoria = s.id  and p.id_subcategoria >=48 and p.id_subcategoria <=74 and p.id_persona = " + id_usuario + " and repeticion= 2", null);
            if (repeticion.getCount() <= 0) {
                if (rep.getCount() == 7) {
                    String strSQL1 = "UPDATE Progreso SET cantidad_preguntas = 3, cantidad_errores = 0, repeticion = 0 WHERE id_persona = "
                            + id_usuario + " and " + " id_subcategoria >=48 and id_subcategoria <=74";
                    db.execSQL(strSQL1);
                }
                id_subcategoria = "48";
                nombreSubcategoria = "Aa";
                String strSQL = "UPDATE Progreso SET cantidad_preguntas = 3, cantidad_errores = 0,repeticion=2 WHERE id_persona = "
                        + id_usuario + " and " + " id_subcategoria= " + id_subcategoria + "";
                db.execSQL(strSQL);
            } else {
                if (repeticion.moveToFirst()) {
                    id_subcategoria = repeticion.getString(repeticion.getColumnIndex("id_subcategoria"));
                    String nombre = repeticion.getString(repeticion.getColumnIndex("nombre"));
                    nombreSubcategoria = nombre;
                }
            }
            rep.close();
            repeticion.close();
        }
        String[] listaAbecedario = new String[]{"Aa", "Bb", "Cc", "Dd", "Ee", "Ff", "Gg", "Hh", "Ii", "Jj", "Kk", "Ll", "Mm", "Nn", "Ññ", "Oo", "Pp", "Qq", "Rr", "Ss",
                "Tt", "Uu", "Vv", "Ww", "Xx", "Yy", "Zz"};
        for (int i = 0; i < listaAbecedario.length; i++) {
            if (nombreSubcategoria.equals(listaAbecedario[i])) {
                Cursor cursor = r.obtenerPreguntasRealizadas(Integer.parseInt(id_subcategoria), mdb);
                Cursor cursor1 = r.obtenerTablaPersona_pregunta(db, id_usuario);
                realizarPreguntas(cursor, cursor1, listaAbecedario[i], mdb, contexto);
            }
        }
        db.close();
    }

    //Este metodo se encarga de verificar la letra del abecedario
    private List<String> verificarCategoriaAbecedario(int id_subcategoria) {
        List<String> datos = new ArrayList<>();
        if (id_subcategoria == 0) {
            datos.add(0, "48");
            datos.add(1, "Aa");
        } else if (id_subcategoria == 1) {
            datos.add(0, "49");
            datos.add(1, "Bb");
        } else if (id_subcategoria == 2) {
            datos.add(0, "50");
            datos.add(1, "Cc");
        } else if (id_subcategoria == 3) {
            datos.add(0, "51");
            datos.add(1, "Dd");
        } else if (id_subcategoria == 4) {
            datos.add(0, "52");
            datos.add(1, "Ee");
        } else if (id_subcategoria == 5) {
            datos.add(0, "53");
            datos.add(1, "Ff");
        } else if (id_subcategoria == 6) {
            datos.add(0, "54");
            datos.add(1, "Gg");
        } else if (id_subcategoria == 7) {
            datos.add(0, "55");
            datos.add(1, "Hh");
        } else if (id_subcategoria == 8) {
            datos.add(0, "56");
            datos.add(1, "Ii");
        } else if (id_subcategoria == 9) {
            datos.add(0, "57");
            datos.add(1, "Jj");
        } else if (id_subcategoria == 10) {
            datos.add(0, "58");
            datos.add(1, "Kk");
        } else if (id_subcategoria == 11) {
            datos.add(0, "59");
            datos.add(1, "Ll");
        } else if (id_subcategoria == 12) {
            datos.add(0, "60");
            datos.add(1, "Mm");
        } else if (id_subcategoria == 13) {
            datos.add(0, "61");
            datos.add(1, "Nn");
        } else if (id_subcategoria == 14) {
            datos.add(0, "Ññ");
            datos.add(1, "Dd");
        } else if (id_subcategoria == 15) {
            datos.add(0, "63");
            datos.add(1, "Oo");
        } else if (id_subcategoria == 16) {
            datos.add(0, "64");
            datos.add(1, "Pp");
        } else if (id_subcategoria == 17) {
            datos.add(0, "65");
            datos.add(1, "Qq");
        } else if (id_subcategoria == 18) {
            datos.add(0, "66");
            datos.add(1, "Rr");
        } else if (id_subcategoria == 19) {
            datos.add(0, "67");
            datos.add(1, "Ss");
        } else if (id_subcategoria == 20) {
            datos.add(0, "68");
            datos.add(1, "Tt");
        } else if (id_subcategoria == 21) {
            datos.add(0, "69");
            datos.add(1, "Uu");
        } else if (id_subcategoria == 22) {
            datos.add(0, "70");
            datos.add(1, "Vv");
        } else if (id_subcategoria == 23) {
            datos.add(0, "71");
            datos.add(1, "Ww");
        } else if (id_subcategoria == 24) {
            datos.add(0, "72");
            datos.add(1, "Xx");
        } else if (id_subcategoria == 25) {
            datos.add(0, "73");
            datos.add(1, "Yy");
        } else if (id_subcategoria == 26) {
            datos.add(0, "74");
            datos.add(1, "Zz");
        }
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
        //si d es igual a cero se acabaron las preguntas
        if (d == 0) {
            //Se elimina las preguntas realizadas y se inserta en la tabla de estadisticas
            r.eliminarPreguntasRealizada(mdb, id_subcategoria, id_usuario);
            abrirFigurasGeometricas();
        } else {
            int numero = r.sortear(d);
            this.id_pregunta = id_p[numero];
            //Obtiene la imagen de la pregunta
            Drawable resultado = establecerImagen(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "abecedario/", contexto), contexto);
            img.setImageDrawable(resultado);

            pregunta = reproducirAudio(obtenerArchivoPregunta(nombreImagen[numero], audio[numero], nombre_subcategoria, "abecedario/audio_pregunta_abecedario/", contexto),
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

    public String obtenerArchivoPregunta(String nombreImagen, String audio, String nombreSubcategoria, String direccion, Context c) {
        String direccionImagen = "";
        if (direccion.equals("abecedario/")) {
            direccionImagen = direccion + "pregunta_abecedario/" + nombreSubcategoria.substring(1) + "/" + nombreImagen;
        } else {
            direccionImagen = direccion + audio + ".mp3";
        }
        return direccionImagen;
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

    //Vuelve abrir la actividad de figuras geometricas
    private void abrirFigurasGeometricas() {
        Intent abecedario = new Intent(Abecedario.this, Abecedario.class);
        abecedario.putExtra("id_usuario", id_usuario);
        abecedario.putExtra("genero", genero);
        abecedario.putExtra("id_subcategoria", id_subcategoria);
        startActivity(abecedario);
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
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Abecedario.this, MenuJuego.class);
        intent.putExtra("id_usuario", id_usuario);
        intent.putExtra("genero", genero);
        startActivity(intent);
        respuesta.release();
        pregunta.release();
        finish();
    }

    //Reconocimiento de vooz
    public SpeechRecognizer hacerAudio() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
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
                Intent menu = new Intent(Abecedario.this, MenuJuego.class);
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

    //Este metodo se encarga de validar el reconocimiento de voz
    private void verificarReconocimientoVoz(String texto) {
        String nombreColor = "";
        String numero = "";
        String figura_geometrica = "";
        if (opcion1.getTag().toString().substring(0, 1).equals(nombreSubcategoria.substring(1, 2))) {
            nombreColor = opcion1.getTag().toString().split("_")[3];
            numero = opcion1.getTag().toString().split("_")[1];
            figura_geometrica = opcion1.getTag().toString().split("_")[2];
        } else if (opcion2.getTag().toString().substring(0, 1).equals(nombreSubcategoria.substring(1, 2))) {
            nombreColor = opcion2.getTag().toString().split("_")[3];
            numero = opcion2.getTag().toString().split("_")[1];
            figura_geometrica = opcion2.getTag().toString().split("_")[2];
        } else if (opcion3.getTag().toString().substring(0, 1).equals(nombreSubcategoria.substring(1, 2))) {
            nombreColor = opcion3.getTag().toString().split("_")[3];
            numero = opcion3.getTag().toString().split("_")[1];
            figura_geometrica = opcion3.getTag().toString().split("_")[2];
        }
        if (nombreSubcategoria.substring(1, 2).equals(texto) || nombreColor.equals(texto + ".png") || numero.equals(texto) || figura_geometrica.equals(texto)) {
            //Correcto
            List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
            verificarErrores((Cursor) datos.get(3), (SQLiteDatabase) datos.get(2), Integer.parseInt(datos.get(0).toString()), Integer.parseInt(datos.get(1).toString()));
        } else if (opcion1.getTag().toString().split("_")[0].equals(texto) || opcion1.getTag().toString().split("_")[1].equals(texto) || opcion1.getTag().toString().split("_")[2].equals(texto) || opcion1.getTag().toString().split("_")[3].equals(texto + ".png")
                || opcion2.getTag().toString().split("_")[0].equals(texto) || opcion2.getTag().toString().split("_")[1].equals(texto) || opcion2.getTag().toString().split("_")[2].equals(texto) || opcion2.getTag().toString().split("_")[3].equals(texto + ".png")
                || opcion3.getTag().toString().split("_")[0].equals(texto) || opcion3.getTag().toString().split("_")[1].equals(texto) || opcion3.getTag().toString().split("_")[2].equals(texto) || opcion3.getTag().toString().split("_")[3].equals(texto + ".png")) {
            //incorrecto
            List datos = c.obtenerDatos(id_subcategoria, id_usuario, getApplicationContext());
            incorrecto((String) datos.get(0), (String) datos.get(1));
        }
    }
}
