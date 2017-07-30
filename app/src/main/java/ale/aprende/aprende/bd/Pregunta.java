package ale.aprende.aprende.bd;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ale.aprende.aprende.principal.MainActivity;

/**
 * Created by Alejandro on 11/06/2017.
 */

public class Pregunta {

    //Este metodo se encarga de llenar la tabla de preguntas
    public void llenarTablaPregunta(Context m) {
        DBHandler mdb = new DBHandler(m);
        SQLiteDatabase db = mdb.getWritableDatabase();
        int icount = verificarTablaPregunta(db);
        int id_subcategoria = 0;
        String[] audio = new String[]{"p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10"};
        String[] nombreImagen = new String[]{"abajo", "adelante", "arriba", "atras", "centro", "derecha", "izquierda"};

        for (int i = 0; i < nombreImagen.length; i++) {
            id_subcategoria++;
            List<String> listaArchivos = obtenerCantidadArchivos("relaciones_espaciales/" + nombreImagen[i], m);
            int cantidad = listaArchivos.size() / 2;
            for (int j = 0; j < cantidad; j++) {
                ContentValues values = new ContentValues();
                values.put("audio", audio[j]);
                values.put("nombre_imagen", audio[j] + "_" + nombreImagen[i]);
                values.put("id_subcategoria", id_subcategoria);
                db.insert("Pregunta", null, values);
            }
        }
        db.close();
    }

    //Este metodo se encarga insertar en base de datos las preguntas relacionadas a los colores
    public void llenarPreguntasColores(Context m) {
        DBHandler mdb = new DBHandler(m);
        SQLiteDatabase db = mdb.getWritableDatabase();
        String[] audio = new String[]{"p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10"};
        String[] nombre = new String[]{"azul", "amarillo", "rojo", "anaranjado", "verde"};
        int id = 8;
        for (int i = 0; i < nombre.length; i++) {
            List<String> listaArchivos = obtenerCantidadArchivos("colores/" + nombre[i], m);
            for (int j = 0; j < listaArchivos.size(); j++) {
                ContentValues values = new ContentValues();
                values.put("audio", audio[j]);
                values.put("nombre_imagen", audio[j] + "_" + nombre[i]);
                values.put("id_subcategoria", id);
                db.insert("Pregunta", null, values);
            }
            id++;
        }
    }

    //Verifica si en la tabla pregunta hay datos
    public int verificarTablaPregunta(SQLiteDatabase db) {
        String count = "SELECT count(*) FROM Pregunta";
        Cursor mcursor = db.rawQuery(count, null);
        mcursor.moveToFirst();
        return mcursor.getInt(0);
    }

    public void llenarPreguntasNumeros(Context m) {
        DBHandler mdb = new DBHandler(m);
        SQLiteDatabase db = mdb.getWritableDatabase();
        int id_subcategoria = 13;
        String[] audio = new String[]{"p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11", "p12", "p13", "p14", "p15", "p16", "p17", "p18", "p19", "p20", "p21", "p22", "p23", "p24", "p25", "p26", "p27", "p28", "p29", "p30"};
        for (int i = 0; i < audio.length; i++) {
            for (int j = 0; j <= 4; j++) {
                ContentValues values = new ContentValues();
                values.put("audio", "pregunta_numero");
                values.put("nombre_imagen", audio[i] + "_" + j);
                values.put("id_subcategoria", id_subcategoria);
                db.insert("Pregunta", null, values);
            }
            id_subcategoria++;
        }
        db.close();
    }

    //Este metodo se encarga de insertar las pregunra de las figuras geometricas en la tabla de pregunta
    public void llenarPreguntasFigurasGeomtricas(Context cm) {
        DBHandler mdb = new DBHandler(cm);
        SQLiteDatabase db = mdb.getWritableDatabase();
        int id_subcategoria = 44;
        String[] listaFiguraGeomtricas = new String[]{"circulo", "triangulo", "rectangulo", "cuadrado"};
        for (int i = 0; i < listaFiguraGeomtricas.length; i++) {
            for (int j = 1; j <= 10; j++) {
                ContentValues values = new ContentValues();
                values.put("audio", "p" + j);
                values.put("nombre_imagen", j + ".png");
                values.put("id_subcategoria", id_subcategoria);
                db.insert("Pregunta", null, values);
            }
            id_subcategoria++;
        }
        db.close();
    }

    //Este metodo se encarga de llenar las preguntas de la categoria del abecedario
    public void llenarPreguntasAbecedario(Context cm) {
        DBHandler mdb = new DBHandler(cm);
        SQLiteDatabase db = mdb.getWritableDatabase();
        int id_subcategoria = 48;
        String[] listaAbecedario = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "nn", "o", "p", "q", "r", "s",
                "t", "u", "v", "w", "x", "y", "z"};
        for (int i = 0; i < listaAbecedario.length; i++) {
            List datos = obtenerCantidadArchivos("abecedario/pregunta_abecedario/"+listaAbecedario[i], cm);
            for (int j = 1; j <= datos.size(); j++) {
                ContentValues values = new ContentValues();
                values.put("audio", "pregunta");
                values.put("nombre_imagen", j + ".png");
                values.put("id_subcategoria", id_subcategoria);
                db.insert("Pregunta", null, values);
            }
            id_subcategoria++;
        }
        db.close();
    }

    //Este metodo obtiene la cantidad de archivos que se encuentran en un folder en especifico
    private List obtenerCantidadArchivos(String tema, Context c) {
        AssetManager assetManager = c.getAssets();
        String[] archivos = new String[0];
        try {
            archivos = assetManager.list(tema);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> listaArchivos = new LinkedList<String>(Arrays.asList(archivos));
        return listaArchivos;
    }
}
