package ale.aprende.aprende.principal;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;


import ale.aprende.aprende.Relaciones_espaciales;
import ale.aprende.aprende.bd.Categoria;
import ale.aprende.aprende.Ingresar.Ingresar;
import ale.aprende.aprende.MenuJuego;
import ale.aprende.aprende.R;
import ale.aprende.aprende.bd.DBHandler;
import ale.aprende.aprende.bd.Pregunta;
import ale.aprende.aprende.bd.Respuesta;
import ale.aprende.aprende.registrar.Registrar;

public class MainActivity extends AppCompatActivity {
    //Declaración de variables
    private static final int SOLICITUD_PERMISO = 1;
    private BootstrapButton btnRegistrar, btnIngresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnRegistrar = (BootstrapButton) findViewById(R.id.btn_registrar);
        btnIngresar = (BootstrapButton) findViewById(R.id.btn_ingresar);

/*
        DBHandler mdb = new DBHandler(getApplicationContext());
        SQLiteDatabase db = mdb.getWritableDatabase();
        for (int i = 1; i <= 7; i++) {
            ContentValues values = new ContentValues();
            values.put("id_persona", 1);
            values.put("id_subcategoria", i);
            values.put("cantidad_preguntas", 3);
            if (i == 7)
                values.put("estado", false);
            else
                values.put("estado", true);

            values.put("cantidad_errores", 0);
           values.put("repeticion", 0);
            db.insert("Progreso", null, values);


                ContentValues est = new ContentValues();
                est.put("id_persona", 1);
                est.put("id_subcategoria", i);
                est.put("cantidad_preguntas", 3);
                if(i == 7){
                    est.put("estado", false);
                }else{
                    est.put("estado", true);
                }


                est.put("porcentaje", 0);
                est.put("cantidad_errores", 0);
                db.insert("Estadistica", null, est);


}
*/
/*
        Relaciones_espaciales r = new Relaciones_espaciales();
        int numero = r.sortear(4);
        numero++;
        if (numero == 1) {
            numero = 8;
        } else if (numero == 2) {
            numero = 9;
        } else if (numero == 3) {
            numero = 10;
        } else if (numero == 4) {
            numero = 11;
        } else if (numero == 5) {
            numero = 12;
        }
        ContentValues values = new ContentValues();
        values.put("id_persona", 1);
        values.put("id_subcategoria", numero);
        values.put("cantidad_preguntas", 3);
        values.put("estado", false);
        values.put("cantidad_errores", 0);
        db.insert("Progreso", null, values);
        */

        if (Build.VERSION.SDK_INT >= 23) {
            verificarPermiso();
        }
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_app);
        if (getString(R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }
    }

    //click en el boton para ingresar a la aplicación
    public void ingresarUsuario(View view) {
/*
                    Intent intento = new Intent(MainActivity.this, Ingresar.class);
                    startActivity(intento);
*/

        Intent intento = new Intent(MainActivity.this, MenuJuego.class);
        startActivity(intento);
    }


    //Click en el boton de registrar
    public void registrarUsuario(View view) {
        Intent intento = new Intent(MainActivity.this, Registrar.class);
        startActivity(intento);
    }

    //Verificar permiso
    private void verificarPermiso() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
        } else {
            explicarUsoPermiso();
            solicitarPermisoAlmacenamiento("Pedimos los permisos");
        }
    }

    //Se le explica al usuario brevemente el motivo para aceptar el permiso
    public boolean explicarUsoPermiso() {
        AlertDialog.Builder mensaje = new AlertDialog.Builder(this);
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            mensaje.setMessage("Se guardará la imagen de perfil en su dispositivo, y se realizará el uso de la cámara");
            cuadroMensajeBasico();
            mensaje.show();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            mensaje.setMessage("Grabación de audio en la aplicación");
            cuadroMensajeBasico();
            mensaje.show();
        }
        return true;
    }

    //Pedimos el permiso o los permisos con un cuadro dialogo  del sistema
    public boolean solicitarPermisoAlmacenamiento(String mensaje) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                SOLICITUD_PERMISO);
        //Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SOLICITUD_PERMISO) {
            if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                //Realizamos la accion permiso concedido
                Toast.makeText(this, "Permiso Concedido", Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        }
    }

    //El cuadro a mostrar en la pantalla para los permisos
    public void cuadroMensajeBasico() {
        AlertDialog.Builder mensaje = new AlertDialog.Builder(this);
        mensaje.setMessage("La imagen elegida, se guardara en su dispositivo mobil");
        mensaje.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        mensaje.show();
    }
}
