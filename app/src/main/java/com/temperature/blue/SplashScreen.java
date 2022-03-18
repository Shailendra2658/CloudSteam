package com.temperature.blue;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import static android.content.ContentValues.TAG;

/**
 * Created by Shailendra on 3/30/2021.
 */
public class SplashScreen extends Activity {

    private static final String PREFS_USER_PREF = "TimeOut";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.splash);
        // HERE WE ARE TAKING THE REFERENCE OF OUR IMAGE
        // SO THAT WE CAN PERFORM ANIMATION USING THAT IMAGE
        ImageView backgroundImage= (ImageView) findViewById(R.id.imageLogo);
        Animation slideAnimation = AnimationUtils.loadAnimation(this, R.anim.side_slide);
        backgroundImage.startAnimation(slideAnimation);
        Thread timerThread = new Thread(){
            public void run(){
                try{
                    sleep(3000);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }finally{
                    if(getCounter(SplashScreen.this)<20) {
                        setCounter(SplashScreen.this, getCounter(SplashScreen.this)+1);
                        Intent intent = new Intent(SplashScreen.this, DeviceList.class);
                        startActivity(intent);
                    }else
                        finish();

                }
            }
        };
        timerThread.start();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }

    public static int getCounter(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFS_USER_PREF, Context.MODE_PRIVATE);

        return pref.getInt("Timelapse", 0);
    }

    public static void setCounter(Context context, int pos) {
        Log.d(TAG, "Timelapse: "+pos);
        SharedPreferences pref = context.getSharedPreferences(PREFS_USER_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("Timelapse", pos);
        editor.commit();
    }

    /*private void Slide(){
        // This is used to hide the status bar and make
        // the splash screen as a full screen activity.
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // HERE WE ARE TAKING THE REFERENCE OF OUR IMAGE
        // SO THAT WE CAN PERFORM ANIMATION USING THAT IMAGE
        TextView backgroundImage= (TextView) findViewById(R.id.textView3);
        Animation slideAnimation = AnimationUtils.loadAnimation(this, R.anim.side_slide);
        backgroundImage.startAnimation(slideAnimation);

        // we used the postDelayed(Runnable, time) method
        // to send a message with a delayed time.
        Handler han = new Handler().postDelayed({
                val intent = Intent(this, MainActivity::class.java)
        startActivity(intent);
        finish();
        }, 3000); // 3000 is the delayed time in milliseconds.
    }*/

}
