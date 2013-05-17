package com.rejasupotaro.hybridge.db;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.rejasupotaro.hybridge.db.entity.CacheContent;
import com.rejasupotaro.hybridge.utils.ExpiresTime;
import com.rejasupotaro.hybridge.utils.UriUtils;

public class DbCache {
    private static final String TAG = DbCache.class.getName();

    private static boolean sIsInitialized = false;
    private static Context sContext;
    private static DatabaseHelper sDatabaseHelper;
    private static final AsyncHttpClient sClient = new AsyncHttpClient();
    private static Map<String, CacheContent> sCacheMap = new HashMap<String, CacheContent>();

    public static synchronized void initialize(Application application) {
        if (sIsInitialized) {
            Log.v(TAG, "Hybridge is already initialized.");
            return;
        }

        sContext = application;
        sDatabaseHelper = new DatabaseHelper(sContext);

        loadPreloadContents();

        sIsInitialized = true;
    }

    public static Map<String, CacheContent> getContentMap() {
        return sCacheMap;
    }

    private static synchronized void loadPreloadContents() {
        Cursor c = null;
        try {
            c = sDatabaseHelper.getAllContents();
            if (c.moveToFirst()) {
                do {
                    CacheContent cacheContent = new CacheContent(c);
                    sCacheMap.put(cacheContent.getUrl(), cacheContent);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
    }

    public static synchronized void dispose() {
        sDatabaseHelper = null;
        sIsInitialized = false;
    }

    public static void preload(String url, final ExpiresTime expires) {
        final String formattedUrl = UriUtils.appendSlashIfNecessary(url);
        sClient.get(formattedUrl, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                sDatabaseHelper.savePreloadContent(formattedUrl, response, expires);
                loadPreloadContents();
            }

            @Override
            public void onFailure(Throwable e, String response) {
                // TODO impl me
            }
        });
    }

    public static void drop(String url) {
        sDatabaseHelper.deleteContent(url);
        if (sCacheMap.containsKey(url)) {
            sCacheMap.remove(url);
        }
    }
}
