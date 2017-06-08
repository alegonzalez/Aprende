package ale.aprende.aprende;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class Relaciones_espaciales extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relaciones_espaciales);
        int value = getIntent().getExtras().getInt("id_usuario");
        Toast.makeText(this, ""+value, Toast.LENGTH_SHORT).show();
    }
}
