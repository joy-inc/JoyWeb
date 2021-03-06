package com.joy.webview.ui;

import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.joy.inject.ActivityScope;
import com.joy.share.JoyShare;
import com.joy.share.ShareItem;
import com.joy.share.ShareUtil;
import com.joy.ui.activity.BaseUiActivity;
import com.joy.utils.LayoutInflater;
import com.joy.utils.LogMgr;
import com.joy.utils.TextUtil;
import com.joy.utils.ViewUtil;
import com.joy.webview.R;
import com.joy.webview.ui.interfaces.BaseViewWeb;
import com.joy.webview.utils.AnimatorUtils;
import com.joy.webview.view.NavigationBar;

import javax.inject.Inject;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.joy.ui.utils.DimenCons.HORIZONTAL_MARGINS;
import static com.joy.ui.utils.DimenCons.STATUS_BAR_HEIGHT;
import static com.joy.webview.ui.interfaces.KConstant.DP_40;
import static com.joy.webview.ui.interfaces.KConstant.KEY_TITLE;
import static com.joy.webview.ui.interfaces.KConstant.KEY_URL;

/**
 * Created by Daisw on 2016/11/4.
 */

@ActivityScope
public class UIDelegate {

    @Inject
    BaseViewWeb mBaseView;

    @Inject
    BaseUiActivity mActivity;

    String mUrl;
    CharSequence mTitle;
    TextView mTvTitle;
    boolean mTitleCloseEnable;
    Drawable mTitleCloseIcon;
    boolean mProgressEnable;
    ImageButton mIbTitleMore;
    ImageButton mIbTitleClose;
    JoyShare mJoyShare;
    ProgressBar mProgressBar;
    NavigationBar mNavBar;
    boolean mNavDisplay = false;
    boolean mNavAnimate = true;
    int mNavHeight;
    int mNavElevation;

    boolean mLongClickable = true;

    @Inject
    UIDelegate() {
    }

    void onCreate() {
        mActivity.setContentView(mBaseView.getPresenter().getWebView());
        mBaseView.getPresenter().load(mUrl);
    }

    void resolveThemeAttribute() {
        TypedArray themeTa = mActivity.obtainStyledAttributes(R.styleable.Theme);
        mLongClickable = themeTa.getBoolean(R.styleable.Theme_longClickable, true);
        mProgressEnable = themeTa.getBoolean(R.styleable.Theme_progressEnable, false);
        mTitleCloseIcon = themeTa.getDrawable(R.styleable.Theme_titleCloseIcon);
        mTitleCloseEnable = mTitleCloseIcon != null;
        themeTa.recycle();

        TypedArray navTa = mActivity.obtainStyledAttributes(R.styleable.NavigationBar);
        mNavDisplay = navTa.getBoolean(R.styleable.NavigationBar_navDisplay, false);
        mNavAnimate = navTa.getBoolean(R.styleable.NavigationBar_navAnimate, true);
        mNavHeight = navTa.getDimensionPixelSize(R.styleable.NavigationBar_navHeight, 0);
        mNavElevation = navTa.getDimensionPixelSize(R.styleable.NavigationBar_navElevation, 0);
        navTa.recycle();
    }

    void initData() {
        mUrl = mActivity.getIntent().getStringExtra(KEY_URL);
        mTitle = mActivity.getIntent().getCharSequenceExtra(KEY_TITLE);

        mJoyShare = new JoyShare(mActivity);
        mJoyShare.setData(mBaseView.getShareItems());
        mJoyShare.setOnItemClickListener((position, v, item) -> mBaseView.onShareItemClick(position, v, item));
    }

