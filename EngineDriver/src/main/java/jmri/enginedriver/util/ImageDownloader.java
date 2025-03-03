/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jmri.enginedriver.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This helper class download images from the Internet and binds those with the provided ImageView.
 * <p>
 * <p>It requires the INTERNET permission, which should be added to your application's manifest
 * file.</p>
 * <p>
 * A local cache of downloaded images is maintained internally to improve performance.
 */
public class ImageDownloader {

    public ImageDownloader() {
        resetPurgeTimer();
    }

    public void requestImage(String sourceUrl, ImageView imageView) {
        Bitmap bitmap = getBitmapFromCache(sourceUrl);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            new DownloadBitmapInBackground(sourceUrl, imageView);
        }
    }

    public class DownloadBitmapInBackground implements Runnable {
        ImageView imageView;
        String sourceUrl;

        DownloadBitmapInBackground(String sourceUrl, ImageView imageView) {
            this.sourceUrl = sourceUrl;
            this.imageView = imageView;
            new Thread(this).start();
        }

        @Override
        public void run() {
            downloadBitmap(sourceUrl, imageView);
            Log.d("Engine_Driver", "ImagDownloader: downloadBitmapInBackground.run:");
        }
    } // end downloadBitmapInBackground()

    private void downloadBitmap(String sourceUrl, ImageView imageView) {
        try {
            java.net.URL url = new java.net.URL(sourceUrl);
            HttpURLConnection con=(HttpURLConnection)url.openConnection();
            con.setDoInput(true);
            con.connect();
            InputStream input=con.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);

            if (bitmap != null) {
                addBitmapToCache(sourceUrl, bitmap);
                imageView.setImageBitmap(bitmap);
            }

        } catch(Exception e){
            Log.w("Engine_Driver", "Error while retrieving bitmap from " + sourceUrl, e);
        }
    }


    /*
     * Cache-related fields and methods.
     * 
     * We use a hard and a soft cache. A soft reference cache is too aggressively cleared by the
     * Garbage Collector.
     */

    private static final int HARD_CACHE_CAPACITY = 40;
    private static final int DELAY_BEFORE_PURGE = 30 * 1000; // in milliseconds

    // Hard cache, with a fixed maximum capacity and a life duration
    private final HashMap<String, Bitmap> sHardBitmapCache =
            new LinkedHashMap<String, Bitmap>(HARD_CACHE_CAPACITY / 2, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
                    if (size() > HARD_CACHE_CAPACITY) {
                        // Entries push-out of hard reference cache are transferred to soft reference cache
                        sSoftBitmapCache.put(eldest.getKey(), new SoftReference<>(eldest.getValue()));
                        return true;
                    } else
                        return false;
                }
            };

    // Soft cache for bitmaps kicked out of hard cache
    private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache =
            new ConcurrentHashMap<>(HARD_CACHE_CAPACITY / 2);

    private final Handler purgeHandler = new Handler();

    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };

    /**
     * Adds this bitmap to the cache.
     *
     * @param bitmap The newly downloaded bitmap.
     */
    private void addBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                sHardBitmapCache.put(url, bitmap);
            }
        }
    }

    /**
     * @param url The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    private Bitmap getBitmapFromCache(String url) {
        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final Bitmap bitmap = sHardBitmapCache.get(url);
            if (bitmap != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(url);
                sHardBitmapCache.put(url, bitmap);
                return bitmap;
            }
        }

        // Then try the soft reference cache
        SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
                // Bitmap found in soft cache
                return bitmap;
            } else {
                // Soft reference has been Garbage Collected
                sSoftBitmapCache.remove(url);
            }
        }

        return null;
    }

    /**
     * Clears the image cache used internally to improve performance. Note that for memory
     * efficiency reasons, the cache will automatically be cleared after a certain inactivity delay.
     */
    public void clearCache() {
        sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }

    /**
     * Allow a new delay before the automatic cache clear is done.
     */
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }
}
