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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import static java.security.AccessController.getContext;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout playerLayout;
    ImageButton btnDock, btnQueueMusic, btnFav, btnVol, btnShuffle, btnPrev, btnPlayPause, btnNext, btnRepeat;
    ImageView albumArt;
    SeekBar seekBar;
    TextView songName, artistName, currDur, totalDur;

    SongsListActivity songsListActivity;
    ImageLoader imageLoader;
    Context context;
    static boolean checkIfPlayerUiNeedsToUpdate = false;
    static boolean isPlaying = false;

    private Handler myHandler = new Handler();
    private Uri albumArtUri;
    private Bitmap albumArtBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initializeFields();
        setSongData();

        btnShuffle.setOnClickListener(this);
        btnPrev.setOnClickListener(this);
        btnPlayPause.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnRepeat.setOnClickListener(this);

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
                setSongData();
            }
            public void onSwipeLeft(){
                playPrev();
                setSongData();
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

        songName = (TextView) findViewById(R.id.pSongName);
        artistName = (TextView)findViewById(R.id.pArtistName);

        currDur = (TextView)findViewById(R.id.pCurrDur);
        totalDur = (TextView)findViewById(R.id.pTotalDur);

        albumArt = (ImageView) findViewById(R.id.pAlbumArt);
        seekBar = (SeekBar)findViewById(R.id.pSeekBar);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.pShuffle:
                if(SongsListActivity.isShuffleOn){
                    SongsListActivity.isShuffleOn = false;
                    btnShuffle.setColorFilter(Color.GRAY);
                    addToPreference(SongsListActivity.isShuffleOn);
                }
                else {
                    SongsListActivity.isShuffleOn = true;
                    btnShuffle.setColorFilter(Color.BLACK);
                }
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
                break;

        }
        Utility.recallNotificationBuilder(context);
    }

    private void addToPreference(boolean isShuffleOn) {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shuffle_music),Context.MODE_PRIVATE);
        SharedPreferences.Editor shuffleMusicEditor = sharedPreferences.edit();
        shuffleMusicEditor.putBoolean(getString(R.string.sleep_timer),isShuffleOn);
    }

    private void playNext() {
        SongsListActivity.playNext();
        SongInfo songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
        SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition,context);
        SongsListActivity.checkIfUiNeedsToUpdate = true;
        setSongData();
    }

    private void playPrev() {
        SongsListActivity.playPrevious();
        SongInfo songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
        SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition,context);
        SongsListActivity.checkIfUiNeedsToUpdate = true;
        setSongData();
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
        setSongData();
    }

    private void setSongData() {
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

        if(SongsListActivity.isShuffleOn){
            btnShuffle.setColorFilter(Color.BLACK);
        }

        songName.setText(songInfo.getSongName());
        artistName.setText(songInfo.getArtistName());

//        albumArtUri = Uri.parse("content://media/external/audio/media/"+songInfo.getSongId()+"");
//        albumArtBitmap = Utility.getSongAlbumArt(context,albumArtUri);
//        if(albumArtBitmap != null)
//            albumArt.setImageBitmap(albumArtBitmap);
//        else
//            albumArt.setImageDrawable(getResources().getDrawable(R.drawable.default_album_art));

        String uri = "content://media/external/audio/media/"+songInfo.getSongId()+"";
        imageLoader.DisplayImage(uri,albumArt);

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
                setSongData();
                checkIfPlayerUiNeedsToUpdate = false;
            }

            String progressBarTime = findTotalDuration(false,currTime);
            currDur.setText(progressBarTime);
            myHandler.postDelayed(this, 100);
        }
    };

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
}


/*


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
 */
