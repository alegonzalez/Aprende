package ale.aprende.aprende.Ingresar;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Created by Alejandro on 22/05/2017.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    //Declaración de variables
    private SurfaceHolder mHolder;
    private Camera mCamera;

    //Constructor
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Instala un SurfaceHolder.Callback para que seamos notificados cuando el
        // se crea y destruye la superficie subyacente.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    //La superficie se ha creado, ahora decir a la cámara donde dibujar la vista previa.
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error al configurar la vista previa de la cámara:" + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            //no  existe la vista
            return;
        }
        // Detener la vista previa antes de realizar cambios
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        // Establecer el tamaño de vista previa y hacer cualquier cambio de tamaño, rotación o  cambios de formato aquí
        // Iniciar la vista previa con la nueva configuración
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error al iniciar la previsualización de la cámara: " + e.getMessage());
        }
    }
}
