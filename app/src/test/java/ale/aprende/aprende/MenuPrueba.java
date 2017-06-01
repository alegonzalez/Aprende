package ale.aprende.aprende;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Alejandro on 30/05/2017.
 */

public class MenuPrueba {
    @Test
    public void verificarIdUsuario() {
        MenuJuego menu = new MenuJuego();
        assertTrue("Error, random is too high", menu.obtenerIdUsuario() > 0);
    }
}
