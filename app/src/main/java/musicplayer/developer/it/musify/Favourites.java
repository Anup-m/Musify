package musicplayer.developer.it.musify;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;


/**
 * Created by anupam on 21-12-2017.
 */

public class Favourites {

    public final String fileName = "favPlaylist";
    private SharedPreferences favPlaylist;
    private SharedPreferences.Editor myEditor;
    private Context context;

    Favourites(Context context){
        this.context = context;
        favPlaylist = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        myEditor = favPlaylist.edit();
    }

    public void save(ArrayList<SongInfo> mySongs){
        // Save the size so that you can retrieve the whole list later
        myEditor.putInt("listSize", mySongs.size());

        for(int i = 0; i < mySongs.size(); i++){
            // Save each song with its index in the list as a key
            myEditor.putString(String.valueOf(i), mySongs.get(i).getSongName());
        }

        myEditor.commit();
    }

    public void save2(ArrayList<String> mySongs){
        // Save the size so that you can retrieve the whole list later
        myEditor.putInt("listSize", mySongs.size());

        for(int i = 0; i < mySongs.size(); i++){
            // Save each song with its index in the list as a key
            myEditor.putString(String.valueOf(i), mySongs.get(i));
        }

        myEditor.commit();
    }

    public void remove(String songName){
        ArrayList<String> favSongs = new ArrayList<String>();
        ArrayList<String> newFavSongs = new ArrayList<String>();
        favSongs = load();

        for(String song : favSongs){
           if(!songName.equals(song)){
               newFavSongs.add(song);
           }
        }

        save2(newFavSongs);
    }

    public ArrayList<String> load() {
        // Create new array to be returned
        ArrayList<String> savedSongs = new ArrayList<String>();

        // Get the number of saved songs
        int numOfSavedSongs = favPlaylist.getInt("listSize", 0);

        // Get saved songs by their index
        for (int i = 0; i < numOfSavedSongs; i++) {
            savedSongs.add(favPlaylist.getString(String.valueOf(i),null));
        }

        return savedSongs;
    }
}
