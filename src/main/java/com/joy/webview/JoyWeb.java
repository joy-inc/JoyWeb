package com.joy.webview;

import android.support.annotation.Nullable;
import android.webkit.CookieManager;

import com.joy.ui.BaseApplication;
import com.joy.utils.TextUtil;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by Daisw on 2016/10/13.
 */

public class JoyWeb {

    private static String mUserAgent;

    private static String mCookie;
    private static boolean mCookieSeeded;

    private static boolean mAppCacheEnabled = true;
    private static String mAppCachePath = BaseApplication.getContext().getExternalCacheDir().toString();
    private static long mAppCacheMaxSize = 1024 * 1024 * 8;// 8M

    public static void setUserAgent(@Nullable String userAgent) {
        mUserAgent = userAgent;
    }

    @Nullable
    public static String getUserAgent() {
        return mUserAgent;
    }

    public static void setCookie(@Nullable String cookie) {
        mCookie = cookie;
        if (TextUtil.isEmpty(cookie)) {
            clearCookie();
        }
    }

    @Nullable
    public static String getCookie() {
        return mCookie;
    }

    public static void clearCookie() {
        mCookie = null;
        mCookieSeeded = false;
        removeAllCookies();
    }

    public static boolean isCookieSeeded() {
        return mCookieSeeded;
    }

    public static void setCookieSeeded(boolean cookieSeeded) {
        mCookieSeeded = cookieSeeded;
    }

    private static void removeAllCookies() {
        try {
            if (SDK_INT >= LOLLIPOP) {
                CookieManager.getInstance().removeAllCookies(value -> {
                });
            } else {
                CookieManager.getInstance().removeAllCookie();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isAppCacheEnabled() {
        return mAppCacheEnabled;
    }

    public static void setAppCacheEnabled(boolean appCacheEnabled) {
        mAppCacheEnabled = appCacheEnabled;
    }

    public static String getAppCachePath() {
        return mAppCachePath;
    }

    public static void setAppCachePath(String appCachePath) {
        mAppCachePath = appCachePath;
    }

    public static long getAppCacheMaxSize() {
        return mAppCacheMaxSize;
    }

    public static void setAppCacheMaxSize(long appCacheMaxSize) {
        mAppCacheMaxSize = appCacheMaxSize;
    }
}
