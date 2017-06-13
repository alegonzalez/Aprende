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
        if (icount <= 0) {
            for (int i = 0; i < nombreImagen.length; i++) {
                id_subcategoria++;
                List<String> listaArchivos = obtenerCantidadArchivos(nombreImagen[i], m);
                int cantidad = listaArchivos.size() / 2;
                for (int j = 0; j < cantidad; j++) {
                    ContentValues values = new ContentValues();
                    values.put("audio", audio[j]);
                    values.put("nombre_imagen", audio[j] + "_" + nombreImagen[i]);
                    values.put("id_subcategoria", id_subcategoria);
                    db.insert("Pregunta", null, values);
                }
            }
        }
        db.close();
    }

    //Este metodo obtiene la cantidad de archivos que se encuentran en un folder en especifico
    private List obtenerCantidadArchivos(String tema, Context c) {
        AssetManager assetManager = c.getAssets();
        String[] archivos = new String[0];
        try {
            archivos = assetManager.list("relaciones_espaciales/" + tema);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> listaArchivos = new LinkedList<String>(Arrays.asList(archivos));
        return listaArchivos;
    }
    //Verifica si en la tabla pregunta hay datos
    private int verificarTablaPregunta(SQLiteDatabase db) {
        String count = "SELECT count(*) FROM Pregunta";
        Cursor mcursor = db.rawQuery(count, null);
        mcursor.moveToFirst();
        return mcursor.getInt(0);
    }

}
