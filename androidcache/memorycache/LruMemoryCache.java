package com.jack.lrumemorycachedemo;

import android.graphics.Bitmap;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jack on 2/14/16.
 */
public class LruMemoryCache implements MemoryCache {

    private final LinkedHashMap<String, Bitmap> map;
    private final int maxSize;
    private int size;

    public LruMemoryCache(int maxsize) {
        if (maxsize < 0) {
            throw new IllegalArgumentException("MaxSize < 0");
        }
        this.maxSize = maxsize;
        this.map = new LinkedHashMap<String, Bitmap>(0, 0.75f, true);
    }

    @Override
    public final Bitmap get(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            return map.get(key);
        }
    }

    @Override
    public final boolean put(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            throw new NullPointerException("key == null || bitmap == null");
        }
        synchronized (this) {
            size += sizeOf(bitmap);
            Bitmap previous = map.put(key, bitmap);
            if (previous != null) {
                size -= sizeOf(previous);
            }
        }
        trimToSize(maxSize);
        return true;
    }

    @Override
    public final Bitmap remove(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            Bitmap bitmap = map.remove(key);
            if (bitmap != null) {
                size -= sizeOf(bitmap);
            }
            return bitmap;
        }
    }

    @Override
    public Collection<String> keys() {
        synchronized (this) {
            return new HashSet<String>(map.keySet());
        }
    }

    @Override
    public void clear() {
        trimToSize(-1);
    }

    private int sizeOf(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    private void trimToSize(int maxsize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                //firstly make the judgement
                if (size < 0 || (map.isEmpty() && size > 0)) {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent result");
                }

                if (size <= maxsize || map.size() == 0) {
                    break;
                }

                Map.Entry<String, Bitmap> entry = map.entrySet().iterator().next();
                if (entry == null) {
                    break;
                }
                key = entry.getKey();
                value = entry.getValue();
                map.remove(key);
                size -= sizeOf(value);
            }
        }
    }

    @Override
    public final String toString() {
        return String.format("LruMemoryCache[maxSize=%d][sizeUsed=%d][percentUsed=%.1f%%]", maxSize, size, size*100.0f/maxSize);
    }
}
