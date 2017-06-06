package ale.aprende.aprende.principal;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;


import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.OnBoomListener;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.nightonke.boommenu.Util;

import java.util.ArrayList;

import ale.aprende.aprende.Abecedario;
import ale.aprende.aprende.BuilderManager;
import ale.aprende.aprende.Categoria;
import ale.aprende.aprende.Colores;
import ale.aprende.aprende.Figuras_geometricas;
import ale.aprende.aprende.Ingresar.Ingresar;
import ale.aprende.aprende.MenuJuego;
import ale.aprende.aprende.Numeros;
import ale.aprende.aprende.R;
import ale.aprende.aprende.Relaciones_espaciales;
import ale.aprende.aprende.registrar.DBHandler;
import ale.aprende.aprende.registrar.Registrar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //Declaración de variables
    private BoomMenuButton bmb;
    private static final int SOLICITUD_PERMISO = 1;
    private ArrayList<Pair> piecesAndButtons = new ArrayList<>();

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
        bmb = (BoomMenuButton) findViewById(R.id.botonPrinipal);
        assert bmb != null;
        bmb.setButtonEnum(ButtonEnum.Ham);
        bmb.setPiecePlaceEnum(PiecePlaceEnum.HAM_2);
        bmb.setButtonPlaceEnum(ButtonPlaceEnum.HAM_2);
        bmb.setBottomHamButtonTopMargin(Util.dp2px(50));
        bmb.setUse3DTransformAnimation(true);
        ListView listView = (ListView) findViewById(R.id.list_view);
        assert listView != null;
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1,
                BuilderManager.getHamButtonData(piecesAndButtons)));
        cargarBotones(bmb);
        listView.setVisibility(View.GONE);
        MenuJuego mn = new MenuJuego();
        Animation resultado = mn.elaborarAnimacion(bmb);
        bmb.startAnimation(resultado);
        listenClickEventOf(R.id.botonPrinipal);
        bmb.setAutoBoom(true);

        //Evento click en el botón
        bmb.setOnBoomListener(new OnBoomListener() {
            @Override
            public void onClicked(int index, BoomButton boomButton) {
                Toast.makeText(MainActivity.this, "HOLA", Toast.LENGTH_SHORT).show();
                if (index == 0) {
                    Intent intento = new Intent(MainActivity.this, Registrar.class);
                    startActivity(intento);
                } else {
                    Intent intento = new Intent(MainActivity.this, Ingresar.class);
                    startActivity(intento);
                }
            }

            @Override
            public void onBackgroundClick() {

            }

            @Override
            public void onBoomWillHide() {

            }

            @Override
            public void onBoomDidHide() {

            }

            @Override
            public void onBoomWillShow() {

            }

            @Override
            public void onBoomDidShow() {

            }
        });

/*
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                levantarMenu();
            }
        }, 100);
        */
    }

    /*
        public void levantarMenu() {
            bmb.isBoomed();    // Whether the BMB is boomed.
            bmb.isReBoomed();
            Toast.makeText(this, "HOLAA", Toast.LENGTH_SHORT).show();
            bmb.setOnClickListener(this);
        }
    */

    private void listenClickEventOf(int bmb) {
        findViewById(bmb).setOnClickListener(this);
    }

    //Carga los botones en donde aparace las opciones de registrar e ingresar
    private void cargarBotones(BoomMenuButton boton) {
        int posicion = 5;
        boton.setPiecePlaceEnum((PiecePlaceEnum) piecesAndButtons.get(posicion).first);
        boton.setButtonPlaceEnum((ButtonPlaceEnum) piecesAndButtons.get(posicion).second);
        boton.clearBuilders();
        for (int i = 0; i < boton.getPiecePlaceEnum().pieceNumber(); i++)
            if (i == 0) {
                boton.addBuilder(BuilderManager.getHamButtonBuilder(R.string.registrar, R.drawable.registrar, 0));
            } else {
                boton.addBuilder(BuilderManager.getHamButtonBuilder(R.string.ingresar, R.drawable.ingresar, 0));
            }
    }


    //Verificar permiso
    private void verificarPermiso() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
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
        }
        return true;
    }

    //Pedimos el permiso o los permisos con un cuadro dialogo  del sistema
    public boolean solicitarPermisoAlmacenamiento(String mensaje) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                SOLICITUD_PERMISO);
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
        return true;
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

    @Override
    public void onClick(View v) {

    }
}
