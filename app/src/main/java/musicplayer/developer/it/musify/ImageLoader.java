package musicplayer.developer.it.musify;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by anupam on 25-12-2017.
 */

public class ImageLoader {

    private Context c;
    private MemoryCache memoryCache = new MemoryCache();
    private FileCache fileCache;
    private Map<ImageView, String> imageViews= Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private ExecutorService executorService;
    private Handler handler=new Handler();

    private final int stub_id = R.drawable.default_album_art;

    public ImageLoader(Context context){
        c = context;
        fileCache = new FileCache(context);
        executorService = Executors.newFixedThreadPool(5);
    }

    public void DisplayImage(String url, ImageView imageView) {
        imageViews.put(imageView, url);
        Bitmap bitmap = memoryCache.get(url);
        if(bitmap != null)
            imageView.setImageBitmap(bitmap);
        else {
            queuePhoto(url, imageView);
            imageView.setImageResource(stub_id);
        }
    }

    private void queuePhoto(String url, ImageView imageView) {
        PhotoToLoad p = new PhotoToLoad(url, imageView);
        executorService.submit(new PhotosLoader(p));
    }

    //Task for the queue
    private class PhotoToLoad {
        private String url;
        private ImageView imageView;

        //Constructor
        private PhotoToLoad(String u, ImageView i){url=u; imageView=i;}
    }

    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;
        PhotosLoader(PhotoToLoad photoToLoad){
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {
            try{
                if(imageViewReused(photoToLoad))
                    return;
                Bitmap bmp = getBitmap(photoToLoad.url);
                memoryCache.put(photoToLoad.url, bmp);
                if(imageViewReused(photoToLoad))
                    return;
                BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
                handler.post(bd);
            }catch(Throwable th){
                th.printStackTrace();
            }
        }
    }

    private Bitmap getBitmap(String url) {
        try {
            Bitmap bitmap = Utility.getSongAlbumArt(c, Uri.parse(url));
            if (bitmap != null)
                return bitmap;
        }
        catch (Exception e){
            Log.i("GET_BITMAP_ERROR",e.getMessage());
        }
        return null;
    }

    class BitmapDisplayer implements Runnable {
        Bitmap bitmap;
        PhotoToLoad photoToLoad;
        private BitmapDisplayer(Bitmap b, PhotoToLoad p){bitmap=b; photoToLoad=p;}
        public void run() {
            if(imageViewReused(photoToLoad))
                return;
            if(bitmap!=null)
                photoToLoad.imageView.setImageBitmap(bitmap);
            else
                photoToLoad.imageView.setImageResource(stub_id);
        }
    }

    private boolean imageViewReused(PhotoToLoad photoToLoad){
        String tag = imageViews.get(photoToLoad.imageView);

        return tag == null || !tag.equals(photoToLoad.url);
    }



    //..................METHODS NOT IN USE...................
    @Deprecated
    public Bitmap getSongAlbmArt(Uri musicUri1){

        ContentResolver musicResolver = c.getContentResolver();
        Cursor musicCursor = null;
        try {
            musicCursor = musicResolver.query(musicUri1, null, null, null, null);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        Bitmap bm=null;
        if (musicCursor != null && musicCursor.moveToFirst())
        {
            String musicFilePath = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA));

            try{
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(musicFilePath);
                byte[] artBytes = mmr.getEmbeddedPicture();

                if (artBytes != null) {
                    InputStream is = new ByteArrayInputStream(artBytes);
                    bm = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
                }
            }
            catch (Exception problem) {
                System.out.println(problem.toString());
            }
        }
        return bm;
    }


    //decodes image and scales it to reduce memory consumption
    @Deprecated
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream stream1=new FileInputStream(f);
            BitmapFactory.decodeStream(stream1,null,o);
            stream1.close();

            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=70;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            FileInputStream stream2=new FileInputStream(f);
            Bitmap bitmap=BitmapFactory.decodeStream(stream2, null, o2);
            stream2.close();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }
}
