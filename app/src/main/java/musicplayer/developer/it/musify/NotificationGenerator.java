package musicplayer.developer.it.musify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.system.Os;
import android.widget.RemoteViews;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by anupam on 26-12-2017.
 */

public class NotificationGenerator {
    public static final int NOTIFICATION_ID = 11;

    public static final String NOTIFY_NEXT = "next";
    public static final String NOTIFY_PREVIOUS = "previous";
    public static final String NOTIFY_PLAY = "play";
    public static final String NOTIFY_PAUSE = "pause";

    public static final String NOTIFY_DUCK_UP = "duckUp";
    public static final String NOTIFY_DUCK_DOWN = "duckDown";

    static Notification notification;
    static boolean isSimpleView = false;


    public static void customNotification(Context context) {

        SongInfo songInfo = SongsListActivity._songs.get(SongsListActivity.currSongPosition);
        String detail = formatSongDetails(songInfo);
        String details[] = detail.split(",");
        String sname = details[0];
        String artist_plus_album = details[1];

        Intent notifyIntent = new Intent(context, SongsListActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent previous = new Intent(NOTIFY_PREVIOUS);
        Intent next = new Intent(NOTIFY_NEXT);
        Intent play = new Intent(NOTIFY_PLAY);
        Intent pause = new Intent(NOTIFY_PAUSE);

        Intent duckUp = new Intent(NOTIFY_DUCK_UP);
        Intent duckDown = new Intent(NOTIFY_DUCK_DOWN);


        PendingIntent pIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent previousIntent = PendingIntent.getBroadcast(context, 0, previous, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent nextIntent = PendingIntent.getBroadcast(context, 0, next, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pauseIntent = PendingIntent.getBroadcast(context, 0, pause, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent playIntent = PendingIntent.getBroadcast(context, 0, play, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent duckUpIntent = PendingIntent.getBroadcast(context, 0, duckUp, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent duckDownIntent = PendingIntent.getBroadcast(context, 0, duckDown, PendingIntent.FLAG_UPDATE_CURRENT);


//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
//
//        builder.setContentIntent(pIntent);
//        builder.setSmallIcon(R.drawable.notification_icon_small);
//        builder.setOngoing(true);
//        builder.setAutoCancel(false);
//        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//        if(isSimpleView)
//            builder.setContent()
//
//        // Add media control buttons that invoke intents in your media service
//        builder.addAction(R.drawable.ic_p_previous, "Previous", previousIntent); // #0
//        builder.addAction(R.drawable.ic_p_play, "Play", playIntent); // #0
//        builder.addAction(R.drawable.ic_p_pause, "Pause", pauseIntent);  // #1
//        builder.addAction(R.drawable.ic_p_next, "Next", nextIntent);     // #2
//
//        builder.addAction(R.drawable.ic_mc_down_arrow, "DuckDown", duckDownIntent);     // #3
//        builder.addAction(R.drawable.ic_mc_up_arrow, "DuckUp", duckUpIntent);     // #4

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.activity_simple_notifiation);
        RemoteViews expandedView = new RemoteViews(context.getPackageName(),R.layout.activity_big_notification);

        remoteViews.setTextViewText(R.id.noti_song_name,sname);
        expandedView.setTextViewText(R.id.noti_song_name,sname);
        remoteViews.setTextViewText(R.id.noti_song_artist,artist_plus_album);
        expandedView.setTextViewText(R.id.noti_song_artist,artist_plus_album);

        Uri albumArtUri = Uri.parse("content://media/external/audio/media/" + songInfo.getSongId() + "");
        Bitmap albumArt = Utility.getSongAlbumArt(context, albumArtUri);


        if (albumArt != null) {
            remoteViews.setImageViewBitmap(R.id.noti_album_art, albumArt);
            expandedView.setImageViewBitmap(R.id.noti_album_art, albumArt);
        }

        else {
            remoteViews.setImageViewResource(R.id.noti_album_art, R.drawable.default_album_art);
            expandedView.setImageViewResource(R.id.noti_album_art, R.drawable.default_album_art);
        }

        if (PlayerActivity.isPlaying) {
            remoteViews.setImageViewResource(R.id.noti_play_pause, R.drawable.ic_p_pause);
            expandedView.setImageViewResource(R.id.noti_play_pause, R.drawable.ic_p_pause);
        }
        else {
            remoteViews.setImageViewResource(R.id.noti_play_pause, R.drawable.ic_p_play);
            expandedView.setImageViewResource(R.id.noti_play_pause, R.drawable.ic_p_play);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(pIntent);
        builder.setSmallIcon(R.drawable.notification_icon_small);
        builder.setOngoing(true);
        builder.setAutoCancel(false);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

//        if(isSimpleView)
//            builder.setContent(expandedView);
//        else
//            builder.setContent(remoteViews);

        // Add media control buttons that invoke intents in your media service
        builder.addAction(R.drawable.ic_p_previous, "Previous", previousIntent); // #0
        builder.addAction(R.drawable.ic_p_play, "Play", playIntent); // #0
        builder.addAction(R.drawable.ic_p_pause, "Pause", pauseIntent);  // #1
        builder.addAction(R.drawable.ic_p_next, "Next", nextIntent);     // #2

        builder.addAction(R.drawable.ic_mc_down_arrow, "DuckDown", duckDownIntent);     // #3
        builder.addAction(R.drawable.ic_mc_up_arrow, "DuckUp", duckUpIntent);     // #4


        //Notification
        //Notification notification = builder.build();
        notification = builder.build();
        notification.contentView = remoteViews;
        notification.bigContentView = expandedView;
        if(PlayerActivity.isPlaying)
            notification.flags = Notification.FLAG_NO_CLEAR;
        else
            notification.flags = Notification.FLAG_AUTO_CANCEL;


        setListeners(remoteViews,expandedView, context);

        NotificationManager notificationmanager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationmanager != null) {
            notificationmanager.notify(NOTIFICATION_ID, notification);
        }
    }

    public static void setListeners(RemoteViews remoteViews,RemoteViews expandedView, Context context) {

        Intent play = new Intent(NOTIFY_PLAY);
        Intent pause = new Intent(NOTIFY_PAUSE);
        Intent previous = new Intent(NOTIFY_PREVIOUS);
        Intent next = new Intent(NOTIFY_NEXT);
        Intent duckUp = new Intent(NOTIFY_DUCK_UP);
        Intent duckDown = new Intent(NOTIFY_DUCK_DOWN);


//        PendingIntent playPauseIntent = PendingIntent.getBroadcast(context, 0, playPause, PendingIntent.FLAG_UPDATE_CURRENT);
//        remoteViews.setOnClickPendingIntent(R.id.noti_play_pause, playPauseIntent);
//        expandedView.setOnClickPendingIntent(R.id.noti_play_pause, playPauseIntent);

        PendingIntent previousIntent = PendingIntent.getBroadcast(context, 0, previous, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.noti_previous, previousIntent);
        expandedView.setOnClickPendingIntent(R.id.noti_previous, previousIntent);

        PendingIntent nextIntent = PendingIntent.getBroadcast(context, 0, next, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.noti_next, nextIntent);
        expandedView.setOnClickPendingIntent(R.id.noti_next, nextIntent);


        PendingIntent playIntent = PendingIntent.getBroadcast(context, 0, play, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.noti_play_pause, playIntent);
        expandedView.setOnClickPendingIntent(R.id.noti_play_pause, playIntent);

        PendingIntent pauseIntent = PendingIntent.getBroadcast(context, 0, pause, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.noti_play_pause, pauseIntent);
        expandedView.setOnClickPendingIntent(R.id.noti_play_pause, pauseIntent);

        PendingIntent duckUpIntent = PendingIntent.getBroadcast(context, 0, duckUp, PendingIntent.FLAG_UPDATE_CURRENT);
        expandedView.setOnClickPendingIntent(R.id.changeViewUp, duckUpIntent);

        PendingIntent duckDownIntent = PendingIntent.getBroadcast(context, 0, duckDown, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.changeViewDown, duckDownIntent);
    }

    public static void exitNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NotificationGenerator.NOTIFICATION_ID);
        }
    }

    private static String formatSongDetails(SongInfo songInfo){
        String details = null;
        String sname = songInfo.getSongName();
        String artist_plus_album = songInfo.artistName +" - "+ songInfo.getSongAlbum();

        if(songInfo.getSongName().length() > 35){
            sname = songInfo.getSongName().substring(0,35) +"...";
        }
        if(artist_plus_album.length() > 18){
            artist_plus_album = artist_plus_album.substring(0,18) + "...";
        }
        details = sname+","+artist_plus_album;
        return details;
    }

}
