package com.jack.lrumemorycachedemo;

import android.graphics.Bitmap;

import java.util.Collection;

/**
 * Created by jack on 2/14/16.
 */
public interface MemoryCache {
    Bitmap get(String key);
    boolean put(String key, Bitmap bitmap);
    Bitmap remove(String key);
    Collection<String> keys();
    void clear();
}
