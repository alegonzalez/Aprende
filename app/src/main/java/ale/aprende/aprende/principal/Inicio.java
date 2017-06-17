package ale.aprende.aprende.principal;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

import ale.aprende.aprende.MenuJuego;
import ale.aprende.aprende.R;
import ale.aprende.aprende.bd.Categoria;
import ale.aprende.aprende.bd.DBHandler;
import ale.aprende.aprende.bd.Pregunta;
import ale.aprende.aprende.bd.Respuesta;

public class Inicio extends AppCompatActivity {
    public static final int segundos = 8;
    public static final int milisegundos = segundos * 1000;
    public ProgressBar pbProgreso;
    public static final int retardo = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        pbProgreso = (ProgressBar) findViewById(R.id.pgCarga);
        pbProgreso.setMax(maximo_pregreso());
        pbProgreso.setScaleY(4f);
        execWithThread();
        iniciarAnimacion();
    }


    public void execWithThread() {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        DBHandler db1 = new DBHandler(getApplicationContext());
                        SQLiteDatabase db = db1.getWritableDatabase();
                        if (db != null) {
                            Categoria categoria = new Categoria(db);
                            categoria.llenarTablaCategoria();
                            Pregunta p = new Pregunta();
                            p.llenarTablaPregunta(getApplicationContext());
                            Respuesta r = new Respuesta();
                            r.llenarTablaRespuesta(getApplicationContext());
                        }
                        db.close();
                    }
                }
        ).start();
    }

    private void iniciarAnimacion() {
        new CountDownTimer(milisegundos, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                pbProgreso.setProgress(establecer_proceso(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                Intent intent = new Intent(Inicio.this, MainActivity.class);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
                finish();
            }
        }.start();
    }

    // Se encarga de ir llenando el progreso de carga
    private int establecer_proceso(long milis) {
        return (int) ((milisegundos - milis) / 1000);
    }

    //El maximo progreso que va a ir teniendo la barra de progreso
    private int maximo_pregreso() {
        return segundos - retardo;
    }
}
