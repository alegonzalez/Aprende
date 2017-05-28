package ale.aprende.aprende.principal;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
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
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import ale.aprende.aprende.Categoria;
import ale.aprende.aprende.Ingresar.Ingresar;
import ale.aprende.aprende.R;
import ale.aprende.aprende.registrar.DBHandler;
import ale.aprende.aprende.registrar.Registrar;

public class MainActivity extends AppCompatActivity {
    //Declaración de variables
    private ImageButton btnIngresar, btnRegistrar;
    private static final int SOLICITUD_PERMISO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean result = true;
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

    //Verificar permiso
    private void verificarPermiso() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
        } else {
            explicarUsoPermiso();
            solicitarPermisoAlmacenamiento();
        }
    }

    //Se le explica al usuario brevemente el motivo para aceptar el permiso
    private void explicarUsoPermiso() {
        AlertDialog.Builder mensaje = new AlertDialog.Builder(this);
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            mensaje.setMessage("Se guardará la imagen de perfil en su dispositivo, y se realizará el uso de la cámara");
            cuadroMensajeBasico();
            mensaje.show();
        }

    }

    //Pedimos el permiso o los permisos con un cuadro dialogo  del sistema
    private void solicitarPermisoAlmacenamiento() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                SOLICITUD_PERMISO);
        Toast.makeText(this, "Pedimos los permisos", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SOLICITUD_PERMISO) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
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
