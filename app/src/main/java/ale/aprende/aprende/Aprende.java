package ale.aprende.aprende;


import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;


/**
 * Created by Alejandro on 24/05/2017.
 */

public class Aprende extends MultiDexApplication {
   @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}