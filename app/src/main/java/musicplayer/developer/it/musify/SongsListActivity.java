package musicplayer.developer.it.musify;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

public class SongsListActivity extends AppCompatActivity implements View.OnLongClickListener, View.OnClickListener,MusicFocusable,SearchView.OnQueryTextListener{

    static ArrayList<SongInfo> _songs = new ArrayList<>();
    static ArrayList<Integer> random_list = new ArrayList<>();
    static ArrayList<String> favourite_list = new ArrayList<>();
    private static boolean isPrep = false;
    RecyclerView recyclerView;
    static SongAdapter songAdapter;
    static MediaPlayer mediaPlayer;
    static int currSongPosition = -1, playedLength = -1,repeatCount = 0;
    static boolean checkIfUiNeedsToUpdate = false, isShuffleOn = false;
    String lastPlayedSongName, lastPlayedSongDetails;

    ImageButton btn_up,btn_playPause;
    SeekBar seekBar;
    TextView songTitle;

    View mediaController;

    private Handler myHandler = new Handler();
    static Context context;

    Animation play_in, pause_in, rotate_clockwise, rotate_anticlockwise;

    public static final float DUCK_VOLUME = 0.1f;
    static AudioFocusHelper mAudioFocusHelper;

    static enum AudioFocus{
        NoFocusNoDuck,
        NoFocusCanDuck,
        Focused
    }
    static AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    static AudioManager mAudioManager;

    Toolbar toolbar;

