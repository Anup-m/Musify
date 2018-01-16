package musicplayer.developer.it.musify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

import musicplayer.developer.it.musify.Adapters.PlayingQueueListAdapter;

import static musicplayer.developer.it.musify.SongsListActivity.playedLength;

/**
 * Created by anupam on 05-01-2018.
 */

public class PlayingQueueActivity extends AppCompatActivity implements View.OnClickListener{

    private ArrayList<SongInfo>  _songList;
    SongInfo songInfo;
    public static boolean checkIfPlayingQueueUiNeedsToUpdate = false;

    static ArrayList<Integer> random_list = new ArrayList<>();
    private Handler myHandler = new Handler();
    TextView songTitle;
    Animation play_in, pause_in, rotate_clockwise, rotate_anticlockwise;
    ImageButton btn_up,btn_playPause;
    SeekBar seekBar;
    Context context;
    private View mediaController;
    View view;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing_queue_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initializeFields();

        final ListView songListView = findViewById(R.id.song_list_view);
        _songList = SongsListActivity._songs;
        PlayingQueueListAdapter playingQueueListAdapter = new PlayingQueueListAdapter(this, _songList);
        songListView.setAdapter(playingQueueListAdapter);
        if (songListView.getFirstVisiblePosition() > SongsListActivity.currSongPosition ||
                songListView.getLastVisiblePosition() < SongsListActivity.currSongPosition)

            songListView.setSelection(SongsListActivity.currSongPosition - 1);

        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                songInfo = _songList.get(position);
                SongsListActivity.playSong(songInfo,position,getApplicationContext());
                alertUIs();
                //SongsListActivity.currSongPosition = position;

            }
        });

        btn_playPause.setOnClickListener(this);
        btn_up.setVisibility(View.GONE);


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

        mediaController.setBackgroundColor(getResources().getColor(R.color.colorDeepGrey));
        songTitle.setTextColor(getResources().getColor(R.color.colorWhite));
        btn_playPause.setColorFilter(getResources().getColor(R.color.colorWhite));

        updateMediaController(SongsListActivity.mediaPlayer);

    }

    private void initializeFields() {

        context = getApplicationContext();

        btn_up = findViewById(R.id.mCUp);
        btn_playPause = findViewById(R.id.mCPlayPause);
        seekBar = findViewById(R.id.mCSeekBar);
        songTitle = findViewById(R.id.mCSongName);
        songTitle.setSelected(true);

        mediaController = findViewById(R.id.mediaController);

        //Animations
        play_in = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.play_in);
        pause_in = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.pause_in);
        rotate_clockwise = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_clockwise);
        rotate_anticlockwise = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_anticlockwise);

    }

    private void updateMediaController(MediaPlayer mp) {

        SongInfo songInfo;
        if(SongsListActivity.currSongPosition == -1){
            songInfo = _songList.get(SongsListActivity.currSongPosition + 1);
            songTitle.setText(songInfo.getSongName());
            random_list.add(0);
        }
        else {

            if (SongsListActivity.mediaPlayer.isPlaying() || PlayerActivity.isPlaying){
                btn_playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mc_pause));
                btn_playPause.startAnimation(rotate_anticlockwise);
            }
            else {
                btn_playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mc_play));
                btn_playPause.startAnimation(rotate_anticlockwise);
            }

            songInfo = _songList.get(SongsListActivity.currSongPosition);
            songTitle.setText(songInfo.getSongName());

            try {
                Thread.sleep(100);
                setSeekBar();
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //Utility.addLastPlayedSongDetails(context,songInfo,);
        checkIfPlayingQueueUiNeedsToUpdate = false;
    }

    private  void setSeekBar() {
        seekBar.setMax(SongsListActivity.mediaPlayer.getDuration());

        if(playedLength != -1){
            seekBar.setProgress(playedLength);
        }
        else {
            seekBar.setProgress(SongsListActivity.mediaPlayer.getCurrentPosition());
        }

        myHandler.postDelayed(UpdateSongTime, 100);
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            if(playedLength != -1) {
                int currTime = SongsListActivity.mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currTime);
            }
            else {
                seekBar.setProgress(playedLength);
                playedLength = -1;
            }

            if(checkIfPlayingQueueUiNeedsToUpdate) {
                updateMediaController(SongsListActivity.mediaPlayer);
                checkIfPlayingQueueUiNeedsToUpdate = false;
            }
            myHandler.postDelayed(this, 100);
        }
    };


    private void alertUIs(){
        PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
        SongsListActivity.checkIfUiNeedsToUpdate = true;
        checkIfPlayingQueueUiNeedsToUpdate = true;
        PlayerActivity.isPlaying = true;
        Utility.recallNotificationBuilder(getApplicationContext());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.mCPlayPause:
                try {
                    if (SongsListActivity.mediaPlayer.isPlaying()) {
                        playedLength = SongsListActivity.mediaPlayer.getCurrentPosition();
                        SongsListActivity.mediaPlayer.pause();
                        btn_playPause.setImageResource(R.drawable.ic_mc_play);
                        btn_playPause.startAnimation(rotate_anticlockwise);
                        PlayerActivity.isPlaying = false;

                        SongsListActivity.checkIfUiNeedsToUpdate = true;
                        PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                    }
                    else{
                        if(SongsListActivity.currSongPosition == -1){
                            SongInfo songInfo = _songList.get(SongsListActivity.currSongPosition + 1);
                            SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition + 1,context);
                            updateMediaController(SongsListActivity.mediaPlayer);
                            btn_playPause.setImageResource(R.drawable.ic_mc_pause);
                            btn_playPause.startAnimation(rotate_clockwise);
                            PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                            SongsListActivity.checkIfUiNeedsToUpdate = true;
                            PlayerActivity.isPlaying = true;
                        }
                        else if (!SongsListActivity.isPrep) {
                            btn_playPause.setImageResource(R.drawable.ic_mc_pause);
                            btn_playPause.startAnimation(rotate_clockwise);

                            SongInfo obj = _songList.get(SongsListActivity.currSongPosition);
                            SongsListActivity.mediaPlayer.setDataSource(context, obj.getSongUrl());
                            SongsListActivity.mediaPlayer.prepareAsync();

                            SongsListActivity.mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    SongsListActivity.mediaPlayer.seekTo(playedLength);
                                    mp.start();
                                }
                            });

                            SongsListActivity.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    mp.reset();
                                    SongsListActivity.playNext();
                                    SongInfo songInfo = _songList.get(SongsListActivity.currSongPosition);
                                    SongsListActivity.playSong(songInfo,SongsListActivity.currSongPosition,context);
                                    PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                                    SongsListActivity.checkIfUiNeedsToUpdate = true;
                                }
                            });

                            PlayerActivity.isPlaying = true;
                            updateMediaController(SongsListActivity.mediaPlayer);
                            SongsListActivity.isPrep = true;
                            SongsListActivity.checkIfUiNeedsToUpdate = true;
                        }
                        else {
                            btn_playPause.setImageResource(R.drawable.ic_mc_pause);
                            btn_playPause.startAnimation(rotate_clockwise);
                            SongsListActivity.mediaPlayer.seekTo(playedLength);
                            SongsListActivity.mediaPlayer.start();
                            PlayerActivity.isPlaying = true;
                            SongsListActivity.checkIfUiNeedsToUpdate = true;
                        }
                    }
                }
                catch (Exception e){
                    Log.d("MC_PlayPause_Btn_Error",e.getMessage());
                }

                Utility.recallNotificationBuilder(context);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        SongsListActivity.checkIfUiNeedsToUpdate = true;
        PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
        super.onBackPressed();
    }
}
