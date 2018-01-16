package musicplayer.developer.it.musify.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

import musicplayer.developer.it.musify.ImageLoader;
import musicplayer.developer.it.musify.PlayerActivity;
import musicplayer.developer.it.musify.R;
import musicplayer.developer.it.musify.SongInfo;
import musicplayer.developer.it.musify.SongsListActivity;
import musicplayer.developer.it.musify.Utility;

/**
 * Created by anupam on 12-01-2018.
 */

public class PlayingQueueListAdapter extends BaseAdapter {

    public ViewHolder viewHolder;
    private static SongInfo songInfo;
    private static int currentSongPosition;
    private ArrayList<SongInfo> _songs;
    private ImageLoader imageLoader;
    private Context context;

    public PlayingQueueListAdapter(Context ctx, ArrayList<SongInfo> _songs) {
        this.context = ctx;
        this.imageLoader = new ImageLoader(ctx.getApplicationContext());
        this._songs = _songs;
    }

    public static int getCurrentSongPosition() {
        return currentSongPosition;
    }

    @Override
    public int getCount() {
        return _songs.size();
    }

    @Override
    public SongInfo getItem(int position) {
        return _songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        currentSongPosition = SongsListActivity.currSongPosition;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.palying_queue_song_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        SongInfo song = getItem(position);
//        if(position == currentSongPosition) {
//            viewHolder.itemName.setText(song.getSongName());
//            viewHolder.itemName.setTextColor(Color.CYAN);
//
//            viewHolder.itemDescription.setText(song.getArtistName());
//            viewHolder.itemDescription.setTextColor(Color.CYAN);
//        }
//        else {
            viewHolder.itemName.setText(song.getSongName());
            viewHolder.itemDescription.setText(song.getArtistName());
        //}



        String data = "content://media/external/audio/media/" + song.getSongId() + "";
        imageLoader.DisplayImage(data, viewHolder.imageView);

        return convertView;
    }

    public class ViewHolder {
        ImageView imageView,overflow;
        TextView itemName;
        TextView itemDescription;

        ViewHolder(View view) {
            imageView = view.findViewById(R.id.albumArt);
            overflow = view.findViewById(R.id.overflow);
            itemName = view.findViewById(R.id.songName);
            itemDescription = view.findViewById(R.id.artistName);
        }
    }
}
