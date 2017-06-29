package ale.aprende.aprende.bd;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alejandro on 09/05/2017.
 */

public class Categoria {
    //Declaración de variables
    private SQLiteDatabase db;
    public Categoria(SQLiteDatabase db) {
        this.db = db;
    }

    //Este metodo verifica que si la tabla de categoria esta vacia, la llene con los datos del metodo llenarLista
    public void llenarTablaCategoria() {
        List<String> lista_categorias = llenarListaCategoria();
        String count = "SELECT count(*) FROM Categoria";
        Cursor mcursor = this.db.rawQuery(count, null);
        mcursor.moveToFirst();
        int icount = mcursor.getInt(0);
        if (icount <= 0) {
            for (int i = 0; i < lista_categorias.size(); i++) {
                db.execSQL("INSERT INTO Categoria (nombre)" + "values ( '" + lista_categorias.get(i) + "')");
            }
        }
        //Metodo para insertar en la tabla subcategoria
        llenarListaSubcategotias();
    }

    //Se encarga de insertar todas las subcategorias de cada categoria
    private void llenarListaSubcategotias() {

        String count = "SELECT count(*) FROM SubCategoria";
        Cursor mcursor = this.db.rawQuery(count, null);
        mcursor.moveToFirst();
        int icount = mcursor.getInt(0);
        if (icount <= 0) {
            List<String> lista = new ArrayList<String>();
            //relaciones espaciales
            lista.add("Abajo");
            lista.add("Adelante");
            lista.add("Arriba");
            lista.add("Atras");
            lista.add("Centro");
            lista.add("Derecha");
            lista.add("Izquierda");
            llenarTablaSubCategoria(lista,1);
            lista.removeAll(lista);
            lista.add("Azul");
            lista.add("Amarillo");
            lista.add("Rojo");
            lista.add("Anaranjado");
            lista.add("Verde");
            llenarTablaSubCategoria(lista,2);
            lista.removeAll(lista);
            lista.add("1");
            lista.add("2");
            lista.add("3");
            lista.add("4");
            lista.add("5");
            lista.add("6");
            lista.add("7");
            lista.add("8");
            lista.add("9");
            lista.add("10");
            lista.add("11");
            lista.add("12");
            lista.add("13");
            lista.add("14");
            lista.add("15");
            lista.add("16");
            lista.add("17");
            lista.add("18");
            lista.add("19");
            lista.add("20");
            lista.add("21");
            lista.add("22");
            lista.add("23");
            lista.add("24");
            lista.add("25");
            lista.add("26");
            lista.add("27");
            lista.add("28");
            lista.add("29");
            lista.add("30");
            llenarTablaSubCategoria(lista,3);
            lista.removeAll(lista);
            lista.add("Circulo");
            lista.add("Triangulo");
            lista.add("Rectangulo");
            lista.add("Cuadrado");
            llenarTablaSubCategoria(lista,4);
            lista.removeAll(lista);
            lista.add("Aa");
            lista.add("Bb");
            lista.add("Cc");
            lista.add("Dd");
            lista.add("Ee");
            lista.add("Ff");
            lista.add("Gg");
            lista.add("Hh");
            lista.add("Ii");
            lista.add("Jj");
            lista.add("Kk");
            lista.add("Ll");
            lista.add("Mm");
            lista.add("Nn");
            lista.add("Ññ");
            lista.add("Oo");
            lista.add("Pp");
            lista.add("Qq");
            lista.add("Rr");
            lista.add("Ss");
            lista.add("Tt");
            lista.add("Uu");
            lista.add("Vv");
            lista.add("Ww");
            lista.add("Xx");
            lista.add("Yy");
            lista.add("Zz");
            llenarTablaSubCategoria(lista,5);
            lista.removeAll(lista);
        }
    }

    private void llenarTablaSubCategoria(List<String> lista_subcategorias,int id) {
        for (int i = 0; i < lista_subcategorias.size(); i++) {
            db.execSQL("INSERT INTO SubCategoria (nombre,id_categoria)" + "values ( '" + lista_subcategorias.get(i) + "',"+id+")");
        }
    }


    //Este metodo se encarga de llenar la lista con la categoria
    private List<String> llenarListaCategoria() {
        List<String> lista = new ArrayList<String>();
        lista.add("Relaciones espaciales");
        lista.add("colores");
        lista.add("Numeros");
        lista.add("Figuras geometricas");
        lista.add("Abecedario");
        return lista;
    }
}
