package ale.aprende.aprende;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.TextInsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.OnBoomListener;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


import ale.aprende.aprende.bd.DBHandler;

public class MenuJuego extends AppCompatActivity implements View.OnClickListener, RecognitionListener, View.OnTouchListener {
    //Declaración de variables
    private ArrayList<android.util.Pair> piecesAndButtons = new ArrayList<>();
    private BoomMenuButton bmb;
    private int id_usuario;
    private Boolean[] lista;
    private Boolean eventoTocar = false;
    AudioManager amanager;
    public SpeechRecognizer speech;
    private Intent recognizerIntent;
    final Handler handler = new Handler();
    RelativeLayout r;
    private String id_subcategoria = "";
    private String genero = "M";
    MediaPlayer audio = new MediaPlayer();
    private int pasada = 0;
    private BootstrapButton btn_relaciones_espaciales, btn_colores, btn_numeros, btn_figuras_geometricas, btn_abecedario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_juego);
        TypefaceProvider.registerDefaultIconSets();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        id_usuario = obtenerIdUsuario();
        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        btn_relaciones_espaciales = (BootstrapButton) findViewById(R.id.btn_relaciones_espaciales);
        btn_colores = (BootstrapButton) findViewById(R.id.btn_colores);
        btn_numeros = (BootstrapButton) findViewById(R.id.btn_numeros);
        btn_figuras_geometricas = (BootstrapButton) findViewById(R.id.btn_figuras_geometricas);
        btn_abecedario = (BootstrapButton) findViewById(R.id.btn_abecedario);
        //genero = getIntent().getExtras().getString("genero");
        cargarBotones();
        //Ejecicion del método en cierto tiempo
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                verificarNoTocaPantalla();
            }
        }, 30000);
        audio.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                audio.release();
            }
        });
    }

    //Método onclick para el boton de relaciones espaciales
    public void relacionesEspaciales(View view) {
        abrirRelacionesEspaciales();
    }

    //Método onclick para el boton de colores
    public void colores(View view) {
        abrirColores();
    }

    //Método onclick para el boton de numeros
    public void numeros(View view) {
        abrirNumeros();
    }

    //Método onclick para el boton de figuras geometricas
    public void figuras_geometricas(View view) {
        abrirFigurasGeometricas();
    }

    //Método onclick para el boton de abecedario
    public void abecedario(View view) {
        abrirAbecedario();
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
                } else if (lista[i] == true && i == 3) {
                    btn_abecedario.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pig, 0, 0, 0);
                }
            }
        } else
            return false;

        return true;
    }


    public Animation elaborarAnimacion(BoomMenuButton boton) {
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(800);

        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(Animation.INFINITE);
        return animation;
    }

    //verifica que temas estan bloqueados
    public Boolean[] verificarTemasBloqueados() {
        Boolean[] estado = new Boolean[5];
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from Progreso where id_persona= " + id_usuario + " and estado = 0", null);
        if (!(cursor.moveToFirst()) || cursor.getCount() == 0) {
            Relaciones_espaciales r = new Relaciones_espaciales();
            int numero = r.sortear(6);
            ContentValues values = new ContentValues();
            numero = numero + 1;
            id_subcategoria = ""+numero;
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
            String id_subcategoria = cursor.getString(cursor.getColumnIndex("id_subcategoria"));
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
                    estado[0] = false;
                    estado[1] = false;
                    estado[2] = false;
                    estado[3] = false;
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
        }
        else {
            hacerAudio();
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }

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
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000);
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
                abrirRelacionesEspaciales();
            } else if (result.equals("Colores") || result.equals("colores") || result.equals("delfín") || result.equals("Delfín")) {
                abrirColores();
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
            }, 30000);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    verificarNoTocaPantalla();
                }
            }, 30000);
            eventoTocar = false;
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int eventaction = event.getAction();
        if (eventaction == MotionEvent.ACTION_DOWN) {
            eventoTocar = true;
        }
        return true;

    }

    //Este metodo se encarga de reproducir si es una niña o niño
    public void audioBienvenida() {
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        String nombre_imagen = "";
        if (genero.trim().equals("M")) {
            nombre_imagen = "general/bienvenido_m.mp3";
        } else {
            nombre_imagen = "general/bienvenida_f.mp3";
        }
        try {
            AssetFileDescriptor afd = getAssets().openFd(nombre_imagen);
            audio.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            //afd.close();
            audio.prepare();
            audio.setVolume(1, 1);
            audio.start();
            pasada = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}