package musicplayer.developer.it.musify;

import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by anupam on 20-12-2017.
 */

public class Utility {

    public static CounterClass timer;
    public static SleepTimerClass sleep_timer;
    public static boolean isTimerOn = false;
    static ArrayList<SongInfo> favourite_songs = new ArrayList<>();

    public static void addToFavouriteList(Context context, SongInfo songInfo){
        favourite_songs.add(songInfo);
        Favourites favourites = new Favourites(context);
        favourites.save(favourite_songs);
    }

    public static void removeFromFavouriteList(Context context, SongInfo songInfo){
        Favourites favourites = new Favourites(context);
        favourites.remove(songInfo.getSongName());
    }

    public static void addLastPlayedSongDetails(Context context, SongInfo songInfo, int length,int radioChecked){
        LastPlayedSong lastPlayedSong = new LastPlayedSong(context);
        lastPlayedSong.save(songInfo,length,radioChecked);
    }

    public static void addTimerDetailsPreferences(Context context,int radioChecked,boolean isTimerOn){
        LastPlayedSong lastPlayedSong = new LastPlayedSong(context);
        lastPlayedSong.save(radioChecked,isTimerOn);
    }


    public static boolean checkIfFilteredByNameOrExtension(Context context,String name,String folderName,long time) {

        boolean status = false;
        ArrayList<String> filterByNameList = null;
        ArrayList<String> filterByExtensionList = null;
        Set<String> filteredFoldersList = null;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String filterByName = sharedPreferences.getString("filter_by_name", " ");
        String filterByExtension = sharedPreferences.getString("filter_by_Extension", " ");
        boolean filterShortTracks = sharedPreferences.getBoolean("ignore_short_tracks", true);

        filteredFoldersList = sharedPreferences.getStringSet("music_folders",null);
        String[] selected =  null;
        if (filteredFoldersList != null) {
            selected = filteredFoldersList.toArray(new String[] {});
            Arrays.sort(selected);
        }


        try {
            if(!filterByName.equals(" ") && !filterByName.equals("")) {
                filterByNameList = new ArrayList<>(Arrays.asList(filterByName.split(",")));
            }
            if(!filterByExtension.equals(" ") || !filterByExtension.equals("")) {
                filterByExtensionList = new ArrayList<>(Arrays.asList(filterByExtension.split(",")));
            }

            if(filterByNameList != null) {
                for (String filteredName : filterByNameList) {
                    if (name.toLowerCase().contains(filteredName.toLowerCase()) && !filteredName.equals(" ") && !filteredName.equals("")) {
                        status = true;
                        break;
                    }
                }
            }

            if(filterByExtensionList != null) {
                for (String filteredExtension : filterByExtensionList) {
                    if (name.toLowerCase().contains(filteredExtension.toLowerCase()) && !filteredExtension.equals(" ") && !filteredExtension.equals("")) {
                        status = true;
                        break;
                    }
                }
            }

            if(filterShortTracks){
                if(time < 60000){
                    status = true;
                }
            }

            if (filteredFoldersList != null && !filteredFoldersList.contains(folderName)) {
                ArrayList<String> foldersList = new ArrayList<>();
                for(String index : selected){
                    foldersList.add(SettingsActivity.folderContainingMusic.get(Integer.parseInt(index)));
                }

                if(!foldersList.contains(folderName))
                    status = true;
            }


        } catch(Exception e) { e.getMessage(); }

      return status;
    }

    private static final String TAG = "Utility";

    public static void setRingtone(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();
        // Set the flag in the database to mark this as a ringtone
        Uri ringUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            ContentValues values = new ContentValues(2);
            values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
            values.put(MediaStore.Audio.Media.IS_ALARM, "1");
            resolver.update(ringUri, values, null, null);
        } catch (UnsupportedOperationException ex) {
            // most likely the card just got unmounted
            Log.e(TAG, "couldn't set ringtone flag for id " + id);
            return;
        }

