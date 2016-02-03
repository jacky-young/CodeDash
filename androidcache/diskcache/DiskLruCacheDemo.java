package com.jack.disklrucachedemo.libcore.io;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jack.disklrucachedemo.R;
import com.jack.disklrucachedemo.naming.Md5FileNameGenerator;
import com.jack.disklrucachedemo.utils.StorageUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity {
    private String imageUrl = "http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg";
    private ImageView mImageView;
    private Button mReadLruCache;
    private TextView mSize;
    private Md5FileNameGenerator generator;
    protected DiskLruCache lruCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        runWriteCacheThread();
        mReadLruCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String key = generator.generate(imageUrl);
                    // Snapshot对象用来获得缓存文件的输入流
                    // 同样,index传入0即可
                    // 其它API：size() flush() close() delete()
                    DiskLruCache.Snapshot snapshot = lruCache.get(key);
                    if (snapshot != null) {
                        InputStream is = snapshot.getInputStream(0);
                        Bitmap b = BitmapFactory.decodeStream(is);
                        mImageView.setImageBitmap(b);
                    }
                    long size = lruCache.size();
                    mSize.setText(size+"B");
                } catch(IOException e) {}
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initData() {
        mSize = (TextView)findViewById(R.id.tv_size);
        mReadLruCache = (Button)findViewById(R.id.btn_read_diskcache);
        mImageView = (ImageView)findViewById(R.id.iv_diskcache);
        generator = new Md5FileNameGenerator();
        try {
            File cacheDir = StorageUtils.getCacheDirectory(getApplicationContext());
            // open(File directory, int appVersion, int valueCount, long maxSize, int maxFileCount)
            // @param directory a writable directory
            // @param valueCount the number of values per cache entry. Must be positive.
            // @param maxSize the maximum number of bytes this cache should use to store
            // @param maxFileCount the maximum file count this cache should store
            lruCache = DiskLruCache.open(cacheDir, 2, 1, 10*1024*1024, 100);
        } catch (IOException e){}
    }

    private boolean downloadUrlToStream(String urlString, OutputStream out) {
        HttpURLConnection connection = null;
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
            final URL url = new URL(urlString);
            int size = 8 * 1024;
            connection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(connection.getInputStream(), size);
            os = new BufferedOutputStream(out, size);
            int buffer;
            while((buffer = is.read()) != -1) {
                os.write(buffer);
            }
            return  true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {

            }
        }
        return false;
    }

    private void runWriteCacheThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 生成MD5 Key作为存储的name,利用Editor实例创建一个输出流,
                    // 由于前面valueCount指定为1,这里index传0即可,执行完操作要提交
                    // LruCache.flush()是为了同步操作到journal文件
                    String key = generator.generate(imageUrl);
                    DiskLruCache.Editor editor = lruCache.edit(key);
                    if (editor != null) {
                        OutputStream os = editor.newOutputStream(0);
                        if (downloadUrlToStream(imageUrl, os)) {
                            editor.commit();
                        } else {
                            editor.abort();
                        }
                    }
                    lruCache.flush();
                } catch (IOException e) {}
            }
        }).start();
    }
}
