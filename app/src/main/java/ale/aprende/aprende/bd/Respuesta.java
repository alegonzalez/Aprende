package ale.aprende.aprende.bd;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alejandro on 11/06/2017.
 */

public class Respuesta {

    //Llena tabla de respuestas en lo que son relaciones espaciales
    public void llenarTablaRespuesta(Context m) {
        DBHandler mdb = new DBHandler(m);
        SQLiteDatabase db = mdb.getWritableDatabase();
        List<String> listaArchivos = new ArrayList<>();
        int cantidad = verificarTablaRespuesta(db);
        if (cantidad <= 0) {
            Cursor cursor = db.rawQuery("select id,id_subcategoria,nombre_imagen  from Pregunta", null);
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    String id = cursor.getString(cursor.getColumnIndex("id"));
                    String id_subcategoria = cursor.getString(cursor.getColumnIndex("id_subcategoria"));
                    String nombre_imagen = cursor.getString(cursor.getColumnIndex("nombre_imagen"));
                    listaArchivos = verificarSubcategoria("relaciones_espaciales/", id_subcategoria, nombre_imagen, m);
                    List<String> listaAudios = verificarSubcategoria("relaciones_espaciales/audios_respuesta_relaciones_espaciales/", id_subcategoria, nombre_imagen, m);
                    Boolean estado = false;
                    for (int i = 0; i < listaArchivos.size(); i++) {
                        estado = false;
                        if (id_subcategoria.equals("6") && listaAudios.get(i).equals("derecha.mp3")) {
                            estado = true;
                        } else if (id_subcategoria.equals("7") && listaAudios.get(i).equals("izquierda.mp3")) {
                            estado = true;
                        } else {
                            String[] resultado = listaArchivos.get(i).split("\\.");
                            String verificar = resultado[0].substring(resultado[0].length() - 1);
                            estado = (verificar.equals("v")) ? true : false;
                        }
                        ContentValues values = new ContentValues();
                        values.put("id_pregunta", id);
                        values.put("nombre_imagen", listaArchivos.get(i));
                        values.put("estado", estado);
                        values.put("audio", listaAudios.get(i));
                        db.insert("Imagen_Respuesta", null, values);
                    }
                    cursor.moveToNext();
                }
            }
        }

    }

    //Verifica el tipo de subcategoria  para asignar respuesta
    private List verificarSubcategoria(String direccion, String id_subcategoria, String nombre_imagen, Context m) {
        List<String> listaArchivos = new ArrayList<>();
        if (id_subcategoria.equals("1")) {
            listaArchivos = obtenerRespuestas(direccion, "abajo", m, nombre_imagen, id_subcategoria);
        } else if (id_subcategoria.equals("2")) {
            listaArchivos = obtenerRespuestas(direccion, "adelante", m, nombre_imagen, id_subcategoria);
        } else if (id_subcategoria.equals("3")) {
            listaArchivos = obtenerRespuestas(direccion, "arriba", m, nombre_imagen, id_subcategoria);
        } else if (id_subcategoria.equals("4")) {
            listaArchivos = obtenerRespuestas(direccion, "atras", m, nombre_imagen, id_subcategoria);
        } else if (id_subcategoria.equals("5")) {
            listaArchivos = obtenerRespuestas(direccion, "centro", m, nombre_imagen, id_subcategoria);
        } else if (id_subcategoria.equals("6")) {
            listaArchivos = obtenerRespuestas(direccion, "derecha", m, nombre_imagen, id_subcategoria);
        } else if (id_subcategoria.equals("7")) {
            listaArchivos = obtenerRespuestas(direccion, "izquierda", m, nombre_imagen, id_subcategoria);
        }
        return listaArchivos;

    }

    //Obtiene los archivos desde la carpeta almacenados
    private List obtenerRespuestas(String direccion, String subcategoria, Context m, String nombre_imagen, String idSubcategoria) {
        AssetManager assetManager = m.getAssets();
        String[] archivos = new String[0];
        String[] numero = nombre_imagen.split("_");
        String resultado = numero[0].substring(1);
        try {
            if (idSubcategoria.equals("6") && !(direccion.equals("relaciones_espaciales/")) || idSubcategoria.equals("7") && !(direccion.equals("relaciones_espaciales/"))) {
                archivos = assetManager.list(direccion + subcategoria);
            } else {
                archivos = assetManager.list(direccion + subcategoria + "/" + "r" + resultado);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> listaArchivos = new LinkedList<String>(Arrays.asList(archivos));
        return listaArchivos;
    }

    //Verificar si hay datos en la tabla de respuesta
    private int verificarTablaRespuesta(SQLiteDatabase db) {
        String count = "SELECT count(*) FROM Imagen_Respuesta";
        Cursor mcursor = db.rawQuery(count, null);
        mcursor.moveToFirst();
        return mcursor.getInt(0);
    }

}