    static int sleep_radio_selected = 6,whichRadio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs_list);

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setInverseBackgroundForced(false);
        progressDialog.show();

        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),linearLayoutManager.getOrientation());

        songAdapter = new SongAdapter(_songs,this);

        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(songAdapter);

        initializeFields();
        getFavouriteList();


        songAdapter.setOnItemClickListener(new SongAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, SongInfo obj, int position) {
               // mediaPlayer = new MediaPlayer();
                mediaPlayer.reset();
                playSong(obj,position,context);
                Utility.recallNotificationBuilder(SongsListActivity.this);
                if(checkIfUiNeedsToUpdate)
                    updateMediaController(mediaPlayer);
            }
        });

        if(checkIfUiNeedsToUpdate)
            updateMediaController(mediaPlayer);

        btn_playPause.setOnClickListener(this);
        btn_up.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userTouch;
            @Override
            public void onProgressChanged(SeekBar seekBar, int length, boolean b) {
                if(mediaPlayer.isPlaying() && userTouch) {
                    mediaPlayer.seekTo(length);
                    mediaPlayer.start();
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

        mediaController.setOnTouchListener(new OnSwipeTouchListener(this){
            public void onSwipeTop() {
                PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                Intent playerIntent = new Intent(getApplicationContext(),PlayerActivity.class);
                startActivity(playerIntent);
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            }
        });

        checkPermission();
        getLastPlayedSongDetails();
        try {
            updateMediaController(mediaPlayer);
        }catch (Exception e){Log.i("MediaControllerError: ",e.getMessage());}

        progressDialog.dismiss();
    }

    private void getFavouriteList(){
        Favourites favourites = new Favourites(context);
        SharedPreferences preferences = getSharedPreferences(favourites.fileName,Context.MODE_PRIVATE);
        favourite_list = favourites.load();
    }

    private void getLastPlayedSongDetails(){
        LastPlayedSong lastPlayedSong = new LastPlayedSong(context);
        SharedPreferences preferences = getSharedPreferences(lastPlayedSong.fileName,Context.MODE_PRIVATE);
        try {

            lastPlayedSongDetails = lastPlayedSong.load();
            String[] details = lastPlayedSongDetails.split(",");
            lastPlayedSongName = details[0];
            playedLength = Integer.valueOf(details[1]);
            whichRadio = Integer.valueOf(details[2]);


            for (SongInfo item : _songs) {
                if (lastPlayedSongName.equals(item.getSongName())) {
                    currSongPosition = _songs.indexOf(item);
                }
            }
        }catch (Exception e){
            Log.d("LastPlayedSong","N/A");
        }
    }

    private void initializeFields() {

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        context = getApplicationContext();

        if(Build.VERSION.SDK_INT >= 8) {
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), (MusicFocusable) this);
        }
        else
            mAudioFocus = AudioFocus.Focused;

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSharedPreferencesForShuffleAndRepeat(context);
    }

    private static void getSharedPreferencesForShuffleAndRepeat(Context context) {
        //sharedPreference data for shuffle and repeat
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isShuffleOn = sharedPreferences.getBoolean(context.getString(R.string.shuffle_music), false);
        repeatCount = sharedPreferences.getInt(context.getString(R.string.repeat_music),0);
    }

    static void giveUpAudioFocus(){
        if(mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                &&mAudioFocusHelper.abandonFocus()){
            mAudioFocus = AudioFocus.NoFocusNoDuck;
        }
    }

    static void tryToGetAudioFocus(){
        if(mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                &&mAudioFocusHelper.requestFocus()){
            mAudioFocus = AudioFocus.Focused;
        }
    }

    static void configureAndStartMediaPlayer(){
        if(mAudioFocus == AudioFocus.NoFocusNoDuck){
            if(mediaPlayer.isPlaying())
                mediaPlayer.pause();
        }
        else if(mAudioFocus == AudioFocus.NoFocusCanDuck)
            mediaPlayer.setVolume(DUCK_VOLUME,DUCK_VOLUME);
        else
            mediaPlayer.setVolume(1.0f,1.0f);

//        if(!mediaPlayer.isPlaying())
//            mediaPlayer.start();

    }

    public void onGainedAudioFocus(){
        mAudioFocus = AudioFocus.Focused;
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            configureAndStartMediaPlayer();
        }
    }

    public  void onLostAudioFocus(boolean canDuck){
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            configureAndStartMediaPlayer();
        }
    }


    public static void playSong(SongInfo obj, int position, final Context context) {

        tryToGetAudioFocus();
        currSongPosition = position;
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context,obj.getSongUrl());
            mediaPlayer.prepareAsync();
            isPrep = true;

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer = mp;
                    configureAndStartMediaPlayer();
                    Utility.recallNotificationBuilder(context);
                    mp.start();
                }
            });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                if (mediaPlayer.getCurrentPosition() > 0) {
                    mp.reset();
                    playNext();
                    SongInfo songInfo = _songs.get(currSongPosition);
                    playSong(songInfo, currSongPosition, context);
                    PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                }
                }
            });

            checkIfUiNeedsToUpdate = true;
            PlayerActivity.isPlaying = true;
            Utility.recallNotificationBuilder(context);
            //NotificationGenerator.customNotification(context);

        }
        catch (Exception e){
            Log.e("Playing Error:",e.getMessage());
            Toast.makeText(context,"Couldn't play this song",Toast.LENGTH_SHORT).show();
            playNext();
            SongInfo songInfo = _songs.get(currSongPosition);
            playSong(songInfo,currSongPosition,context);
        }
    }

    private void updateMediaController(MediaPlayer mp) {

        SongInfo songInfo;
        if(currSongPosition == -1){
            songInfo = _songs.get(currSongPosition + 1);
            songTitle.setText(songInfo.getSongName());
            random_list.add(0);
        }
        else {

            if (mediaPlayer.isPlaying() || PlayerActivity.isPlaying){
                btn_playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mc_pause));
                btn_playPause.startAnimation(rotate_anticlockwise);
            }
            else {
                btn_playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mc_play));
                btn_playPause.startAnimation(rotate_anticlockwise);
            }

            songInfo = _songs.get(currSongPosition);
            songTitle.setText(songInfo.getSongName());

            try {
                Thread.sleep(100);
                setSeekBar();
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //Utility.addLastPlayedSongDetails(context,songInfo,);
        checkIfUiNeedsToUpdate = false;
    }

    private  void setSeekBar() {
        seekBar.setMax(mediaPlayer.getDuration());

        if(playedLength != -1){
            seekBar.setProgress(playedLength);
        }
        else {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
        }

        myHandler.postDelayed(UpdateSongTime, 100);
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            if(playedLength != -1) {
                int currTime = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currTime);
            }
            else {
                seekBar.setProgress(playedLength);
                playedLength = -1;
            }

            if(checkIfUiNeedsToUpdate) {
                updateMediaController(mediaPlayer);
                checkIfUiNeedsToUpdate = false;
            }
            myHandler.postDelayed(this, 100);
        }
    };

    private static void generateRandomSongPosition() {

        Random randomGenerate = new Random();
        currSongPosition = randomGenerate.nextInt( _songs.size() );
        if(random_list.contains(currSongPosition))
            generateRandomSongPosition();
        else
            random_list.add(currSongPosition);
    }

    public static void playNext() {
        getSharedPreferencesForShuffleAndRepeat(context);
        if(isShuffleOn){
            generateRandomSongPosition();
        }
        else if(repeatCount == 1){

        }
        else {
            currSongPosition++;
            if (currSongPosition >= _songs.size())
                currSongPosition = 0;
        }
        //SongInfo songInfo = _songs.get(currSongPosition);
        //playSong(songInfo,currSongPosition);
        //Utility.recallNotificationBuilder(context);
    }

    public static void playPrevious() {

        if(isShuffleOn){
            generateRandomSongPosition();
        }
        else {
            currSongPosition--;
            if (currSongPosition < 0)
                currSongPosition = _songs.size() - 1;
        }
        //SongInfo songInfo = _songs.get(currSongPosition);
        //playSong(songInfo,currSongPosition);
        //Utility.recallNotificationBuilder(context);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.mCUp:
                PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                Intent playerIntent = new Intent(getApplicationContext(),PlayerActivity.class);
                startActivity(playerIntent);
                break;
            case R.id.mCPlayPause:
                try {
                    if (mediaPlayer.isPlaying()) {
                        playedLength = mediaPlayer.getCurrentPosition();
                        mediaPlayer.pause();
                        btn_playPause.setImageResource(R.drawable.ic_mc_play);
                        btn_playPause.startAnimation(rotate_anticlockwise);
                        PlayerActivity.isPlaying = false;
                    }
                    else{
                        if(currSongPosition == -1){
                            SongInfo songInfo = _songs.get(currSongPosition + 1);
                            playSong(songInfo,currSongPosition + 1,context);
                            updateMediaController(mediaPlayer);
                            btn_playPause.setImageResource(R.drawable.ic_mc_pause);
                            btn_playPause.startAnimation(rotate_clockwise);
                            PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                            PlayerActivity.isPlaying = true;
                        }
                        else if (!isPrep) {
                            btn_playPause.setImageResource(R.drawable.ic_mc_pause);
                            btn_playPause.startAnimation(rotate_clockwise);

                            SongInfo obj = _songs.get(currSongPosition);
                            mediaPlayer.setDataSource(context, obj.getSongUrl());
                            mediaPlayer.prepareAsync();

                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mediaPlayer.seekTo(playedLength);
                                    mp.start();
                                }
                            });

                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    mp.reset();
                                    playNext();
                                    SongInfo songInfo = _songs.get(currSongPosition);
                                    playSong(songInfo,currSongPosition,context);
                                    PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
                                }
                            });

                            PlayerActivity.isPlaying = true;
                            updateMediaController(mediaPlayer);
                            isPrep = true;
                        }
                        else {
                            btn_playPause.setImageResource(R.drawable.ic_mc_pause);
                            btn_playPause.startAnimation(rotate_clockwise);
                            mediaPlayer.seekTo(playedLength);
                            mediaPlayer.start();
                            PlayerActivity.isPlaying = true;
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



    private void checkPermission(){
        if(Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
            } else{
                loadSongs();
            }

//            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS)
//                    != PackageManager.PERMISSION_GRANTED){
//                requestPermissions(new String[]{Manifest.permission.WRITE_SETTINGS}, 223);
//            }
        }
        else{
            loadSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 123:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    loadSongs();
                    if(playedLength == -1) {
                        recyclerView.setAdapter(songAdapter);
                    }
                    getLastPlayedSongDetails();
                    try {
                        updateMediaController(mediaPlayer);
                    }catch (Exception e){Log.i("MediaControllerError: ",e.getMessage());}

                    songAdapter.setOnItemClickListener(new SongAdapter.OnItemClickListener() {
                        @Override
                        public void OnItemClick(View v, SongInfo obj, int position) {
                            // mediaPlayer = new MediaPlayer();
                            mediaPlayer.reset();
                            playSong(obj,position,context);
                            Utility.recallNotificationBuilder(SongsListActivity.this);
                            if(checkIfUiNeedsToUpdate)
                                updateMediaController(mediaPlayer);
                        }
                    });
                }
                else{
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
                    checkPermission();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void loadSongs() {
        long id,dateAdded,duration;
        String name,artist,album,title,folder;
        Uri songUri;
        SongInfo songInfo;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);

        if(cursor!= null){
            if(cursor.moveToFirst()){
                do {
                    id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    songUri = Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                    album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                    folder = Utility.getFolderNameOutOfUrl(songUri.toString());

                    boolean filtered = Utility.checkIfFilteredByNameOrExtension(this,name,folder,duration);
                    if(!filtered) {
                        songInfo = new SongInfo(id, name, artist, album, songUri);
                        _songs.add(songInfo);
                    }

                }while (cursor.moveToNext());
            }
            cursor.close();
        }
        displaySongs();
    }

    private void displaySongs(){
        Collections.sort(_songs, new Comparator<SongInfo>(){
            public int compare(SongInfo a, SongInfo b){
                return a.getSongName().compareTo(b.getSongName());
            }
        });
        songAdapter = new SongAdapter(_songs,this);
        //recyclerView.setAdapter(songAdapter);
    }

    private String getDate(long mills){
        Date date = new Date(mills);
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        DateFormat formatter2 = DateFormat.getDateInstance();

        return formatter.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
                if(searchView != null)
                    searchView.setOnQueryTextListener(this);
                break;
            case R.id.action_shuffle_all:
                break;
            case R.id.action_new_playlist:
                break;
            case R.id.action_sleep_timer:
                //setUpSleepTimer();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        newText = newText.toLowerCase();
        ArrayList<SongInfo> searchList = new ArrayList<>();
        for(SongInfo song : _songs){
            String name = song.getSongName().toLowerCase();
            if(name.contains(newText))
                searchList.add(song);
        }
        songAdapter = new SongAdapter(searchList,this);
        recyclerView.setAdapter(songAdapter);
        //songAdapter.setFilter(searchList);

        songAdapter.setOnItemClickListener(new SongAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, SongInfo obj, int position) {
                mediaPlayer.reset();
                position = getRealSongPosition(obj);
                playSong(obj,position,context);
            }
        });
        return true;
    }

    public int getRealSongPosition(SongInfo obj){
        int position = 0;
        for(SongInfo song : _songs){
            if(obj.equals(song)){
                position = _songs.indexOf(obj);
            }
        }
        return position;
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(SettingsActivity.hasDataChanged) {
            _songs = new ArrayList<>();
            loadSongs();
            songAdapter.setOnItemClickListener(new SongAdapter.OnItemClickListener() {
                @Override
                public void OnItemClick(View v, SongInfo obj, int position) {
                    mediaPlayer.reset();
                    position = getRealSongPosition(obj);
                    playSong(obj,position,context);
                }
            });
            SettingsActivity.hasDataChanged = false;
        }
    }

    @Override
    protected void onStop() {
        saveLastPlayedSongDetails();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        saveLastPlayedSongDetails();
        if(Utility.isTimerOn) {
            Utility.timer.cancel();
            //Utility.addTimerDetailsPreferences(SongsListActivity.this,6,false);
        }
        //NotificationGenerator.exitNotification(context);
        super.onDestroy();
    }

