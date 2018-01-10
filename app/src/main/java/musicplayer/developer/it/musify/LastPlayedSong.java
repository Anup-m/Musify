package musicplayer.developer.it.musify;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

/**
 * Created by anupam on 22-12-2017.
 */

public class LastPlayedSong {
    public final String fileName = "lastPlayedSong";
    private SharedPreferences lastPlayedSong;
    private SharedPreferences.Editor myEditor;
    private Context context;

    LastPlayedSong(Context context){
        this.context = context;
        lastPlayedSong = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        myEditor = lastPlayedSong.edit();
    }

    public void save(SongInfo songLastPlayed, int playedUpTo,int timerRadio){
        myEditor.putInt("played_up_to",playedUpTo);
        myEditor.putInt("radio_checked",timerRadio);
        myEditor.putString("song_title", songLastPlayed.getSongName());
        myEditor.commit();
    }

    public void save(int timerRadio,boolean isTimerOn){
        myEditor.putInt("radio_checked",timerRadio);
        myEditor.putBoolean("timer_status",isTimerOn);
        myEditor.commit();
    }

    public String load() {
        String savedSong,details = null;
        int length = 0,timerRadio;
        boolean isTimerOn;
        savedSong = lastPlayedSong.getString("song_title",null);
        length = lastPlayedSong.getInt("played_up_to",0);
        timerRadio = lastPlayedSong.getInt("radio_checked",0);
        isTimerOn = lastPlayedSong.getBoolean("timer_status",false);


        if(length != 0 && savedSong != null)
            details = savedSong+","+length+","+timerRadio+","+isTimerOn;

        return details;
    }
}
