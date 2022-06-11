package com.temperature.red;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashScreen extends Activity {
    private static final String PREFS_USER_PREF = "TimeOut";

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.splash);
        TextView textView = (TextView) findViewById(R.id.textView3);
        ((ImageView) findViewById(R.id.imageView3)).startAnimation(AnimationUtils.loadAnimation(this, R.anim.side_slide));
        new Thread() {
            public void run() {
                Intent intent;
                try {
                    sleep(3000);
                    intent = new Intent(SplashScreen.this, DeviceList.class);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    intent = new Intent(SplashScreen.this, DeviceList.class);
                } catch (Throwable th) {
                    SplashScreen.this.startActivity(new Intent(SplashScreen.this, DeviceList.class));
                    throw th;
                }
                SplashScreen.this.startActivity(intent);
            }
        }.start();
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        finish();
    }

    public static int getCounter(Context context) {
        return context.getSharedPreferences(PREFS_USER_PREF, 0).getInt("Timelapse", 0);
    }

    public static void setCounter(Context context, int pos) {
        Log.d("ContentValues", "Timelapse: " + pos);
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_USER_PREF, 0).edit();
        editor.putInt("Timelapse", pos);
        editor.commit();
    }
}
