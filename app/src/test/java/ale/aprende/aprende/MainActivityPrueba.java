package ale.aprende.aprende;

import android.os.Build;

import junit.framework.Assert;

import org.junit.Test;

import ale.aprende.aprende.principal.MainActivity;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by Alejandro on 30/05/2017.
 */

public class MainActivityPrueba {
    MainActivity mn = new MainActivity();

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }
    //Se prueba el metodo de explicación de permisos
    @Test
    public void mostrarMensaje() {
        assertTrue(mn.explicarUsoPermiso());
    }


}
