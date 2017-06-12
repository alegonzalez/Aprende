package ale.aprende.aprende;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ale.aprende.aprende.bd.DBHandler;

public class Relaciones_espaciales extends AppCompatActivity {
    //Declaración de variables
    private int id_usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relaciones_espaciales);
        id_usuario = getIntent().getExtras().getInt("id_usuario");
        List<String> subcategoria = obtenerProgreso();
        verificarTipoSubcategoria(subcategoria);
    }

    //Verifica las preguntas realizada segun la subcategoria
    private Cursor obtenerPreguntasRealizadas(int id_subcategoria) {
        List<String> preguntas = new ArrayList<String>();
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from  Pregunta  " +
                "where  id_subcategoria= " + id_subcategoria + "and id", null);
        return cursor;
    }

    //Realiza las preguntas segun tema
    public void realizarPreguntas(Cursor cursor, int id_subcategoria, String nombre_subcategoria) {
        if (!(cursor.moveToFirst()) || cursor.getCount() == 0) {
            try {
                AssetManager assetManager = getAssets();
                Toast.makeText(this, "Entro", Toast.LENGTH_SHORT).show();
                String[] files = assetManager.list("relaciones_espaciales");
                InputStream input = assetManager.open("abajo");
                if (input.read() != -1) {
                    for (int i = 0; i > input.read(); i++) {

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Verificar el tipo de subcategoria
    public void verificarTipoSubcategoria(List<String> subcategoria) {
        if ((subcategoria.get(1)).trim().equals("Abajo")) {
            Cursor cursor = obtenerPreguntasRealizadas(Integer.parseInt(subcategoria.get(0)));
            realizarPreguntas(cursor, Integer.parseInt(subcategoria.get(0)), "Abajo");
        } else if ((subcategoria.get(1)).trim().equals("Adelante")) {

        } else if ((subcategoria.get(1)).trim().equals("Arriba")) {

        } else if ((subcategoria.get(1)).trim().equals("Atras")) {

        } else if ((subcategoria.get(1)).trim().equals("Centro")) {

        } else if ((subcategoria.get(1)).trim().equals("Derecha")) {

        } else if ((subcategoria.get(1)).trim().equals("Izquierda")) {

        }
    }

    // Obtiene el progreso acerca del tema
    public List obtenerProgreso() {
        List<String> subcategoria = new ArrayList<String>();
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        Cursor cursor = db.rawQuery("select p.id_subcategoria,p.cantidad_preguntas,sub.nombre " +
                " from Progreso p, SubCategoria sub " +
                "where p.id_subcategoria = sub.id and " +
                " estado= " + false, null);
        if (cursor.moveToFirst()) {
            subcategoria.add(cursor.getString(cursor.getColumnIndex("id_subcategoria")));
            subcategoria.add(cursor.getString(cursor.getColumnIndex("nombre")));
        }
        db.close();
        return subcategoria;
    }


    //Evento si se realiza un retroceso desde el dispositivo
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Relaciones_espaciales.this, MenuJuego.class);
        startActivity(intent);
        super.onBackPressed();
    }
}