        String[] cols = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE
        };

        String where = MediaStore.Audio.Media._ID + "=" + id;
        Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                cols, where , null, null);
        try {
            if (cursor != null && cursor.getCount() == 1) {
                // Set the system setting to make this the current ringtone
                cursor.moveToFirst();
                Settings.System.putString(resolver, Settings.System.RINGTONE, ringUri.toString());
                //String message = context.getString(R.string.ringtone_set);
                //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static Cursor query(Context context, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }


    public static boolean deleteSong(String path) {
        boolean status = false;
        File file = new File(path);
        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
                status = true;
            } catch (IOException e) {
                Log.d("DeleteFiles : ",e.getMessage());
            }
        }
        return status;
    }

    public static Float calculateFileSizeInMB(String size) {
        long fileSizeInBytes = Long.parseLong(size);
        float fileSizeInKB = fileSizeInBytes / 1024;
        float sizeInMB = fileSizeInKB/1024;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Float.valueOf(twoDForm.format(sizeInMB));
    }

    public static String getSongDetails(Context context, SongInfo obj){
        String song_size = null,song_length = null,details = null,song_bitrate,song_format;

        MediaExtractor mex = new MediaExtractor();
        try {
            String pathOfSong = String.valueOf(obj.getSongUrl());
            mex.setDataSource(pathOfSong); // the address location of the sound on sdcard.
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mf = mex.getTrackFormat(0);

        int bitRate = mf.getInteger(MediaFormat.KEY_BIT_RATE);
        song_bitrate = String.valueOf(bitRate/1000);
        song_format = mf.getString(MediaFormat.KEY_MIME);

        Uri uri = Uri.parse("content://media/external/audio/media/"+obj.getSongId()+"");
        Cursor cursor = context.getContentResolver().query(uri,null,null,
                null,null);
        if(cursor != null) {
            if (cursor.moveToFirst()) {
                song_size = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
                song_length = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            }
            cursor.close();
        }

        if(song_size != null && song_length != null && song_format != null) {
            details = song_size+","+song_length+","+song_bitrate+","+song_format;
        }

        return details;
    }

    public static void shareSong(Context context){
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("audio/*");
        context.startActivity(Intent.createChooser(sharingIntent, "Share With"));
    }

    public static Bitmap getSongAlbumArt(Context context,Uri uri) {
        Bitmap bm = null;
        String path = null;
        MediaMetadataRetriever mmr;
        try{

            Cursor cursor = context.getContentResolver().query(uri,null,null,null,null);
            if(cursor != null && cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            }

            if(path != null) {
                mmr = new MediaMetadataRetriever();
                mmr.setDataSource(path);
                byte[] artBytes = mmr.getEmbeddedPicture();
                if (artBytes != null) {
                    //InputStream is = new ByteArrayInputStream(artBytes);
                    bm = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
                }
            }
            cursor.close();
        }
        catch (Exception problem) {
            Log.d("Bitmap Loading Problem",problem.getMessage());
        }
        return bm;
    }

    public static void setTimer(Context context, long mills){
        if(!isTimerOn) {
            sleep_timer = new SleepTimerClass(context, mills, 1000);
            sleep_timer.start();
            isTimerOn = true;
        }else {
            sleep_timer.cancel();
        }
    }

    public static class SleepTimerClass extends CountDownTimer {
        Context mCtx;
        public SleepTimerClass(Context context, long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            mCtx = context;
            //this.textView = textView;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String timeRemaining = getTime(millisUntilFinished/1000);
            //editSharedPreferenceForSleepTimer(mCtx,timeRemaining,R.string.sleep_time_remaining);
        }

        @Override
        public void onFinish() {
            isTimerOn = false;
            SongsListActivity.mediaPlayer.pause();
            PlayerActivity.isPlaying = false;
            SongsListActivity.checkIfUiNeedsToUpdate = true;
            PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
            Utility.recallNotificationBuilder(mCtx);

            editSharedPreferenceForSleepTimer(mCtx,"00",R.string.sleep_timer);
        }
    }

    private static void editSharedPreferenceForSleepTimer(Context ctx,String value,int id ){
        //SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getString(R.string.sleep_timer),Context.MODE_PRIVATE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor sleepTimeEditor = sharedPreferences.edit();
        sleepTimeEditor.putString(ctx.getString(id),value);
        sleepTimeEditor.apply();
    }


    @Deprecated
    public static void setTimer(Context context, long mills,TextView textView){
        if(!isTimerOn) {
            timer = new CounterClass(context, mills, 1000,textView);
            timer.start();
            isTimerOn = true;
        }else {
            timer.cancel();
        }
    }

    @Deprecated
    public static class CounterClass extends CountDownTimer {

        Context mCtx;
        TextView textView;

        public CounterClass(Context context, long millisInFuture, long countDownInterval, TextView textView) {
            super(millisInFuture, countDownInterval);
            mCtx = context;
            this.textView = textView;
        }

        @Override
        public void onTick(long millisUntilFinished) {
//            GlobalData.timerTimeRemaining = millisUntilFinished;
            String timeRemaining = getTime(millisUntilFinished/1000);

            textView.setText(timeRemaining);
//            seekBar.setProgress((int) (millisUntilFinished/1000));
        }

        @Override
        public void onFinish() {
            SongsListActivity.mediaPlayer.pause();
            SongsListActivity.checkIfUiNeedsToUpdate = true;
            PlayerActivity.checkIfPlayerUiNeedsToUpdate = true;
            Utility.recallNotificationBuilder(mCtx);
            isTimerOn = false;

            addTimerDetailsPreferences(mCtx,6,false);
        }
    }

    public static String getTime(long time) {
        String Sec = String.valueOf(time%60);
        time/=60;
        String Min = String.valueOf(time%60);
        time/=60;
        String Hour = "0" + String.valueOf(time%60);
        if(Sec.length() == 1)
            Sec = "0" + Sec;
        if(Min.length() == 1)
            Min = "0" + Min;
        return Hour + ":" + Min + ":" + Sec + " ";
    }

    final static ArrayList<String> musicFoldersList = new ArrayList<>();

    public static ArrayList<String> getMusicFoldersList(Context  context){
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, null, selection, null, sortOrder);
        int count = 0;

        if(cur != null) {
            count = cur.getCount();
            if(count > 0) {
                while(cur.moveToNext()) {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));

                    //String[] s = data.split("/");
                    //int len = s.length;
                    //String folderName = s[len-2];
                    String folderName = getFolderNameOutOfUrl(data);

                    File file = new File(data);
                    String path = file.getParent();
                    String total = folderName + "\n" + path;

                    if(!musicFoldersList.contains(folderName)) {
                        musicFoldersList.add(folderName);
                    }
                }
            }
            cur.close();
        }
        return musicFoldersList;
    }

    public static String getFolderNameOutOfUrl(String url){
        String[] s = url.split("/");
        int len = s.length;
        return s[len-2];
    }

    public static void recallNotificationBuilder(Context context) {
        NotificationGenerator.customNotification(context);
    }



    //+--------------------------------------------------------------------------------------------------+
    //.................................. Functions not in use ...........................................
    //+--------------------------------------------------------------------------------------------------+

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static void exitNotification(Context context){
        NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NotificationGenerator.NOTIFICATION_ID);
        }
    }


    public static void timePicker(Context context) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(context,new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                // tv_time.setText(hourOfDay + ":" + minute);
            }
        }, hour, minute, false);
        timePickerDialog.show();
    }


    public static void setAsRingtone(Context context,SongInfo obj){
        Uri newUri = Uri.parse("content://media/external/audio/media/"+obj.getSongId());
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);
        }
        catch (Throwable t) {

        }
    }

    public static void renameSong(SongInfo obj,String newName){
        String path = String.valueOf(obj.getSongUrl());

        File from = new File(path,obj.getSongName());
        File to = new File(path,newName.trim());
        from.renameTo(to);

        Log.i("Directory is", path);
        Log.i("From path is", from.toString());
        Log.i("To path is", to.toString());
    }


    @Deprecated
    public static boolean deleteSongOld(String filePath){
        boolean status = false;
        File file = new File(filePath);

        if(file.exists()){
            if(file.delete())
                status = true;
        }
        return status;
    }



    @Deprecated
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Deprecated
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    @Deprecated
    public static ArrayList<String> foldersWithMusic(Context context){
        ArrayList<String> musicFoldersList = null;
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        Cursor cursor;
        String selection;
        String[] projection = { MediaStore.Audio.Media.IS_MUSIC };
        String[] projection1 = new String[]{"COUNT(" + MediaStore.Files.FileColumns.DATA + ") AS totalFiles",
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.PARENT,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME
        };
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        selection = MediaStore.Audio.Media.DATA+" like"+"'%"+directory +"/%'";

        String selection1 = MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO +
                " OR "+ MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO +
                ") GROUP BY (" + MediaStore.Files.FileColumns.PARENT;
        cursor = context.getContentResolver().query(uri,projection1,selection1,null,null);
        if(cursor != null){
            if(cursor.moveToFirst()){
                String folder = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                //TODO i don't know let's see...........
                musicFoldersList.add(folder);
            }
        }
        return musicFoldersList;
    }

    @Deprecated
    public static ArrayList<String> foldersWithMusic2(final Context context){

        String removableStoragePath = null;
        File fileList[] = new File("/storage/").listFiles();
        for (File file : fileList) {
            if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath())
                    && file.isDirectory() && file.canRead())
                removableStoragePath = file.getAbsolutePath();
        }

        File baseDirectory = null;
        if(removableStoragePath == null) {
            //Your base dir here
            String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
            baseDirectory = new File(directory);
        }
        else{
            //Your base dir here
            baseDirectory = new File(removableStoragePath);
        }

        File[] files = baseDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                try {
                    boolean status = false;
                    File possibleMp3Folder = new File(dir, fileName);
                    if (possibleMp3Folder.isDirectory()) {
                        File[] filesList = possibleMp3Folder.listFiles();
                        for (File file : filesList) {
                            if(file.isDirectory()){
                                // foldersWithMusic2(context);
                            }
                            if (file.getName().toLowerCase().endsWith(".mp3")) {
                                status = true;
                                break;
                            }
                        }
                        if (status) {
                            musicFoldersList.add(fileName);
                        }
                    }

                }catch (Exception e){
                    e.getMessage();
                }
                return false;
            }
        });
        return musicFoldersList;
    }

}


/*

//                    String s1,s2,s3,s4,s5,s6,s7,s8;
//                    s1 = s[0];
//                    s2 = s[1];
//                    s3 = s[2];
//                    s4 = s[3];
//                    s5 = s[4];
//                    // Save to your list here
//                    if(isNumeric(s4)){
//                        String folderName = s5;
//                        if(!musicFoldersList.contains(folderName)) {
//                            musicFoldersList.add(folderName);
//                        }
//                    }
//                    else {
//                        String folderName = s4;
//                        if(!musicFoldersList.contains(folderName)) {
//                            musicFoldersList.add(folderName);
//                        }
//                    }


 */