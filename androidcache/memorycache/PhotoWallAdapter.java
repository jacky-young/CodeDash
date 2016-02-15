package com.jack.lrumemorycachedemo;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jack on 2/14/16.
 */
public class PhotoWallAdapter extends ArrayAdapter<String> implements AbsListView.OnScrollListener {

    private GridView mPhotoWall;
    private MemoryCache memoryCache;
    private Set<BitmapWorkerTask> taskCollection;

    private int mFirstVisibleItem;
    private int mVisibleItemCount;
    private boolean isFirstEnter = true;

    public PhotoWallAdapter(Context context, int textViewResourceId, String[] objects, GridView photowall) {
        super(context, textViewResourceId, objects);
        mPhotoWall = photowall;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int memory = am.getLargeMemoryClass();
        memoryCache = new LruMemoryCache(memory * 1024 * 1024 / 8);
//        long maxMemory = Runtime.getRuntime().maxMemory();
//        memoryCache = new LruMemoryCache((int)(maxMemory / 8));
        taskCollection = new HashSet<BitmapWorkerTask>();
        mPhotoWall.setOnScrollListener(this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String url = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.photo_layout, null);
        } else {
            view = convertView;
        }
        final ImageView photo = (ImageView) view.findViewById(R.id.photo);
        //给每一个ImageView设置一个Tag，保证异步时不会乱序
        photo.setTag(url);
        setImageView(url, photo);
        return view;
    }

    private void setImageView(String imageUrl, ImageView imageView) {
        Bitmap bitmap = memoryCache.get(imageUrl);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.drawable.stat_sys_download);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        //仅当GridView静止时才去下载照片，滑动时取消下载任务
        if (scrollState == SCROLL_STATE_IDLE) {
            loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
        } else {
            cancelAllTasks();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d("BBB", "[firstVisibleItem:"+firstVisibleItem+"][visibleItemCount:"+visibleItemCount
                        +"][totalItemCount:"+totalItemCount+"]");
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;
        //下载任务放在onScrollStateChanged()中调用，但首次进入程序onScrollStateChanged()不会调用，
        //因此通过参数isFirstEnter在首次进入时开启下载任务
        if (isFirstEnter && visibleItemCount > 0) {
            loadBitmaps(firstVisibleItem, visibleItemCount);
            isFirstEnter = false;
        }
    }

    public void cancelAllTasks() {
        if (taskCollection != null) {
            for (BitmapWorkerTask task : taskCollection) {
                task.cancel(false);
            }
        }
    }

    private void loadBitmaps(int firstVisibleItem, int visibleItemCount) {
        try {
            for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
                String imageUrl = Images.imageThumbUrls[i];
                Bitmap bitmap = memoryCache.get(imageUrl);
                Log.d("BBB", memoryCache.toString());
                if (bitmap == null){
                    BitmapWorkerTask task = new BitmapWorkerTask();
                    taskCollection.add(task);
                    task.execute(imageUrl);
                } else {
                    ImageView imageView = (ImageView) mPhotoWall.findViewWithTag(imageUrl);
                    if (imageView != null && bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... strings) {
            imageUrl = strings[0];
            //后台开始下载图片
            Bitmap bitmap = downloadBitmap(imageUrl);
            if (bitmap != null) {
                //图片下载完成后缓存到Cache
                memoryCache.put(imageUrl, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //根据Tag找到相应的ImageView，显示出来
            ImageView imageView = (ImageView)mPhotoWall.findViewWithTag(imageUrl);
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            taskCollection.remove(this);
        }

        private Bitmap downloadBitmap(String imageUrl) {
            Bitmap bitmap = null;
            HttpURLConnection con = null;
            try {
                URL url = new URL(imageUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5 * 1000);
                con.setReadTimeout(10 * 1000);
                bitmap = BitmapFactory.decodeStream(con.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            return bitmap;
        }
    }
}
