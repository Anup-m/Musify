package musicplayer.developer.it.musify;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class SplashScreenActivity extends AppCompatActivity {


    Animation play_in;
    ImageView imageView;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        imageView = findViewById(R.id.splashscreen_img);
        progressBar = findViewById(R.id.progressBar);
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        //Animations
        play_in = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.play_in);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                imageView.setVisibility(View.VISIBLE);
                //progressBar.setVisibility(View.VISIBLE);
                imageView.startAnimation(play_in);
            }
        }, 200);
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                startActivity(new Intent(SplashScreenActivity.this,SongsListActivity.class));
                finish();
            }
        }, 1500);
    }
}