    void initTitleView() {
        if (mActivity.hasTitle()) {
            if (mActivity.hasTitleMore()) {
                mIbTitleMore = mActivity.getTitleMoreView();
                mIbTitleMore.setAlpha(0.f);
            }
            if (hasTitleClose()) {
                mIbTitleClose = mActivity.addTitleRightView(mTitleCloseIcon, getTitleCloseClickListener());
                mIbTitleClose.setAlpha(0.f);
            }
            if (mActivity.hasTitleMore() && hasTitleClose()) {
                mIbTitleMore.setMinimumWidth(DP_40);
                mIbTitleClose.setMinimumWidth(DP_40);
            }
            mActivity.setTitle(mTitle);
            if (TextUtil.isEmpty(mTitle)) {
                mTvTitle.setAlpha(0.f);
            }
            if (!mActivity.hasTitleBack()) {
                ((Toolbar.LayoutParams) mTvTitle.getLayoutParams()).leftMargin = HORIZONTAL_MARGINS;
            }
            if (mActivity.getToolbar().getChildCount() == 2) {
                ((Toolbar.LayoutParams) mTvTitle.getLayoutParams()).rightMargin = HORIZONTAL_MARGINS;
            }
        }
    }

    void initContentView() {
        mBaseView.getPresenter().getWebView().setOnLongClickListener((v) -> !mLongClickable);
        addProgressBarIfNecessary();
        addNavBarIfNecessary();
    }

    public JoyShare getJoyShare() {
        return mJoyShare;
    }

