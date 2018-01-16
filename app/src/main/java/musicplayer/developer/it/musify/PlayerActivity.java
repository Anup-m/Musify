package musicplayer.developer.it.musify;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {


    Context context;
    public static boolean checkIfPlayerUiNeedsToUpdate = false;
    public static boolean isPlaying = false;
    //static int repeatCount = 0;

    LinearLayout playerLayout;
    //RelativeLayout playerLayout;
    ImageButton btnShuffle, btnPrev, btnPlayPause, btnNext, btnRepeat, btnDock, btnVol;
    ImageView btnFav, btnQueueMusic, btnMenu,btnBack;
    ImageView albumArt;

    SeekBar seekBar;
    TextView songName, artistName, currDur, totalDur;

    ImageLoader imageLoader; //class for lazy loading images into an imageView...

    private Handler myHandler = new Handler();

    //private Uri albumArtUri;
    //private Bitmap albumArtBitmap;
    //SongsListActivity songsListActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_player);

        initializeFields();
        updatePlayerUI();

        btnShuffle.setOnClickListener(this);
        btnPrev.setOnClickListener(this);
        btnPlayPause.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnRepeat.setOnClickListener(this);

        btnFav.setOnClickListener(this);
        btnQueueMusic.setOnClickListener(this);
        btnMenu.setOnClickListener(this);
        btnBack.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userTouch;
            @Override
            public void onProgressChanged(SeekBar seekBar, int length, boolean b) {
                if(SongsListActivity.mediaPlayer.isPlaying() && userTouch) {
                    SongsListActivity.mediaPlayer.seekTo(length);
                    SongsListActivity.mediaPlayer.start();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userTouch = false;
            }
        });

        albumArt.setOnTouchListener(new OnSwipeTouchListener(PlayerActivity.this) {
            public void onSwipeBottom() {
                onBackPressed();
                //TODO MainActivity MusicController need to update after this action
            }
            public void onSwipeRight(){
                playNext();
                updatePlayerUI();
            }
            public void onSwipeLeft(){
                playPrev();
                updatePlayerUI();
            }
        });
    }

    private void initializeFields(){
        context = getApplicationContext();
        imageLoader = new ImageLoader(context);
        playerLayout = findViewById(R.id.player_layout);

        btnRepeat = (ImageButton) findViewById(R.id.pRepeat);
        btnShuffle = (ImageButton) findViewById(R.id.pShuffle);
        btnPrev = (ImageButton) findViewById(R.id.pPrevious);
        btnPlayPause = (ImageButton) findViewById(R.id.pPause);
        btnNext = (ImageButton) findViewById(R.id.pNext);

        btnFav = findViewById(R.id.pFavourits);
        btnQueueMusic = findViewById(R.id.pPlayingQueue);
        btnMenu = findViewById(R.id.pOverflow);
        btnBack = findViewById(R.id.pBack);
        songName = (TextView) findViewById(R.id.pSongName);
        artistName = (TextView)findViewById(R.id.pArtistName);

        currDur = (TextView)findViewById(R.id.pCurrDur);
        totalDur = (TextView)findViewById(R.id.pTotalDur);

        albumArt = (ImageView) findViewById(R.id.pAlbumArt);
        seekBar = (SeekBar)findViewById(R.id.pSeekBar);
        changeStatusBarColor();
        getFavouriteList();
    }

    private void updatePlayerUI() {
        //For PlayPause Button
        SongInfo songInfo;
        if(SongsListActivity.currSongPosition == -1) {
            if(!(isPlaying)) {
                btnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_play));
                currDur.setText("00:00");
                totalDur.setText("05:00");

            }
            else {
                btnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_pause));
            }
            songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition + 1);
        }
        else {
            if(isPlaying)
                btnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_pause));
            else
                btnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_play));

            songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
        }

        //For Shuffle Button
        if(SongsListActivity.isShuffleOn){
            btnShuffle.setColorFilter(Color.BLACK);
        }

        //For Repeat Button
        if(SongsListActivity.repeatCount == 0){
            btnRepeat.setColorFilter(Color.GRAY);
            btnRepeat.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_repeat));
        }
        else if(SongsListActivity.repeatCount == 1){
            btnRepeat.setColorFilter(Color.BLACK);
            btnRepeat.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_repeate_one));
        }
        else {
            btnRepeat.setColorFilter(Color.BLACK);
            btnRepeat.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_repeat));
        }

        //For TextBoxes like songName & artistName..
        songName.setText(songInfo.getSongName());
        artistName.setText(songInfo.getArtistName());

        //For albumArt
        String uri = "content://media/external/audio/media/"+songInfo.getSongId()+"";
        imageLoader.DisplayImage(uri,albumArt);
        //boolean isAlbumArtPresent = imageLoader.displayImage(uri,albumArt);

        Bitmap bitmap = Utility.getSongAlbumArt(context,Uri.parse(uri));

        if(bitmap != null) {

            int vibrantColor = getVibrantColor(bitmap);

            btnBack.setColorFilter(Color.BLACK);
            btnFav.setColorFilter(Color.BLACK);
            btnQueueMusic.setColorFilter(Color.BLACK);
            btnMenu.setColorFilter(Color.BLACK);
        }

        //For FavouriteButton
        if(SongsListActivity.favourite_list.contains(songInfo.getSongName())){
            btnFav.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_favourite_recognised));
        } else
            btnFav.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_favourite));

        //For seekBar
        try {
            Thread.sleep(500);
            setSeekBar();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        checkIfPlayerUiNeedsToUpdate = false;
    }

    private void setSeekBar() {
        seekBar.setMax(SongsListActivity.mediaPlayer.getDuration());
        seekBar.setProgress(SongsListActivity.mediaPlayer.getCurrentPosition());

        if(SongsListActivity.currSongPosition != -1) {
            String endTime = findTotalDuration(false, SongsListActivity.mediaPlayer.getDuration());
            totalDur.setText(endTime);
            myHandler.postDelayed(UpdateSongTime, 100);
        }
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            int currTime = SongsListActivity.mediaPlayer.getCurrentPosition();
            seekBar.setProgress(currTime);

            if(checkIfPlayerUiNeedsToUpdate) {
                updatePlayerUI();
                checkIfPlayerUiNeedsToUpdate = false;
            }

            String progressBarTime = findTotalDuration(false,currTime);
            currDur.setText(progressBarTime);
            myHandler.postDelayed(this, 100);
        }
    };



    private int getVibrantColor(Bitmap bitmap){
        final int[] abc = new int[3];
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
               Palette.Swatch textSwatch = palette.getVibrantSwatch();

                if (textSwatch == null) {
                    return;
                }
                abc[0] = textSwatch.getRgb();
                abc[1] = textSwatch.getBodyTextColor();
                abc[2] = textSwatch.getTitleTextColor();
                //vibrantColor = textSwatch.getRgb();
                changeStatusBarColor2(abc[0]);
            }
        });
        return abc[0];
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.pShuffle:
                shuffle(SongsListActivity.isShuffleOn);
                break;
            case R.id.pPrevious:
                playPrev();
                break;
            case R.id.pPause:
                if(SongsListActivity.mediaPlayer.isPlaying())
                    pause();
                else
                    resume();
                break;
            case R.id.pNext:
                playNext();
                break;
            case R.id.pRepeat:
                if(SongsListActivity.repeatCount != 2)
                    SongsListActivity.repeatCount++;
                else
                    SongsListActivity.repeatCount = 0;
                repeat(SongsListActivity.repeatCount);
                break;
            case R.id.pFavourits:
                favourites();
                break;
            case R.id.pPlayingQueue:
                startActivity(new Intent(this,PlayingQueueActivity.class));
                break;
            case R.id.pOverflow:

                break;

            case R.id.pBack:
                onBackPressed();
                break;
        }
        try {
            Utility.recallNotificationBuilder(context);
        }catch (Exception ignored) { }

    }

    private void favourites() {
        getFavouriteList();
        SongInfo songObj = SongsListActivity._songs.get(SongsListActivity.currSongPosition);

        for (String name : SongsListActivity.favourite_list) {
            if (songObj.getSongName().equals(name)) {
                Utility.removeFromFavouriteList(context, songObj);
                Toast.makeText(context, "Removed from favourites", Toast.LENGTH_SHORT).show();
                btnFav.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_favourite));
                return;
            }
        }

        try {
            Utility.addToFavouriteList(context, songObj);
            btnFav.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_favourite_recognised));
            Toast.makeText(context, "Added to favourites", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d("FAVOURITE ERROR :", e.getMessage());
        }
    }


    private void shuffle(boolean isShuffleOn) {
        if(isShuffleOn){
            SongsListActivity.isShuffleOn = false;
            btnShuffle.setColorFilter(Color.GRAY);
            Toast.makeText(this,"Shuffle off",Toast.LENGTH_SHORT).show();
        }
        else {
            SongsListActivity.isShuffleOn = true;
            btnShuffle.setColorFilter(Color.BLACK);
            Toast.makeText(this,"Shuffle on",Toast.LENGTH_SHORT).show();

            if(SongsListActivity.repeatCount != 0) {
                SongsListActivity.repeatCount = 0;
                addRepeatPreference(SongsListActivity.repeatCount);
                btnRepeat.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_repeat));
                btnRepeat.setColorFilter(Color.GRAY);
            }
        }
        addToPreference(SongsListActivity.isShuffleOn);
    }

    private void repeat(int repeatCount) {
        switch (repeatCount){
            case 0:
                btnRepeat.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_repeat));
                btnRepeat.setColorFilter(Color.GRAY);
                Toast.makeText(this,"Repeat off",Toast.LENGTH_SHORT).show();
                break;
            case 1:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                SongsListActivity.isShuffleOn = sharedPreferences.getBoolean(getString(R.string.shuffle_music), false);

                if(SongsListActivity.isShuffleOn) {
                    SongsListActivity.isShuffleOn = false;
                    addToPreference(SongsListActivity.isShuffleOn);
                    btnShuffle.setColorFilter(Color.GRAY);
                }

                btnRepeat.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_repeate_one));
                btnRepeat.setColorFilter(Color.BLACK);

                Toast.makeText(this,"Repeat this song",Toast.LENGTH_SHORT).show();
                break;
            case 2:
                if(SongsListActivity.isShuffleOn){
                    SongsListActivity.isShuffleOn = false;
                    addToPreference(SongsListActivity.isShuffleOn);
                    btnShuffle.setColorFilter(Color.GRAY);
                }

                btnRepeat.setImageDrawable(getResources().getDrawable(R.drawable.ic_p_repeat));
                btnRepeat.setColorFilter(Color.BLACK);

                Toast.makeText(this,"Repeat all",Toast.LENGTH_SHORT).show();
                break;
        }
        addRepeatPreference(repeatCount);
    }

    private void addRepeatPreference(int repeatCount) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor shuffleMusicEditor = sharedPreferences.edit();
        shuffleMusicEditor.putInt(getString(R.string.repeat_music),repeatCount);
        shuffleMusicEditor.apply();
    }

    private void addToPreference(boolean isShuffleOn) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor shuffleMusicEditor = sharedPreferences.edit();
        shuffleMusicEditor.putBoolean(getString(R.string.shuffle_music),isShuffleOn);
        shuffleMusicEditor.apply();
    }

    private void playNext() {
        SongsListActivity.playNext();
        SongInfo songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
        SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition,context);
        SongsListActivity.checkIfUiNeedsToUpdate = true;
        updatePlayerUI();
    }

    private void playPrev() {
        SongsListActivity.playPrevious();
        SongInfo songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
        SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition,context);
        SongsListActivity.checkIfUiNeedsToUpdate = true;
        updatePlayerUI();
    }

    private void pause() {
        SongsListActivity.mediaPlayer.pause();
        btnPlayPause.setImageDrawable((getResources().getDrawable(R.drawable.ic_p_play)));
        SongsListActivity.playedLength = SongsListActivity.mediaPlayer.getCurrentPosition();
        //SongsListActivity.checkIfUiNeedsToUpdate = true;
        isPlaying = false;
    }

    private void resume() {

        isPlaying = true;
        if(SongsListActivity.playedLength == -1) {
            SongInfo songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition + 1);
            SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition + 1,context);
        }
        else {
            SongsListActivity.mediaPlayer.seekTo(SongsListActivity.playedLength);
            SongsListActivity.mediaPlayer.start();
        }
        //btnPlayPause.setImageDrawable((getResources().getDrawable(R.drawable.ic_p_pause)));
        updatePlayerUI();
    }


    public static String findTotalDuration(Boolean setEndTime,long time){
        long millis;
        if(setEndTime)
            millis = SongsListActivity.mediaPlayer.getCurrentPosition();
        else
            millis = time;//For converting ProgressTime to H:M:S

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        long seconds = totalSeconds - (minutes*60);
        String min = String.valueOf(minutes);
        String sec = String.valueOf(seconds);

        if(min.length() == 1)
            min = "0" + min;
        if(sec.length() == 1)
            sec = "0" + sec;

        return min + ":" + sec;
    }

    @Override
    public void onBackPressed() {
        SongsListActivity.checkIfUiNeedsToUpdate = true;
        PlayingQueueActivity.checkIfPlayingQueueUiNeedsToUpdate = true;
        super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                try {
                    Utility.shareSong(context);
                }catch (Exception e){
                    Log.d("SHARE_DETAILS ERROR :",e.getMessage());
                }
                break;

            case R.id.action_add_favourite:
                try {
                    SongInfo songObj = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
                    Utility.addToFavouriteList(context,songObj);
                    Toast.makeText(context, "Added to favourite", Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    Log.d("FAVOURITE ERROR :",e.getMessage());
                }
                break;

            case R.id.action_set_ringtone:
                try {
                    SongInfo songObj = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
                    long id = songObj.getSongId();
                    //Utility.setRingtone(context,id);
                    //Utility.setAsRingtone(context,songObj);
                    getPermission(id);
                   // Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    Log.d("SETTING RINGTONE ERROR:",e.getMessage());
                }
                break;

            case R.id.action_rename:
                Toast.makeText(this,"Feature under devlopment!!..",Toast.LENGTH_SHORT).show();
                break;

            case R.id.action_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    private void checkPermission(long id){
        if(Build.VERSION.SDK_INT >= 23) {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS)
                    != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_SETTINGS},123);
            }
        }
        else{
           Utility.setRingtone(context,id);
        }
    }

    private void getPermission(long id){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)){
                // Do stuff here
                Utility.setRingtone(context,id);
            }
            else {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                String s = "package:"+context.getPackageName();
                intent.setData(Uri.parse(s));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        SongInfo songObj = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
        long id = songObj.getSongId();

        switch (requestCode){
            case 123:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Utility.setRingtone(context,id);
                }
                else{
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
                    checkPermission(id);
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void getFavouriteList(){
        Favourites favourites = new Favourites(context);
        SharedPreferences preferences = getSharedPreferences(favourites.fileName,Context.MODE_PRIVATE);
        SongsListActivity.favourite_list = favourites.load();
        for(String name : SongsListActivity.favourite_list){
            Log.i("FavSong :",name);
        }
    }

    public void changeStatusBarColor(){
        // if (android.os.Build.VERSION.SDK_INT >= 21) {
        Window window = this.getWindow();
        //window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorGrey));
        // }
    }

    public void changeStatusBarColor2(int color){
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(color);
    }

    public int calculateAverageColor(android.graphics.Bitmap bitmap, int pixelSpacing) {
        int R = 0; int G = 0; int B = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int n = 0;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i += pixelSpacing) {
            int color = pixels[i];
            R += Color.red(color);
            G += Color.green(color);
            B += Color.blue(color);
            n++;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Color color = Color.valueOf(Color.rgb(R / n, G / n, B / n));
        }
        return Color.rgb(R / n, G / n, B / n);
    }

}


/*


//        albumArtUri = Uri.parse("content://media/external/audio/media/"+songInfo.getSongId()+"");
//        albumArtBitmap = Utility.getSongAlbumArt(context,albumArtUri);
//        if(albumArtBitmap != null)
//            albumArt.setImageBitmap(albumArtBitmap);
//        else
//            albumArt.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art));


case R.id.action_delete:
                try {
                    SongInfo obj = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
                    String path = obj.getSongUrl().toString();
                    boolean deleted = Utility.deleteSong(path);
                    if (deleted)
                        Toast.makeText(context, "Deleted from device", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(context, "Could not delete file", Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    Log.d("DELETE ERROR : ",e.getMessage());
                }

                break;


                //        sharedPreference data for shuffle
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        SongsListActivity.isShuffleOn = sharedPreferences.getBoolean(getString(R.string.shuffle_music), false);
//        SongsListActivity.repeatCount = sharedPreferences.getInt(getString(R.string.repeat_music),0);

 */
