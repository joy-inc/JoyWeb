package com.joy.webview.presenter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.joy.utils.LogMgr;
import com.joy.utils.TextUtil;
import com.joy.webview.JoyWeb;
import com.joy.webview.ui.interfaces.BaseViewWeb;
import com.joy.webview.utils.DocumentParser;
import com.joy.webview.utils.PayIntercepter;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient.CustomViewCallback;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by Daisw on 16/8/14.
 */

@SuppressLint("AddJavascriptInterface")
public class BaseWebX5Presenter implements IPresenter {

    @Inject
    WebView mWebView;

    @Inject
    BaseViewWeb mBaseView;

    private String mInitialUrl;
    private String mTempUrl;
    private Document mDocument;
    private boolean mIsError;
    private boolean mNeedSeedCookie;
    private int mCurIndex;
    private Map<String, Boolean> mSessionFinished;

    @Inject
    BaseWebX5Presenter() {
        mSessionFinished = new HashMap<>();
    }

    @Inject
    void setWebViewClient() {

        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                String prevUrl = view.getUrl();
                boolean isRedirected = mSessionFinished.get(prevUrl) != null && !mSessionFinished.get(prevUrl);
                if (isRedirected) {// 如果initialUrl被重定向了，则跳出方法体。
                    return;
                }
                mSessionFinished.put(url, false);
                mIsError = false;
                mBaseView.hideTipView();
                if (!mBaseView.isProgressEnabled()) {
                    mBaseView.hideContent();
                    mBaseView.showLoading();
                }
                if (!mNeedSeedCookie) {
                    mBaseView.onPageStarted(url, favicon);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (mNeedSeedCookie) {
                    mNeedSeedCookie = false;
                    mWebView.loadUrl(mTempUrl);
                } else {
                    mIsError = true;
                    if (!mBaseView.isProgressEnabled()) {
                        mBaseView.hideLoading();
                    }
                    mBaseView.hideContent();
                    mBaseView.showErrorTip();
                    mBaseView.onReceivedError(errorCode, description, failingUrl);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mSessionFinished.put(url, true);
                if (!url.equals(view.getUrl())) {// 如果当前URL和webview所持有的URL不一致时，抛掉当前URL的回调，跳出方法体。
                    return;
                }
                if (mNeedSeedCookie) {
                    mNeedSeedCookie = false;
                    JoyWeb.setCookieSeeded(true);
                    mWebView.loadUrl(mTempUrl);
                } else if (!mIsError) {
                    mCurIndex = mWebView.copyBackForwardList().getCurrentIndex();
                    if (mCurIndex == -1) {
                        return;
                    }
                    if (!mBaseView.isProgressEnabled()) {
                        mBaseView.hideLoading();
                    }
                    mBaseView.hideTipView();
                    mBaseView.showContent();
                    getHtmlByTagName("html", 0);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (PayIntercepter.interceptPayIntent(view.getContext(), url)) {
                    return true;
                }
                String prevUrl = view.getUrl();
                boolean isAutoRedirect = mSessionFinished.get(prevUrl) != null && !mSessionFinished.get(prevUrl);
                if (isAutoRedirect) {// 如果是自动重定向，则交给webview处理。
                    LogMgr.d("core-web", "BaseWebX5Presenter shouldOverrideUrlLoading # auto redirect");
                    return super.shouldOverrideUrlLoading(view, url);
                }
                boolean consumed = mBaseView.onOverrideUrl(url);
                if (!consumed) {
                    mCurIndex++;
                }
                return consumed;
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mBaseView.onShowCustomView(view);
            }

            @Override
            public void onHideCustomView() {
                mBaseView.onHideCustomView();
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (!mIsError && !mNeedSeedCookie) {
                    mBaseView.onReceivedTitle(title);
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!mNeedSeedCookie) {
                    mBaseView.onProgress(newProgress);
                }
            }

            // file upload callback (Android 3.0 (API level 11) -- Android 4.0 (API level 15)) (hidden method)
            @TargetApi(HONEYCOMB)
            public void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType) {
                mBaseView.onShowFileChooser(filePathCallback, acceptType);
            }

            // file upload callback (Android 4.1 (API level 16) -- Android 4.3 (API level 18)) (hidden method)
            @Override
            @TargetApi(JELLY_BEAN)
            public void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType, String capture) {
                mBaseView.onShowFileChooser(filePathCallback, acceptType);
            }

            // for >= Lollipop, all in one
            @Override
            @TargetApi(LOLLIPOP)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return mBaseView.onShowFileChooser(filePathCallback);
            }
        });
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength)
                -> mBaseView.onDownloadStart(url, userAgent, contentDisposition, mimetype, contentLength));
        mWebView.addJavascriptInterface(new JSHtmlSource() {
            @JavascriptInterface
            @WorkerThread
            @Override
            public void receivedHtml(String html) {
                mWebView.post(() -> onReceivedHtml(html));
            }
        }, "htmlSource");
    }

    private void onReceivedHtml(String html) {
        mDocument = Jsoup.parse(html);
        mBaseView.onPageFinished(mWebView.getUrl());
    }

    @SuppressLint("DefaultLocale")
    private void getHtmlByTagName(String tag, int index) {
        String format = "javascript:window.htmlSource.receivedHtml(document.getElementsByTagName('%s')[%d].outerHTML);";
        mWebView.loadUrl(String.format(format, tag, index));
    }

    @Override
    public void setUserAgent(String userAgent) {
        if (TextUtil.isNotEmpty(userAgent)) {
            WebSettings settings = mWebView.getSettings();
            settings.setUserAgentString(settings.getUserAgentString() + " " + userAgent);
        }
    }

    @Override
    public Document getDocument() {
        return mDocument;
    }

    @Nullable
    @Override
    public Elements getElementsByTag(String tagName) {
        return DocumentParser.getElementsByTag(mDocument, tagName);
    }

    @Nullable
    @Override
    public Element getElementByTag(String tagName, int index) {
        return DocumentParser.getElementByTag(mDocument, tagName, index);
    }

    @Nullable
    @Override
    public Element getFirstElementByTag(String tagName) {
        return DocumentParser.getFirstElementByTag(mDocument, tagName);
    }

    @Override
    public String getTag(String tagName) {
        return DocumentParser.getTag(mDocument, tagName);
    }

    @Override
    public String getAttribute(String attrName, String attrValue, String attributeKey) {
        return DocumentParser.getAttribute(mDocument, attrName, attrValue, attributeKey);
    }

    @Override
    public WebView getWebView() {
        return mWebView;
    }

    @Override
    public String url() {
        return mWebView.getUrl();
    }

    /**
     * Get the index of the current history item. This index can be used to
     * directly index into the array list.
     *
     * @return The current index from 0...n or -1 if the list is empty.
     */
    @Override
    public int getCurrentIndex() {
        return mCurIndex;
    }

    @Override
    public void load(String url) {
        if (TextUtil.isNotEmpty(url)) {
            if (mInitialUrl == null) {
                mInitialUrl = url;
            }
            String cookieUrl = JoyWeb.getCookie();
            mNeedSeedCookie = TextUtil.isNotEmpty(cookieUrl) && !JoyWeb.isCookieSeeded();
            if (mNeedSeedCookie) {
                mTempUrl = url;
                mWebView.loadUrl(cookieUrl);
            } else {
                mWebView.loadUrl(url);
            }
        }
    }

    @Override
    public void reload() {
        load(mWebView.getUrl());
    }

    @Override
    public boolean canGoBack() {
        return mWebView.canGoBack();
    }

    @Override
    public boolean canGoForward() {
        return mWebView.canGoForward();
    }

    @Override
    public void goBack() {
        mCurIndex = mWebView.copyBackForwardList().getCurrentIndex() - 1;
        boolean isFirstPage = mWebView.getUrl().equals(mInitialUrl);
        if (isFirstPage || !canGoBack()) {
            mBaseView.finish();
        } else {
            mWebView.goBack();
        }
    }

    @Override
    public void goForward() {
        if (canGoForward()) {
            mCurIndex++;
            mWebView.goForward();
        }
    }
}