    boolean onShareItemClick(ShareItem item) {
        getJoyShare().dismiss();
        ShareItem.DEFAULT def = item.mDefault;
        if (def != null) {
            String curUrl = mBaseView.getPresenter().getUrl();
            String curTitle = mBaseView.getPresenter().getTitle();
            switch (def) {
                case COPY_LINK:
                    ShareUtil.copyUrl(mActivity, curUrl);
                    return true;
                case BROWSER:
                    ShareUtil.openBrowser(mActivity, curUrl);
                    return true;
                case MORE:
                    ShareUtil.shareTextUrl(mActivity, curUrl, curTitle);
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    public void setTitleCloseEnable(boolean enable) {
        mTitleCloseEnable = enable;
    }

    public boolean hasTitleClose() {
        return mTitleCloseEnable;
    }

    private View.OnClickListener getTitleCloseClickListener() {
        return (v) -> {
            if (v.getAlpha() == 1.f)
                mBaseView.onTitleCloseClick();
        };
    }

    public void fadeInTitleClose(long delay) {
        AnimatorUtils.fadeIn(mIbTitleClose, delay);
    }

    public void fadeInTitleMore(long delay) {
        AnimatorUtils.fadeIn(mIbTitleMore, delay);
    }

    public void fadeInTitleAll() {
        if (hasTitleClose()) {
            fadeInTitleClose(0);
            if (mActivity.hasTitleMore()) {
                fadeInTitleMore(200);
            }
        } else {
            if (mActivity.hasTitleMore()) {
                fadeInTitleMore(0);
            }
        }
    }

    public void setTitle(CharSequence title) {
        setTitle(title, false);
    }

    public void setTitle(CharSequence title, boolean fixed) {
        if (fixed) {
            mTitle = title;
        }
        mTvTitle = mActivity.getTitleTextView();
        AnimatorUtils.fadeIn(mTvTitle, title);
    }

    public boolean isProgressEnabled() {
        return mProgressEnable;
    }

    @SuppressWarnings("ResourceType")
    private void addProgressBarIfNecessary() {
        if (mProgressEnable) {
            mProgressBar = mBaseView.initProgressBar();
            mProgressBar.setAlpha(0.f);
            LayoutParams progressLp = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            if (mActivity.hasTitle() && !mActivity.isTitleOverlay()) {
                int height = mActivity.getToolbarHeight();
                progressLp.topMargin = mActivity.isSystemBarTrans() ? STATUS_BAR_HEIGHT + height : height;
            }
            mActivity.addContentView(mProgressBar, progressLp);
        }
    }

    ProgressBar initProgressBar() {
        return LayoutInflater.inflate(mActivity, R.layout.lib_view_web_progress_bar);
    }

    private void addNavBarIfNecessary() {
        if (mNavDisplay) {
            mNavBar = mBaseView.initNavigationBar();
            if (mNavBar != null) {
                addNavigationBar(mNavBar, generateNavBarLp(), mNavAnimate, false);
            }
        }
    }

    public void addNavigationBar(@NonNull NavigationBar navBar) {
        addNavigationBar(navBar, generateNavBarLp(), mNavAnimate);
    }

    public void addNavigationBar(@NonNull NavigationBar navBar, @NonNull LayoutParams lp) {
        addNavigationBar(navBar, lp, mNavAnimate);
    }

    public void addNavigationBar(@NonNull NavigationBar navBar, boolean animate) {
        addNavigationBar(navBar, generateNavBarLp(), animate);
    }

    public void addNavigationBar(@NonNull NavigationBar navBar, @NonNull LayoutParams lp, boolean animate) {
        addNavigationBar(navBar, lp, animate, true);
    }

    private void addNavigationBar(@NonNull NavigationBar navBar, @NonNull LayoutParams lp, boolean animate, boolean showLater) {
        if (animate) {
            navBar.setAlpha(0.f);
            navBar.setTranslationY(lp.height);
        } else {
            mActivity.getContentViewLp().bottomMargin = lp.height - mNavElevation;
        }
        mActivity.addContentView(navBar, lp);
        if (animate && showLater) {
            navBar.runEnterAnimator();
        }
        mNavDisplay = true;
        mNavBar = navBar;
        mNavHeight = lp.height;
        mNavAnimate = animate;
    }

    private LayoutParams generateNavBarLp() {
        LayoutParams lp = new LayoutParams(MATCH_PARENT, mNavHeight);
        lp.gravity = Gravity.BOTTOM;
        return lp;
    }

    public void setNavigationBarVisible(boolean visible) {
        if (mNavBar == null) {
            throw new NullPointerException("NavigationBar is null.");
        }
        if (visible) {
            ViewUtil.showView(mNavBar);
            mActivity.getContentViewLp().bottomMargin = mNavHeight - mNavElevation;
        } else {
            ViewUtil.hideView(mNavBar);
            mActivity.getContentViewLp().bottomMargin = 0;
        }
    }

    @Nullable
    NavigationBar initNavigationBar() {
        NavigationBar navBar = LayoutInflater.inflate(mActivity, R.layout.lib_view_web_navigation_bar);
        navBar.getIvNav(0).setOnClickListener((v1) -> mBaseView.getPresenter().goBack());
        navBar.getIvNav(1).setOnClickListener((v1) -> mActivity.finish());
        navBar.getIvNav(2).setOnClickListener((v1) -> mBaseView.getPresenter().goForward());
        navBar.getIvNav(3).setOnClickListener((v1) -> mBaseView.onNavCustomItemClick((ImageView) v1));
        navBar.getIvNav(4).setOnClickListener((v1) -> mJoyShare.show());
        return navBar;
    }

    void onPageStarted(String url, Bitmap favicon) {
        if (LogMgr.DEBUG) {
            LogMgr.d("core-web", mActivity.getClass().getSimpleName() + " onPageStarted # url: " + url);
        }
        AnimatorUtils.fadeIn(mProgressBar);
    }

    void onPageFinished(String url) {
        if (LogMgr.DEBUG) {
            LogMgr.d("core-web", mActivity.getClass().getSimpleName() + " onPageFinished # url: " + url);
        }
        fadeInTitleAll();
        if (mNavDisplay && mNavAnimate && mNavBar != null) {
            mNavBar.runEnterAnimator();
        }
    }

    void onReceivedError(int errorCode, String description, String failingUrl) {
        if (LogMgr.DEBUG) {
            LogMgr.d("core-web", mActivity.getClass().getSimpleName() + " onReceivedError # errorCode: " + errorCode + " description: " + description + " failingUrl: " + failingUrl);
        }
    }

    void onReceivedTitle(String title) {
        if (mActivity.hasTitle()) {
            if (TextUtil.isEmpty(mTitle)) {
                setTitle(title);
            }
        }
    }

    void onProgress(int progress) {
        if (mProgressEnable && mProgressBar != null) {
            mProgressBar.setProgress(progress);
            if (progress == 100) {
                AnimatorUtils.fadeOut(mProgressBar);
            }
        }
    }

    void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (mNavDisplay && mNavAnimate && mNavBar != null) {
            if (scrollY > oldScrollY) {// to down
                mNavBar.runExitAnimator();
            } else {// to up
                mNavBar.runEnterAnimator();
            }
        }
    }

    public NavigationBar getNavigationBar() {
        return mNavBar;
    }

    public String getInitialUrl() {
        return mUrl;
    }

    public ImageButton getTitleCloseBtn() {
        return mIbTitleClose;
    }
}
