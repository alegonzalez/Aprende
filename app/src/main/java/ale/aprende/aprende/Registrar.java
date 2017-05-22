package ale.aprende.aprende;

import android.content.Intent;
import android.graphics.Bitmap;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar);
        rbtFemenina = (RadioButton) findViewById(R.id.rbtFemenina);
        rbtMasculino = (RadioButton) findViewById(R.id.rbtMasculino);
        //evento del radio button en caso que eliga como genero niña
        rbtFemenina.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && rbtFemenina.isChecked()) {
                    rbtMasculino.setChecked(false);
                }
                // rbtMasculino.setChecked(false);
                Toast.makeText(Registrar.this, "Radiobutton niña", Toast.LENGTH_SHORT).show();
                //rbtFemenina.isChecked();

            }
        });
        //evento del radio button en caso que eliga como genero niño
        rbtMasculino.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && rbtMasculino.isChecked()) {
                    rbtFemenina.setChecked(false);
                }
                Toast.makeText(Registrar.this, "Radiobutton niño", Toast.LENGTH_SHORT).show();
                //rbtMasculino.isChecked();
                // rbtFemenina.setChecked(false);
            }
        });
    }

    //evento onclick para cargar la foto
    public void cargarFoto(View view) {
        Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
        gallIntent.setType("image/*");
        startActivityForResult(Intent.createChooser(gallIntent, "Seleccione la imagen"), ELEGIR_IMAGEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ELEGIR_IMAGEN && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = (ImageView) findViewById(R.id.imgPerfil);
                imageView.setImageBitmap(bitmap);
                //detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
