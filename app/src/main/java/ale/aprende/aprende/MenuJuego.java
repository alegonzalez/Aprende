package ale.aprende.aprende;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Movie;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Parcelable;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.TextInsideCircleButton;
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.OnBoomListener;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.nightonke.boommenu.Util;

import java.io.File;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_juego);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        id_usuario = obtenerIdUsuario();
        bmb = (BoomMenuButton) findViewById(R.id.bmb);
        bmb.setButtonEnum(ButtonEnum.TextInsideCircle);
        bmb.setAutoBoom(true);
        ListView listView = (ListView) findViewById(R.id.list_view);
        r = (RelativeLayout) findViewById(R.id.rlMenuJuego);
        r.setOnTouchListener(this);
        assert listView != null;
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1,
                BuilderManager.getCircleButtonData(piecesAndButtons)));
        cargarBotones(listView);
        animacionBoton(bmb);
        listenClickEventOf(R.id.bmb);
        //Evento click en el botón
        bmb.setOnBoomListener(new OnBoomListener() {
            @Override
            public void onClicked(int index, BoomButton boomButton) {
                if (index == 0 && lista[index] == true) {
                    Intent intento = new Intent(MenuJuego.this, Relaciones_espaciales.class);
                    abrirActividad(intento);
                    finish();
                } else if (index == 1 && lista[index] == true) {
                    Intent intento = new Intent(MenuJuego.this, Colores.class);
                    abrirActividad(intento);
                } else if (index == 2 && lista[index] == true) {
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    Intent intento = new Intent(MenuJuego.this, Numeros.class);
                    abrirActividad(intento);
                } else if (index == 3 && lista[index] == true) {
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    Intent intento = new Intent(MenuJuego.this, Figuras_geometricas.class);
                    abrirActividad(intento);
                } else if (index == 4 && lista[index] == true) {
                    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    Intent intento = new Intent(MenuJuego.this, Abecedario.class);
                    abrirActividad(intento);
                }
            }

            @Override
            public void onBackgroundClick() {
            }

            @Override
            public void onBoomWillHide() {
            }

            @Override
            public void onBoomDidHide() {

            }

            @Override
            public void onBoomWillShow() {
            }

            @Override
            public void onBoomDidShow() {
            }
        });


        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //Ejecicion del método en cierto tiempo
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                verificarNoTocaPantalla();
            }
        }, 20000);

    }

    private void listenClickEventOf(int bmb) {
        findViewById(bmb).setOnClickListener(this);
    }


    //Obtiene el id del usuario que se encuentra desde la imagen
    public int obtenerIdUsuario() {
        File file = new File("/sdcard/Aprende/");
        File list[] = file.listFiles();
        String[] resultado = list[0].getName().split("\\.");
        return Integer.parseInt(resultado[0].substring(resultado[0].length() - 1, resultado[0].length()));
    }

    //Verificar el tipo de pantalla para la colocación de botones
    public boolean cargarBotones(ListView ls) {
        lista = verificarTemasBloqueados();
        String[] temas = {"relaciones_espaciales", "Colores", "Numeros", "figuras_geometricas", "abecedario"};
        int texto = 0;
        int position = 57;
        ls.setVisibility(View.GONE);
        bmb.setPiecePlaceEnum((PiecePlaceEnum) piecesAndButtons.get(position).first);
        bmb.setButtonPlaceEnum((ButtonPlaceEnum) piecesAndButtons.get(position).second);
        bmb.clearBuilders();
        for (int i = 0; i < bmb.getPiecePlaceEnum().pieceNumber(); i++)
            if (temas[i].equals("relaciones_espaciales")) {
                TextInsideCircleButton.Builder builder = new TextInsideCircleButton.Builder();
                //        bmb.addBuilder(builder);
                texto = R.string.relaciones_espaciales;

                bmb.addBuilder(BuilderManager.getTextInsideCircleButtonBuilder(texto, aplicarIcono(lista[i])));
            } else if (temas[i].equals("Colores")) {
                texto = R.string.Colores;
                TextInsideCircleButton.Builder builder = new TextInsideCircleButton.Builder();
                // bmb.addBuilder(builder);
                bmb.addBuilder(BuilderManager.getTextInsideCircleButtonBuilder(texto, aplicarIcono(lista[i])));
            } else if (temas[i].equals("Numeros")) {
                texto = R.string.Numeros;
                TextInsideCircleButton.Builder builder = new TextInsideCircleButton.Builder();
                // bmb.addBuilder(builder);
                bmb.addBuilder(BuilderManager.getTextInsideCircleButtonBuilder(texto, aplicarIcono(lista[i])));
            } else if (temas[i].equals("figuras_geometricas")) {
                texto = R.string.figuras_geometricas;
                TextInsideCircleButton.Builder builder = new TextInsideCircleButton.Builder();
                //bmb.addBuilder(builder);
                bmb.addBuilder(BuilderManager.getTextInsideCircleButtonBuilder(texto, aplicarIcono(lista[i])));
            } else {
                texto = R.string.abecedario;
                TextInsideCircleButton.Builder builder = new TextInsideCircleButton.Builder();
                //bmb.addBuilder(builder);
                bmb.addBuilder(BuilderManager.getTextInsideCircleButtonBuilder(texto, aplicarIcono(lista[i])));
            }
        return true;
    }

    //Este metodo retorna 1 si puede ingresar al tema y cero si esta bloqueado
    private int aplicarIcono(boolean estado) {
        return (estado == true) ? 1 : 0;
    }

    //Animación del boton, que aparece en parpadeo
    private void animacionBoton(BoomMenuButton bmb) {
        Animation animacion = elaborarAnimacion(bmb);
        bmb.startAnimation(animacion);
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
        Cursor cursor = db.rawQuery("select * from Progreso where id_persona= " + id_usuario, null);
        if (!(cursor.moveToFirst()) || cursor.getCount() == 0) {
            Relaciones_espaciales r = new Relaciones_espaciales();
            int numero = r.sortear(4);
            ContentValues values = new ContentValues();
            values.put("id_persona", id_usuario);
            values.put("id_subcategoria", numero + 1);
            values.put("estado", false);
            values.put("cantidad_preguntas", 3);
            values.put("cantidad_errores", 0);
            db.insert("Progreso", null, values);
            estado[0] = true;
            estado[1] = false;
            estado[2] = false;
            estado[3] = false;
            estado[4] = false;
        } else {
            while (!cursor.isAfterLast()) {
                int estado_tema = (cursor.getInt(cursor.getColumnIndex("estado")));
                if (estado_tema == 0) {
                    String id_subcategoria = cursor.getString(cursor.getColumnIndex("id_subcategoria"));
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
                    categoria.close();
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        db.close();
        return estado;
    }

    //Este metodo se utiliza para abrir as actividades
    public void abrirActividad(Intent intento) {
        intento.putExtra("id_usuario", id_usuario);
        startActivity(intento);
    }


    @Override
    public void onResume() {
        speech = hacerAudio();
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
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
            speech.stopListening();
            speech.destroy();
        }
        super.onDestroy();
    }

    //Abrir la actividad de relaciones espaciales
    public void abrirRelacionesEspaciales() {
        Intent intent = new Intent(MenuJuego.this, Relaciones_espaciales.class);
        intent.putExtra("id_usuario", id_usuario);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {

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
            if (result.equals("Relaciones espaciales") || result.equals("relaciones espaciales")) {
                abrirRelacionesEspaciales();
            } else if (result.equals("Caballo") || result.equals("caballo") || result.equals("caballos") || result.equals("Caballos")) {
                abrirRelacionesEspaciales();
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
            Toast.makeText(this, "Vamos niño tu puedes", Toast.LENGTH_SHORT).show();
            eventoTocar = false;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    verificarNoTocaPantalla();
                }
            }, 20000);
        }else{
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    verificarNoTocaPantalla();
                }
            }, 20000);
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
}
