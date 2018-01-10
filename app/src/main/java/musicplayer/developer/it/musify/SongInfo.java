package musicplayer.developer.it.musify;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Created by anupam on 10-12-2017.
 */

public class SongInfo {
    String songName, artistName, songAlbum;
    //Bitmap albumArt;
    Uri songUri;
    long songId;

    public SongInfo(long songId, String songName, String artistName,String songAlbum,Uri songUri /*, Bitmap albumArt*/ ) {
        this.songId = songId;
        this.songName = songName;
        this.artistName = artistName;
        this.songAlbum = songAlbum;
        //this.albumArt = albumArt;
        this.songUri = songUri;
    }

    public String getSongAlbum() {
        return songAlbum;
    }

    public long getSongId() {
        return songId;
    }

    public String getSongName() {
        return songName;
    }

    public String getArtistName() {
        return artistName;
    }

    public Uri getSongUrl() { return songUri;}

    @Deprecated
    public Bitmap getAlbumArt() {
        //return albumArt;
        return null;
    }


}
