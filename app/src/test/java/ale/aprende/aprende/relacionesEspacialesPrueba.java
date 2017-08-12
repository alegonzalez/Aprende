package ale.aprende.aprende;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by Alejandro on 09/08/2017.
 */

public class relacionesEspacialesPrueba {
    Relaciones_espaciales r = new Relaciones_espaciales();
    //Verifica si se envia lleno un arreglo
    @Test
    public void verificarCantidadArreglo() throws Exception {
        String[] datos = new String[]{"1","3"};
        assertEquals(2 ,r.verificarCantidadArreglo(datos));
    }
    //Verifica se se enviar vacio un arreglo
    @Test
    public void verificarCantidadArregloVacio() throws Exception {
        String[] datos = new String[]{};
        assertEquals(0 ,r.verificarCantidadArreglo(datos));
    }
    //Verifica se elimine correctamente los valores vacios en el arreglo
    @Test
    public void eliminarValoresArreglo() throws Exception {
        String[] datos = new String[]{"1","2",""};
        String[] resultado = new String[]{"1","2"};
        int cantidad = r.verificarCantidadArreglo(datos);
        assertArrayEquals(resultado ,r.eliminarValoresArreglo(datos,cantidad));
    }
    //Verifica que al realizar el random no sea un -1
    @Test
    public void verificarRandom() throws Exception {
        assertThat("resultado diferente a -1 ",-1,is(not(r.sortear(10))));
    }

}
