package ale.aprende.aprende;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.BoomButtonBuilder;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.OnBoomListener;
import com.nightonke.boommenu.OnBoomListenerAdapter;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.nightonke.boommenu.Util;

import java.io.File;
import java.util.ArrayList;


import ale.aprende.aprende.principal.MainActivity;
import ale.aprende.aprende.registrar.DBHandler;

public class MenuJuego extends AppCompatActivity implements View.OnClickListener, RecognitionListener {
    private ArrayList<android.util.Pair> piecesAndButtons = new ArrayList<>();
    private BoomMenuButton bmb;
    private int id_usuario;
    private Boolean[] lista;
    private Intent recognizerIntent;
    private SpeechRecognizer speech = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_juego);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        id_usuario = obtenerIdUsuario();
        bmb = (BoomMenuButton) findViewById(R.id.bmb);
        assert bmb != null;
        bmb.setButtonEnum(ButtonEnum.Ham);
        bmb.setPiecePlaceEnum(PiecePlaceEnum.HAM_1);
        bmb.setButtonPlaceEnum(ButtonPlaceEnum.HAM_1);
        bmb.setBottomHamButtonTopMargin(Util.dp2px(20));
        bmb.performClick();
        bmb.setAutoBoom(true);
        ListView listView = (ListView) findViewById(R.id.list_view);
        assert listView != null;
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1,
                BuilderManager.getHamButtonData(piecesAndButtons)));
        cargarBotones(listView);
        animacionBoton(bmb);
        listenClickEventOf(R.id.bmb);
        hacerAudio();
        //Evento click en el botón
        bmb.setOnBoomListener(new OnBoomListener() {

            @Override
            public void onClicked(int index, BoomButton boomButton) {

                if (index == 0 && lista[index] == true) {
                    Toast.makeText(MenuJuego.this, "ENTRARARARARARA", Toast.LENGTH_SHORT).show();
                    Intent intento = new Intent(MenuJuego.this, Relaciones_espaciales.class);
                    abrirActividad(intento);
                } else if (index == 1 && lista[index] == true) {
                    Intent intento = new Intent(MenuJuego.this, Colores.class);
                    abrirActividad(intento);
                } else if (index == 2 && lista[index] == true) {
                    Intent intento = new Intent(MenuJuego.this, Numeros.class);
                    abrirActividad(intento);
                } else if (index == 3 && lista[index] == true) {
                    Intent intento = new Intent(MenuJuego.this, Figuras_geometricas.class);
                    abrirActividad(intento);
                } else if (index == 4 && lista[index] == true) {
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
        hacerAudio();
        AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
    }

    public void hacerAudio() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        speech.startListening(recognizerIntent);
    }

    private void listenClickEventOf(int bmb) {
        findViewById(bmb).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
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
        int position = 14;
        ls.setVisibility(View.GONE);
        bmb.setPiecePlaceEnum((PiecePlaceEnum) piecesAndButtons.get(position).first);
        bmb.setButtonPlaceEnum((ButtonPlaceEnum) piecesAndButtons.get(position).second);
        bmb.clearBuilders();
        for (int i = 0; i < bmb.getPiecePlaceEnum().pieceNumber(); i++)
            if (temas[i].equals("relaciones_espaciales")) {
                texto = R.string.relaciones_espaciales;
                bmb.addBuilder(BuilderManager.getHamButtonBuilder(texto, aplicarIcono(lista[i])));
            } else if (temas[i].equals("Colores")) {
                texto = R.string.Colores;
                bmb.addBuilder(BuilderManager.getHamButtonBuilder(texto, aplicarIcono(lista[i])));
            } else if (temas[i].equals("Numeros")) {
                texto = R.string.Numeros;
                bmb.addBuilder(BuilderManager.getHamButtonBuilder(texto, aplicarIcono(lista[i])));
            } else if (temas[i].equals("figuras_geometricas")) {
                texto = R.string.figuras_geometricas;
                bmb.addBuilder(BuilderManager.getHamButtonBuilder(texto, aplicarIcono(lista[i])));
            } else {
                texto = R.string.abecedario;
                bmb.addBuilder(BuilderManager.getHamButtonBuilder(texto, aplicarIcono(lista[i])));
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
        //animation.setStartOffset(10);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(Animation.INFINITE);
        return animation;
    }

    //verifica que temas estan bloqueados
    public Boolean[] verificarTemasBloqueados() {
        Boolean[] estado = new Boolean[5];
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from Progreso where id= " + id_usuario, null);
        if (!(cursor.moveToFirst()) || cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put("id_persona", id_usuario);
            values.put("id_subcategoria", 1);
            values.put("estado", false);
            db.insert("Progreso", null, values);
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
        return estado;
    }

    //Este metodo se utiliza para abrir as actividades
    public void abrirActividad(Intent intento) {
        startActivity(intento);
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
        if (errorCode != 7 && errorCode != 8) {
            Toast.makeText(this, "" + getErrorText(errorCode), Toast.LENGTH_SHORT).show();
        } else {
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
            } else if (result.equals("Caballo") || result.equals("caballo") ||result.equals("caballos") || result.equals("Caballos")) {
                abrirRelacionesEspaciales();
            }
        hacerAudio();
    }

    @Override
    public void onResume() {
        hacerAudio();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speech != null) {
            speech.destroy();
        }

    }
    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.destroy();
        }

    }
    @Override
    protected void onRestart() {
        hacerAudio();
        super.onRestart();
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

    //Abrir la actividad de relaciones espaciales
    public void abrirRelacionesEspaciales() {
        Intent intent = new Intent(MenuJuego.this, Relaciones_espaciales.class);
        startActivity(intent);
        speech.destroy();
    }
}
