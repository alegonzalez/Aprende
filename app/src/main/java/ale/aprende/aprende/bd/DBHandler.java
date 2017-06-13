package ale.aprende.aprende.bd;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.AccessControlContext;

import static android.R.id.list;

/**
 * Created by Alejandro on 07/05/2017.
 */

public class DBHandler extends SQLiteOpenHelper {
    //Declaración de las variables
    //version de la base de datos
    private static final int VERSION_BASE_DATOS = 1;
    //Nombre de la base de datos
    private static final String NOMBRE_BASE_DATOS = "aprende";
    //Sql de la creacion de la tabla persona
    private String persona = "CREATE TABLE Persona(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, genero TEXT, imagen TEXT, rostro TEXT,rostro_detectado TEXT)";
    //Sql de la creación de la tabla categoria
    private String categoria = "CREATE TABLE Categoria (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT);";
    //Sql de la creación tabla subcategoria
    private String sub_categoria = "create table SubCategoria ( id  INTEGER PRIMARY KEY autoincrement, "
            + "nombre " + " TEXT  null, "
            + "id_categoria" + " INTEGER NOT NULL, "
            + " FOREIGN KEY (" + "id_categoria" + ") REFERENCES " + "Categoria" + "(" + "id" + "));";
    //Sql de la creación tabla pregunta
    private String pregunta = "CREATE TABLE Pregunta " +
            "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,audio TEXT,nombre_imagen TEXT,id_subcategoria INTEGER NOT NULL," +
            "FOREIGN KEY (id_subcategoria) REFERENCES  SubCategoria ( id));" + ")";
    //Sql de  la creación de la tabla intermedia entre usuario y pregunta
    private String persona_pregunta = "CREATE TABLE Persona_Pregunta (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,id_persona INTEGER NOT NULL,id_pregunta INTEGER NOT NULL,estado BOOLEAN," +
            " FOREIGN KEY (id_persona) REFERENCES Persona(id) " + "," + " FOREIGN KEY (id_pregunta) REFERENCES Pregunta(id)" + ")";
    //Sql de  la creación de la tabla  entre imagen_respuesta
    private String imagen_respuesta = "CREATE TABLE Imagen_Respuesta (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,id_pregunta INTEGER NOT NULL,nombre_imagen TEXT ,estado BOOLEAN,audio TEXT,"+
            " FOREIGN KEY (id_pregunta) REFERENCES Pregunta(id))";
    //Sql de la creación de la tabla intermedia entre imagen de respuesta y la tabla de pregunta
    //Sql creación de la tabla estadistica
    private String estadistica = "CREATE TABLE Estadistica (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, id_pregunta INTEGER, cantidad_errores INTEGER , id_persona INTEGER NOT NULL, " + " FOREIGN KEY (id_pregunta) REFERENCES Pregunta (id)" + " " + "" +
            "FOREIGN KEY (id_persona) REFERENCES Persona (id)" + ");";
    private String progreso = "CREATE TABLE Progreso (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, id_persona INTEGER NOT NULL, id_subcategoria INTEGER NOT NULL, cantidad_preguntas, estado BOOLEAN, " + " FOREIGN KEY (id_persona) REFERENCES Persona (id)" + " " + "" +
            "FOREIGN KEY (id_subcategoria) REFERENCES SubCategoria (id)" + ");";
    SQLiteDatabase db;

    public DBHandler(Context context) {
        super(context, NOMBRE_BASE_DATOS, null, VERSION_BASE_DATOS);
        db = this.getWritableDatabase();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(persona);
        db.execSQL(categoria);
        db.execSQL(sub_categoria);
        db.execSQL(pregunta);
        db.execSQL(imagen_respuesta);
        db.execSQL(persona_pregunta);
        db.execSQL(estadistica);
        db.execSQL(progreso);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Eliminar la tabla anterior si existe
        db.execSQL("DROP TABLE IF EXISTS " + persona);
        db.execSQL("DROP TABLE IF EXISTS " + categoria);
        db.execSQL("DROP TABLE IF EXISTS " + sub_categoria);
        db.execSQL("DROP TABLE IF EXISTS " + pregunta);
        db.execSQL("DROP TABLE IF EXISTS " + imagen_respuesta);
        db.execSQL("DROP TABLE IF EXISTS " + persona_pregunta);
        db.execSQL("DROP TABLE IF EXISTS " + estadistica);
        db.execSQL("DROP TABLE IF EXISTS " + progreso);
        // Creando las tablas de nuevo
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }
}
