package ale.aprende.aprende;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.IOException;

public class Registrar extends AppCompatActivity {
    //Declaración de variables
    private final int ELEGIR_IMAGEN = 1;
    public RadioButton rbtFemenina, rbtMasculino;
    public ImageView imageView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar);
        rbtFemenina = (RadioButton) findViewById(R.id.rbtFemenina);
        rbtMasculino = (RadioButton) findViewById(R.id.rbtMasculino);
        imageView = (ImageView) findViewById(R.id.imgPerfil);
        //evento del radio button en caso que eliga como genero niña
        rbtFemenina.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && rbtFemenina.isChecked()) {
                    rbtMasculino.setChecked(false);
                }
            }
        });
        //evento del radio button en caso que eliga como genero niño
        rbtMasculino.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && rbtMasculino.isChecked()) {
                    rbtFemenina.setChecked(false);
                }
            }
        });
    }

    //evento onclick para cargar la foto
    public void cargarFoto(View view) {
        Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
        gallIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(gallIntent, "Seleccione la imagen"), ELEGIR_IMAGEN);
    }

    //Se ejecuta cuando se selecciona la imagen a cargar desde galeria
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ELEGIR_IMAGEN && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageView = (ImageView) findViewById(R.id.imgPerfil);
                imageView.setImageBitmap(bitmap);

                //detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //metodo onclick para registrar
    public void registrar(View view) {
        if (!(validarSeleccionImagen())) {
            Toast.makeText(this, "Debes seleccionar una foto de perfil del niño", Toast.LENGTH_LONG).show();
            return;
        } else if (!(validarSeleccionGenero())) {
            Toast.makeText(this, "Debes seleccionar el género", Toast.LENGTH_LONG).show();
            return;
        }
    }

    //valida que se halla seleccionado la imagen del perfil del niño
    public boolean validarSeleccionImagen() {
        if (null != imageView.getDrawable()) {
            return true;
        } else {
            return false;
        }
    }

    //valida que se halla seleccionado el genero
    public boolean validarSeleccionGenero() {
        if (rbtMasculino.isChecked() || rbtFemenina.isChecked()) {
            return true;
        } else {
            return false;
        }
    }
}