//    @Override
//    public void onBackPressed() {
//        saveLastPlayedSongDetails();
////        this.moveTaskToBack(true);
////        android.os.Process.killProcess(android.os.Process.myPid());
////        System.exit(1);
//        super.onBackPressed();
//    }

    private void saveLastPlayedSongDetails() {
        try {
            SongInfo songInfo = _songs.get(currSongPosition);
            int position = mediaPlayer.getCurrentPosition();
            Utility.addLastPlayedSongDetails(context,songInfo,position,sleep_radio_selected);
        }catch (Exception e){
            Log.i("LastSongInfoError: ",e.getMessage());
        }
    }
}






/*



    private void loadSongs() {
        long id;
        String name,artist;
        Uri songUri,albumArtUri;
        Bitmap albumArt;
        SongInfo songInfo;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);

        if(cursor!= null){
            if(cursor.moveToFirst()){
                do {
                    id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    songUri = Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));

//                    albumArtUri = Uri.parse("content://media/external/audio/media/"+id+"");
//                    albumArt = Utility.getSongAlbumArt(context,albumArtUri);
//
//                    if(albumArt != null)
//                         songInfo = new SongInfo(id,name, artist,albumArt,songUri);
//                    else
                         songInfo = new SongInfo(id,name, artist,null,songUri);

                    _songs.add(songInfo);
                }while (cursor.moveToNext());
            }
            cursor.close();
            songAdapter = new SongAdapter(_songs,this);
        }
        Collections.sort(_songs, new Comparator<SongInfo>(){
            public int compare(SongInfo a, SongInfo b){
                return a.getSongName().compareTo(b.getSongName());
            }
        });
    }










    //String musicFilePath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
    //artBytes = cursor.getBlob(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
    //albumArt = getSongAlbumArt(artBytes,musicFilePath,songUri);

    private Bitmap getSongAlbumArt(byte[] artBytes,String path, Uri uri) {
        Bitmap bm = null;
        try{
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            byte[] artBytesNew = mmr.getEmbeddedPicture();
            if (artBytesNew != null) {
                InputStream is = new ByteArrayInputStream(artBytesNew);
                bm = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            }
        }
        catch (Exception problem) {
            Log.d("Bitmap Loading Problem",problem.getMessage());
        }
        return bm;
    }


    @Deprecated
    private void updateMediaController2(MediaPlayer mp) {

        if(!(mp.isPlaying()) && currSongPosition == -1){
            playedLength == -1

            SongInfo songInfo = _songs.get(currSongPosition + 1);
            songTitle.setText(songInfo.getSongName());
        }

                    else {
                    SongInfo songInfo = _songs.get(currSongPosition);
                    songTitle.setText(songInfo.getSongName());
                    btn_playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mc_pause));

                    try {
                    Thread.sleep(100);

                    if (currSongPosition != -1 || PlayerActivity.isPlaying) {
                    btn_playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mc_pause));
                    } else {
                    btn_playPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_mc_play));
                    }

                    setSeekBar();
                    }catch (InterruptedException e) {
                    e.printStackTrace();
                    }
                    }

                    }


}


switch (sleep_radio_selected){
                        case 1:
                            millis = 10000;
                            Utility.setTimer(SongsListActivity.this,millis);
                            break;
                        case 2:
                            millis = 20000;
                            Utility.setTimer(SongsListActivity.this,millis);
                            break;
                        case 3:
                            millis = 30000;
                            Utility.setTimer(SongsListActivity.this,millis);
                            break;
                        case 4:
                            millis = 60000;
                            Utility.setTimer(SongsListActivity.this,millis);
                            break;
                        case 5:
                            millis = Long.getLong(sleepTime)*1000;
                            Utility.setTimer(SongsListActivity.this,millis);
                            break;
                    }

String sleepTime = null;
    TextView tx_timer;
    private void setUpSleepTimer() {

        final RadioButton r10,r20,r30,r60,r_cus,r_off;
        final EditText et_cus_time;
        Button btn_ok,btn_cancel;


        LastPlayedSong lastPlayedSong = new LastPlayedSong(context);
        SharedPreferences preferences = getSharedPreferences(lastPlayedSong.fileName,Context.MODE_PRIVATE);
        String detail = lastPlayedSong.load();
        String[] details = detail.split(",");
        whichRadio = Integer.valueOf(details[2]);
        Utility.isTimerOn = Boolean.valueOf(details[3]);


        //Dialog Creation
        final Dialog myDialog = new Dialog(this);
        myDialog.setContentView(R.layout.sleep_timer);
        myDialog.setCancelable(true);


        //Defining height and width of Dialog
        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.65);
        myDialog.getWindow().setLayout(width, height);

        r10 = myDialog.findViewById(R.id.on10);
        r20 = myDialog.findViewById(R.id.on20);
        r30 = myDialog.findViewById(R.id.on30);
        r60 = myDialog.findViewById(R.id.on60);
        r_cus = myDialog.findViewById(R.id.oncustom);
        r_off = myDialog.findViewById(R.id.on_sleepoff);
        et_cus_time = myDialog.findViewById(R.id.custom_minutes);
        tx_timer = myDialog.findViewById(R.id.timer);
        btn_cancel = myDialog.findViewById(R.id.cancel);
        btn_ok = myDialog.findViewById(R.id.ok);

        if(!Utility.isTimerOn){
            r_off.setChecked(true);
        }
        else {
            tx_timer.setVisibility(View.VISIBLE);
            switch (whichRadio){
                case 1:
                    r10.setChecked(true);
                    break;
                case 2:
                    r20.setChecked(true);
                    break;
                case 3:
                    r30.setChecked(true);
                    break;
                case 4:
                    r60.setChecked(true);
                    break;
                case 5:
                    r_cus.setChecked(true);
                    break;
            }
        }

        r10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sleep_radio_selected = 1;
                r20.setChecked(false);
                r30.setChecked(false);
                r60.setChecked(false);
                r_cus.setChecked(false);
                r_off.setChecked(false);
                sleepTime = "10";
            }
        });

        r20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sleep_radio_selected = 2;
                r10.setChecked(false);
                r30.setChecked(false);
                r60.setChecked(false);
                r_cus.setChecked(false);
                r_off.setChecked(false);
                sleepTime = "20";
            }
        });

        r30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sleep_radio_selected = 3;
                r20.setChecked(false);
                r10.setChecked(false);
                r60.setChecked(false);
                r_cus.setChecked(false);
                r_off.setChecked(false);
                sleepTime = "30";
            }
        });

        r60.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sleep_radio_selected = 4;
                r20.setChecked(false);
                r30.setChecked(false);
                r10.setChecked(false);
                r_cus.setChecked(false);
                r_off.setChecked(false);
                sleepTime = "60";
            }
        });

        r_cus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sleep_radio_selected = 5;
                r20.setChecked(false);
                r30.setChecked(false);
                r60.setChecked(false);
                r10.setChecked(false);
                r_off.setChecked(false);

                //sleepTime = et_cus_time.getText().toString();
            }
        });

        r_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sleep_radio_selected = 6;
                r20.setChecked(false);
                r30.setChecked(false);
                r60.setChecked(false);
                r_cus.setChecked(false);
                r10.setChecked(false);

                if(Utility.isTimerOn) {
                    Utility.isTimerOn = false;
                    sleepTime = null;
                    Utility.addTimerDetailsPreferences(context, sleep_radio_selected, Utility.isTimerOn);
                    Toast.makeText(SongsListActivity.this,"Timer Cancel",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.addTimerDetailsPreferences(context, sleep_radio_selected,Utility.isTimerOn);
                myDialog.dismiss();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sleepTime == null && !(sleep_radio_selected == 5)){
                    //if(Utility.isTimerOn)
                      //  Toast.makeText(SongsListActivity.this,"Sleep timer is not turned on",Toast.LENGTH_SHORT).show();
                }
                else {

                    try {
                        if (sleep_radio_selected == 5)
                            sleepTime = et_cus_time.getText().toString();

                        long time = Long.valueOf(sleepTime);
                        long millis = time * 60 * 1000;
                        if(!Utility.isTimerOn) {
                            tx_timer.setVisibility(View.VISIBLE);
                            Utility.setTimer(SongsListActivity.this, millis,tx_timer);
                            Toast.makeText(SongsListActivity.this, "Sleep timer set for " + sleepTime + " minutes", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(SongsListActivity.this, "Sleep timer running", Toast.LENGTH_SHORT).show();
                        }

                        Utility.addTimerDetailsPreferences(context, sleep_radio_selected,Utility.isTimerOn);

                    }catch (Exception e){
                        Log.e("TIMER ERROR: ",e.getMessage());
                    }
                }
                myDialog.dismiss();
            }
        });
        myDialog.show();
    }



                    */
