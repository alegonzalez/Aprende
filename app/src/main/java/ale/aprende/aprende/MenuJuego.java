package ale.aprende.aprende;

import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
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

import com.beardedhen.androidbootstrap.BootstrapButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ale.aprende.aprende.Ingresar.Ingresar;
import ale.aprende.aprende.bd.DBHandler;

public class MenuJuego extends AppCompatActivity implements View.OnClickListener, RecognitionListener, View.OnTouchListener {
    //Declaración de variables
    private int id_usuario;
    private Boolean[] lista;
    private Boolean eventoTocar = false;
    AudioManager amanager;
    public SpeechRecognizer speech;
    private Intent recognizerIntent;
    private final Handler handler = new Handler();
    private String id_subcategoria = "";
    private String genero = "";
    private MediaPlayer audio = new MediaPlayer();
    private int pasada = 0;
    private BootstrapButton btn_relaciones_espaciales, btn_colores, btn_numeros, btn_figuras_geometricas, btn_abecedario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_juego);
        id_usuario = obtenerIdUsuario();
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        btn_relaciones_espaciales = (BootstrapButton) findViewById(R.id.btn_relaciones_espaciales);
        btn_relaciones_espaciales.setBackgroundColor(Color.GREEN);
        btn_colores = (BootstrapButton) findViewById(R.id.btn_colores);
        btn_colores.setBackgroundColor(Color.YELLOW);
        btn_numeros = (BootstrapButton) findViewById(R.id.btn_numeros);
        btn_numeros.setBackgroundColor(Color.RED);
        btn_figuras_geometricas = (BootstrapButton) findViewById(R.id.btn_figuras_geometricas);
        btn_figuras_geometricas.setBackgroundColor(Color.BLUE);
        btn_abecedario = (BootstrapButton) findViewById(R.id.btn_abecedario);
        btn_abecedario.setBackgroundColor(Color.parseColor("#FF8900"));
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        genero = getIntent().getExtras().getString("genero");
        cargarBotones();
        //Ejecición del método en cierto tiempo
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                verificarNoTocaPantalla();
            }
        }, 15000);
        audio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                audio.reset();
            }
        });
    }

    //Método onclick para el boton de relaciones espaciales
    public void relacionesEspaciales(View view) {
        //Ejecicion del método en cierto tiempo
        if (lista[1] == false) {
            handler.removeCallbacksAndMessages(null);
            String direccionAudio = (genero.equals("M")) ? "general/relaciones_espaciales_m.mp3" : "general/relaciones_espaciales_f.mp3";
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            audio.reset();
            reproducirAudio(direccionAudio);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    audio.reset();
                    abrirRelacionesEspaciales();
                }
            }, 6000);
        } else {
            audio.reset();
            abrirRelacionesEspaciales();
        }
    }

    //Método onclick para el boton de colores
    public void colores(View view) {
        handler.removeCallbacksAndMessages(null);
        if(lista[1] == true){
            if (lista[2] == true) {
                String direccionAudio = (genero.equals("M")) ? "general/colores_m.mp3" : "general/colores_f.mp3";
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                audio.reset();
                reproducirAudio(direccionAudio);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        audio.reset();
                        abrirColores();
                    }
                }, 5000);
            } else {
                audio.reset();
                abrirColores();
            }
        }

    }

    //Método onclick para el boton de numeros
    public void numeros(View view) {
        handler.removeCallbacksAndMessages(null);
       if(lista[2] == true){
           if (lista[3] == true) {
               String direccionAudio = (genero.equals("M")) ? "general/numeros_m.mp3" : "general/numeros_f.mp3";
               amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
               audio.reset();
               reproducirAudio(direccionAudio);
               handler.postDelayed(new Runnable() {
                   @Override
                   public void run() {
                       audio.reset();
                       abrirNumeros();
                   }
               }, 5000);
           } else {
               audio.reset();
               abrirNumeros();
           }
       }
    }

    //Método onclick para el boton de figuras geometricas
    public void figuras_geometricas(View view) {
        handler.removeCallbacksAndMessages(null);
       if(lista[3] == true){
           if (lista[4] == false) {
               String direccionAudio = (genero.equals("M")) ? "general/figuras_geometricas_m.mp3" : "general/figuras_geometricas_f.mp3";
               amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
               audio.reset();
               reproducirAudio(direccionAudio);
               handler.postDelayed(new Runnable() {
                   @Override
                   public void run() {
                       audio.reset();
                       abrirFigurasGeometricas();
                   }
               }, 7000);
           } else {
               audio.reset();
               abrirFigurasGeometricas();
           }

       }

    }

    //Método onclick para el boton de abecedario
    public void abecedario(View view) {
        handler.removeCallbacksAndMessages(null);
        if (lista[4] == true) {
            DBHandler mdb = new DBHandler(getApplicationContext());
            SQLiteDatabase db = mdb.getWritableDatabase();
            Cursor cursor = db.rawQuery("select count(*) as resultado from Estadistica where id_persona= " + id_usuario + " and id_subcategoria = 74 and estado = 0", null);
            if (cursor.moveToFirst()) {
                if (cursor.getString(cursor.getColumnIndex("resultado")).equals("1")) {
                    String direccionAudio = (genero.equals("M")) ? "general/abecedario_m.mp3" : "general/abecedario_f.mp3";
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    audio.reset();
                    reproducirAudio(direccionAudio);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            audio.reset();
                            abrirAbecedario();
                        }
                    }, 6000);
                } else {
                    abrirAbecedario();
                }
            }
            cursor.close();
            db.close();
        } else {
            audio.reset();
            abrirAbecedario();
        }

    }

    //Obtiene el id del usuario que se encuentra desde la imagen
    public int obtenerIdUsuario() {
        File file = new File("/sdcard/Aprende/");
        File list[] = file.listFiles();
        String[] resultado = list[0].getName().split("\\.");
        return Integer.parseInt(resultado[0].substring(resultado[0].length() - 1, resultado[0].length()));
    }

    //Verificar el tipo de pantalla para la colocación de botones
    public boolean cargarBotones() {
        lista = verificarTemasBloqueados();
        if (lista != null) {
            for (int i = 0; i < lista.length; i++) {
                if (lista[i] == true && i == 0) {
                    btn_relaciones_espaciales.setCompoundDrawablesWithIntrinsicBounds(R.drawable.horse, 0, 0, 0);
                } else if (lista[i] == true && i == 1) {
                    btn_colores.setCompoundDrawablesWithIntrinsicBounds(R.drawable.dolphin, 0, 0, 0);
                } else if (lista[i] == true && i == 2) {
                    btn_numeros.setCompoundDrawablesWithIntrinsicBounds(R.drawable.elephant, 0, 0, 0);
                } else if (lista[i] == true && i == 3) {
                    btn_figuras_geometricas.setCompoundDrawablesWithIntrinsicBounds(R.drawable.cat, 0, 0, 0);
                } else if (lista[i] == true && i == 4) {
                    btn_abecedario.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pig, 0, 0, 0);
                }
            }
        } else
            return false;

        return true;
    }

    //verifica que temas estan bloqueados
    public Boolean[] verificarTemasBloqueados() {
        Boolean[] estado = new Boolean[5];
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from Progreso where id_persona= " + id_usuario + " and estado = 0", null);
        Cursor datos = db.rawQuery("select max(id_subcategoria) as id_subcategoria from Progreso where id_persona= " + id_usuario, null);
        String id_subcategoria = " ";
        datos.moveToFirst();
        id_subcategoria = datos.getString(datos.getColumnIndex("id_subcategoria"));
        if (id_subcategoria == null) {
            id_subcategoria = "";
        }
        if (cursor.getCount() == 0 && !(id_subcategoria.equals("74"))) {
            Relaciones_espaciales r = new Relaciones_espaciales();
            int numero = r.sortear(6);
            ContentValues values = new ContentValues();
            numero = numero + 1;
            id_subcategoria = "" + numero;
            values.put("id_persona", id_usuario);
            values.put("id_subcategoria", numero);
            values.put("cantidad_preguntas", 3);
            values.put("estado", false);
            values.put("cantidad_errores", 0);
            db.insert("Progreso", null, values);
            estado[0] = true;
            estado[1] = false;
            estado[2] = false;
            estado[3] = false;
            estado[4] = false;
            audioBienvenida();
        } else {
            if (!(id_subcategoria.equals("74"))) {
                cursor.moveToFirst();
                id_subcategoria = cursor.getString(cursor.getColumnIndex("id_subcategoria"));
            }
            this.id_subcategoria = id_subcategoria;
            Cursor categoria = db.rawQuery("select * from SubCategoria  where id= '" + id_subcategoria.trim() + "'", null);
            if (categoria != null && categoria.moveToFirst()) {
                if ((categoria.getInt(categoria.getColumnIndex("id_categoria"))) == 1) {
                    estado[0] = true;
                    estado[1] = false;
                    estado[2] = false;
                    estado[3] = false;
                    estado[4] = false;
                } else if (categoria.getInt(categoria.getColumnIndex("id_categoria")) == 2) {
                    estado[0] = true;
                    estado[1] = true;
                    estado[2] = false;
                    estado[3] = false;
                    estado[4] = false;
                } else if (categoria.getInt(categoria.getColumnIndex("id_categoria")) == 3) {
                    estado[0] = true;
                    estado[1] = true;
                    estado[2] = true;
                    estado[3] = false;
                    estado[4] = false;
                } else if (categoria.getInt(categoria.getColumnIndex("id_categoria")) == 4) {
                    estado[0] = true;
                    estado[1] = true;
                    estado[2] = true;
                    estado[3] = true;
                    estado[4] = false;
                } else if (categoria.getInt(categoria.getColumnIndex("id_categoria")) == 5) {
                    estado[0] = true;
                    estado[1] = true;
                    estado[2] = true;
                    estado[3] = true;
                    estado[4] = true;
                }
            }
        }
        cursor.close();
        db.close();
        return estado;
    }


    @Override
    public void onResume() {
        if (pasada == 1) {
            speech = hacerAudio();
            pasada = 0;
            audio.reset();
            BootstrapButton btn = null;
            btn = obtenerBoton();
            animar(btn);
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            ejecutar();
        } else {
            hacerAudio();
            ejecutar();
            //amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }
        super.onResume();
    }

    private void ejecutar() {
        //Ejecicion del método en cierto tiempo
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                verificarNoTocaPantalla();
            }
        }, 12000);
    }

    @Override
    protected void onPause() {
        pasada = 1;
        speech.destroy();
        handler.removeCallbacksAndMessages(null);
        audio.reset();
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        audio.reset();
        super.onDestroy();
        if (speech != null) {
            speech.stopListening();
            speech.destroy();
        }
    }

    //Abrir la actividad de relaciones espaciales
    public void abrirRelacionesEspaciales() {
        Intent intent = new Intent(MenuJuego.this, Relaciones_espaciales.class);
        intent.putExtra("id_usuario", id_usuario);
        intent.putExtra("genero", genero);
        startActivity(intent);
        finish();
    }

    //Abrir la actividad de colores
    public void abrirColores() {
        if (lista[1] == true) {
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            Intent intento = new Intent(MenuJuego.this, Colores.class);
            intento.putExtra("id_usuario", id_usuario);
            intento.putExtra("genero", genero);
            intento.putExtra("id_subcategoria", id_subcategoria);
            startActivity(intento);
            finish();
        }
    }

    //Abrir la actividad de numeros
    public void abrirNumeros() {
        if (lista[2] == true) {
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            Intent intento = new Intent(MenuJuego.this, Numeros.class);
            intento.putExtra("id_usuario", id_usuario);
            intento.putExtra("genero", genero);
            intento.putExtra("id_subcategoria", id_subcategoria);
            startActivity(intento);
            finish();
        }
    }

    //Abrir la actividad de figuras geometricas
    public void abrirFigurasGeometricas() {
        if (lista[3] == true) {
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            Intent intento = new Intent(MenuJuego.this, Figuras_geometricas.class);
            intento.putExtra("id_usuario", id_usuario);
            intento.putExtra("genero", genero);
            intento.putExtra("id_subcategoria", id_subcategoria);
            startActivity(intento);
            finish();
        }
    }

    //Abrir la actividad de abecedario
    public void abrirAbecedario() {
        if (lista[4] == true) {
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            Intent intento = new Intent(MenuJuego.this, Abecedario.class);
            intento.putExtra("id_usuario", id_usuario);
            intento.putExtra("genero", genero);
            intento.putExtra("id_subcategoria", id_subcategoria);
            startActivity(intento);
            finish();
        }
    }

    @Override
    public void onClick(View v) {

    }

    //Reconocimiento de vooz
    public SpeechRecognizer hacerAudio() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speech.startListening(recognizerIntent);
        return speech;
    }

    //Metodos de la implementacion de RecognitionListener
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
    public void onError(int errorCode) {
        if (speech != null) {
            speech.destroy();
            hacerAudio();
        }
    }

    //Este metodo se obtiene el texto que dice la persona
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String texto = "";
        for (String result : matches)
            if (result.equals("Relaciones espaciales") || result.equals("relaciones espaciales") || result.equals("Caballo") || result.equals("caballo")) {
                handler.removeCallbacksAndMessages(null);
                if (lista[1] == false) {
                    String direccionAudio = (genero.equals("M")) ? "general/relaciones_espaciales_m.mp3" : "general/relaciones_espaciales_f.mp3";
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    audio.reset();
                    reproducirAudio(direccionAudio);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            audio.reset();
                            abrirRelacionesEspaciales();
                        }
                    }, 6000);
                } else {
                    audio.reset();
                    abrirRelacionesEspaciales();
                }
            } else if (result.equals("Colores") || result.equals("colores") || result.equals("delfín") || result.equals("Delfín")) {
                handler.removeCallbacksAndMessages(null);
                if (lista[2] == false) {
                    String direccionAudio = (genero.equals("M")) ? "general/colores_m.mp3" : "general/colores_f.mp3";
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    audio.reset();
                    reproducirAudio(direccionAudio);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            audio.reset();
                            abrirColores();
                        }
                    }, 5000);
                } else {
                    audio.reset();
                    abrirColores();
                }
            } else if (result.equals("Numeros") || result.equals("numeros") || result.equals("números") || result.equals("elefante") || result.equals("Elefante")) {
                handler.removeCallbacksAndMessages(null);
                if (lista[3] == false) {
                    String direccionAudio = (genero.equals("M")) ? "general/numeros_m.mp3" : "general/numeros_f.mp3";
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    audio.reset();
                    reproducirAudio(direccionAudio);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            audio.reset();
                            abrirNumeros();
                        }
                    }, 5000);
                } else {
                    audio.reset();
                    abrirNumeros();
                }
            } else if (result.equals("figuras geométricas") || result.equals("Figuras geométricas") || result.equals("Gato") || result.equals("gato")) {
                handler.removeCallbacksAndMessages(null);
                if (lista[4] == false) {
                    String direccionAudio = (genero.equals("M")) ? "general/figuras_geometricas_m.mp3" : "general/figuras_geometricas_f.mp3";
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    audio.reset();
                    reproducirAudio(direccionAudio);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            audio.reset();
                            abrirFigurasGeometricas();
                        }
                    }, 7000);
                } else {
                    audio.reset();
                    abrirFigurasGeometricas();
                }
            } else if (result.equals("Abecedario") || result.equals("abecedario") || result.equals("Cerdo") || result.equals("cerdo") || result.equals("Chancho")
                    || result.equals("chancho")) {
                handler.removeCallbacksAndMessages(null);
                if (lista[4] == true) {
                    DBHandler mdb = new DBHandler(getApplicationContext());
                    SQLiteDatabase db = mdb.getWritableDatabase();
                    Cursor cursor = db.rawQuery("select count(*) as resultado from Estadistica where id_persona= " + id_usuario + " and id_subcategoria = 74 and estado = 0", null);
                    if (cursor.moveToFirst()) {
                        if (cursor.getString(cursor.getColumnIndex("resultado")).equals("1")) {
                            String direccionAudio = (genero.equals("M")) ? "general/abecedario_m.mp3" : "general/abecedario_f.mp3";
                            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                            audio.reset();
                            reproducirAudio(direccionAudio);
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    audio.reset();
                                    abrirAbecedario();
                                }
                            }, 6000);
                        } else {
                            abrirAbecedario();
                        }
                    }
                    cursor.close();
                    db.close();
                } else {
                    audio.reset();
                    abrirAbecedario();
                }
            }
        hacerAudio();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Error de grabación de audio";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Error del lado del cliente";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Permisos insuficientes";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Error de red";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Tiempo de espera de la red";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No coinciden";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Reconocimiento Servicio ocupado";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Error del servidor";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "Sin entrada de voz";
                break;
            default:
                message = "No lo entendí, por favor, inténtelo de nuevo.";
                break;
        }
        return message;
    }


    //Este metodo verifica que si la pantalla no es tocada en cierto limite de tiempo
    public void verificarNoTocaPantalla() {
        if (!eventoTocar) {
            eventoTocar = false;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    verificarNoTocaPantalla();
                }
            }, 15000);

            amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            if (!(audio.isPlaying())) {
                String direccionAudio = (genero.equals("M")) ? "general/menu_m.mp3" : "general/menu_f.mp3";
                reproducirAudio(direccionAudio);
                BootstrapButton btn;
                btn = obtenerBoton();
                animar(btn);
            }
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    verificarNoTocaPantalla();
                }
            }, 15000);
            eventoTocar = false;
        }
    }

    private BootstrapButton obtenerBoton() {
        BootstrapButton btn = null;
        if (lista[0] == true) {
            btn = btn_relaciones_espaciales;
        }
        if (lista[1] == true) {
            btn = btn_colores;
        }
        if (lista[2] == true) {
            btn = btn_numeros;
        }
        if (lista[3] == true) {
            btn = btn_figuras_geometricas;
        }
        if (lista[4] == true) {
            btn = btn_abecedario;
        }
        return btn;
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
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(MenuJuego.this, Ingresar.class);
        startActivity(intent);
        finish();
    }

    //Este metodo se encarga de reproducir si es una niña o niño
    public void audioBienvenida() {
        pasada = 0;
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        String nombre_imagen = "";
        if (genero.trim().equals("M")) {
            nombre_imagen = "general/bienvenido_m.mp3";
        } else {
            nombre_imagen = "general/bienvenida_f.mp3";
        }
        reproducirAudio(nombre_imagen);
    }


    private void reproducirAudio(String direccionAudio) {
        audio.reset();
        try {
            AssetFileDescriptor afd = this.getAssets().openFd(direccionAudio);
            audio.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            // afd.close();
            audio.prepare();
            audio.setVolume(1, 1);
            audio.start();
            //pasada = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Animar los botones
    private void animar(BootstrapButton btn) {
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
}