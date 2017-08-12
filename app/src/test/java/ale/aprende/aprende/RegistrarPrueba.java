package ale.aprende.aprende;

import org.junit.Test;

import ale.aprende.aprende.registrar.Registrar;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Alejandro on 07/08/2017.
 */

public class RegistrarPrueba {
    Registrar r = new Registrar();
    @Test
    public void establecerInformacion() throws Exception {
        assertTrue(r.establecerInformacion("HOLA"));
    }
}
