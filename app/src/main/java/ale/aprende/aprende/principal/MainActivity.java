package ale.aprende.aprende.principal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import ale.aprende.aprende.Categoria;
import ale.aprende.aprende.Ingresar.Ingresar;
import ale.aprende.aprende.R;
import ale.aprende.aprende.registrar.DBHandler;
import ale.aprende.aprende.registrar.Registrar;

public class MainActivity extends AppCompatActivity {
    //Declaración de variables
    ImageButton btnIngresar;
    ImageButton btnRegistrar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_app);
        if (getString(R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }
        DBHandler db1 = new DBHandler(this);
        SQLiteDatabase db = db1.getWritableDatabase();
        if (db != null) {
            Categoria categoria = new Categoria(db);
            categoria.llenarTablaCategoria();
        }
        db.close();
        btnRegistrar = (ImageButton) findViewById(R.id.registrar);
        btnIngresar = (ImageButton) findViewById(R.id.ingresar);
    }

    //onclick registrar

    public void registrar(View view) {
        Intent intent = new Intent(MainActivity.this, Registrar.class);
        startActivity(intent);
    }

    //onclick para ingresar
    public void ingresar(View view) {
        Intent intent = new Intent(MainActivity.this, Ingresar.class);
        startActivity(intent);
    }
}
