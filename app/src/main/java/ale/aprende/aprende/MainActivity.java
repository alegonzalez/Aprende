package ale.aprende.aprende;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import static android.R.id.message;
import static android.provider.AlarmClock.EXTRA_MESSAGE;

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
/*
    public void registrar(View view) {
        Intent intent = new Intent(MainActivity.this, registrar.class);
        startActivity(intent);
    }
    */
    //onclick para ingresar
    public void ingresar(View view){
        Intent intent = new Intent(MainActivity.this, Ingresar.class);
        startActivity(intent);
    }
}
