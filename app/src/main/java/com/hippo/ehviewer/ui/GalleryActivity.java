/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.hippo.anani.AnimationUtils;
import com.hippo.anani.SimpleAnimatorListener;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.DirGalleryProvider;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProviderListener;
import com.hippo.ehviewer.gallery.ZipGalleryProvider;
import com.hippo.ehviewer.gallery.gl.GalleryPageView;
import com.hippo.ehviewer.gallery.gl.GalleryView;
import com.hippo.gl.glrenderer.ImageTexture;
import com.hippo.gl.view.GLRootView;
import com.hippo.image.Image;
import com.hippo.util.SystemUiHelper;
import com.hippo.widget.Slider;
import com.hippo.yorozuya.ConcurrentPool;
import com.hippo.yorozuya.SimpleHandler;

import java.io.File;

public class GalleryActivity extends AppCompatActivity
        implements GalleryProviderListener, Slider.OnSetProgressListener,
        GalleryView.Listener {

    public static final String ACTION_DIR = "dir";
    public static final String ACTION_ZIP = "zip";
    public static final String ACTION_EH = "eh";

    public static final String KEY_ACTION = "action";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String KEY_SHOWING = "showing";

    private String mAction;
    private String mFilename;
    private GalleryInfo mGalleryInfo;

    @Nullable
    private GalleryView mGalleryView;

    @Nullable
    private ImageTexture.Uploader mUploader;
    @Nullable
    private GalleryProvider mGalleryProvider;

    @Nullable
    private SystemUiHelper mSystemUiHelper;
    private boolean mSystemUiShowing = false;

    @Nullable
    private View mSliderPanel;
    @Nullable
    private TextView mLeftText;
    @Nullable
    private TextView mRightText;
    @Nullable
    private Slider mSlider;

    private int mLayoutMode;
    private int mSize;
    private int mCurrentIndex;

    private final ConcurrentPool<NotifyTask> mNotifyTaskPool = new ConcurrentPool<>(3);

    private final Runnable mRequestLayoutSliderTask = new Runnable() {
        @Override
        public void run() {
            if (mSliderPanel != null) {
                mSliderPanel.requestLayout();
            }
        }
    };

    private final SimpleAnimatorListener mHideSliderListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (mSliderPanel != null) {
                mSliderPanel.setVisibility(View.INVISIBLE);
            }
        }
    };

    private void buildProvider() {
        if (mGalleryProvider != null) {
            return;
        }

        if (ACTION_DIR.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new DirGalleryProvider(new File(mFilename));
            }
        } else if (ACTION_ZIP.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new ZipGalleryProvider(new File(mFilename));
            }
        } else if (ACTION_EH.equals(mAction)) {
            if (mGalleryInfo != null) {
                mGalleryProvider = new EhGalleryProvider(this, mGalleryInfo);
            }
        }
    }

    private void onInit() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        mAction = intent.getAction();
        mFilename = intent.getStringExtra(KEY_FILENAME);
        mGalleryInfo = intent.getParcelableExtra(KEY_GALLERY_INFO);
        buildProvider();
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mAction = savedInstanceState.getString(KEY_ACTION);
        mFilename = savedInstanceState.getString(KEY_FILENAME);
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        buildProvider();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putString(KEY_FILENAME, mFilename);
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
        }
        outState.putBoolean(KEY_SHOWING, mSystemUiShowing);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }

        if (mGalleryProvider == null) {
            finish();
            return;
        }
        mGalleryProvider.setGalleryProviderListener(this);
        mGalleryProvider.start();

        setContentView(R.layout.activity_gallery);
        GLRootView glRootView = (GLRootView) findViewById(R.id.gl_root_view);
        mGalleryProvider.setGLRoot(glRootView);
        mUploader = new ImageTexture.Uploader(glRootView);

        mGalleryView = new GalleryView(this, new GalleryAdapter(),
                this, Settings.getReadingDirection());
        glRootView.setContentPane(mGalleryView);

        // System UI helper
        mSystemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE,
                SystemUiHelper.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES | SystemUiHelper.FLAG_IMMERSIVE_STICKY);
        if (savedInstanceState == null || !savedInstanceState.getBoolean(KEY_SHOWING, false)) {
            mSystemUiHelper.hide();
            mSystemUiShowing = false;
        } else {
            mSystemUiHelper.show();
            mSystemUiShowing = true;
        }

        mSliderPanel = findViewById(R.id.slider_panel);
        mLeftText = (TextView) mSliderPanel.findViewById(R.id.left);
        mRightText = (TextView) mSliderPanel.findViewById(R.id.right);
        mSlider = (Slider) mSliderPanel.findViewById(R.id.slider);
        mSlider.setOnSetProgressListener(this);

        mSize = mGalleryProvider.size();
        mCurrentIndex = mGalleryView.getCurrentIndex();
        mLayoutMode = mGalleryView.getLayoutMode();
        updateSlider();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGalleryView = null;
        if (mUploader != null) {
            mUploader.clear();
            mUploader = null;
        }
        if (mGalleryProvider != null) {
            mGalleryProvider.setGalleryProviderListener(null);
            mGalleryProvider.stop();
            mGalleryProvider = null;
        }

        mSliderPanel = null;
        mLeftText = null;
        mRightText = null;
        mSlider = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && mSystemUiHelper != null) {
            if (mSystemUiShowing) {
                mSystemUiHelper.show();
            } else {
                mSystemUiHelper.hide();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mGalleryView == null) {
            return super.onKeyDown(keyCode, event);
        }

        // Check volume
        if (Settings.getVolumePage()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            }
        }

        // Check keyboard and Dpad
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MENU:
                onTapMenuArea();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Check volume
        if (Settings.getVolumePage()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                    keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }

        // Check keyboard and Dpad
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_SPACE ||
                keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private GalleryPageView findPageByIndex(int index) {
        if (mGalleryView != null) {
            return mGalleryView.findPageByIndex(index);
        } else {
            return null;
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateSlider() {
        if (mSlider == null || mRightText == null || mLeftText == null || mSize <= 0 || mCurrentIndex < 0) {
            return;
        }

        TextView start;
        TextView end;
        if (mLayoutMode == GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT) {
            start = mRightText;
            end = mLeftText;
            mSlider.setReverse(true);
        } else {
            start = mLeftText;
            end = mRightText;
            mSlider.setReverse(false);
        }
        start.setText(Integer.toString(mCurrentIndex + 1));
        end.setText(Integer.toString(mSize));
        mSlider.setRange(1, mSize);
        mSlider.setProgress(mCurrentIndex + 1);
    }

    @Override
    public void onDataChanged() {
        if (mGalleryView != null) {
            mGalleryView.onDataChanged();
        }

        if (mGalleryProvider != null) {
            int size = mGalleryProvider.size();
            NotifyTask task = mNotifyTaskPool.pop();
            if (task == null) {
                task = new NotifyTask();
            }
            task.setData(NotifyTask.KEY_SIZE, size);
            SimpleHandler.getInstance().post(task);
        }
    }

    @Override
    public void onPageWait(int index) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_INDETERMINATE);
            page.setError(null, null);
        }
    }

    @Override
    public void onPagePercent(int index, float percent) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(percent);
            page.setError(null, null);
        }
    }

    @Override
    public void onPageSucceed(int index, Image image) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null) {
            page.showImage();
            page.setImage(image);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_GONE);
            page.setError(null, null);
        } else {
            image.recycle();
        }
    }

    @Override
    public void onPageFailed(int index, String error) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null && mGalleryView != null) {
            page.showInfo();
            page.setImage(null);
            page.setPage(index + 1);
            page.setProgress(GalleryPageView.PROGRESS_GONE);
            page.setError(error, mGalleryView);
        }
    }

    @Override
    public void onDataChanged(int index) {
        GalleryPageView page = findPageByIndex(index);
        if (page != null && mGalleryProvider != null) {
            mGalleryProvider.request(index);
        }
    }

    @Override
    public void onSetProgress(Slider slider, int newProgress, int oldProgress,
            boolean byUser, boolean confirm) {
        if (confirm && byUser && mGalleryView != null) {
            mGalleryView.setCurrentPage(newProgress - 1);
        }
    }

    @Override
    public void onUpdateCurrentIndex(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_CURRENT_INDEX, index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapSliderArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_SLIDER_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapMenuArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_MENU_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onLongPressPage(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_LONG_PRESS_PAGE, index);
        SimpleHandler.getInstance().post(task);
    }

    private class NotifyTask implements Runnable {

        public static final int KEY_LAYOUT_MODE = 0;
        public static final int KEY_SIZE = 1;
        public static final int KEY_CURRENT_INDEX = 2;
        public static final int KEY_TAP_SLIDER_AREA = 3;
        public static final int KEY_TAP_MENU_AREA = 4;
        public static final int KEY_LONG_PRESS_PAGE = 5;

        private int mKey;
        private int mValue;

        public void setData(int key, int value) {
            mKey = key;
            mValue = value;
        }

        private void onTapMenuArea() {
            new AlertDialog.Builder(GalleryActivity.this)
                    .setTitle(R.string.gallery_menu_title).show();
        }

        private void onTapSliderArea() {
            if (mSliderPanel == null || mSize <= 0 || mCurrentIndex < 0) {
                return;
            }

            if (mSliderPanel.getVisibility() == View.VISIBLE) {
                mSliderPanel.animate().translationY(mSliderPanel.getHeight()).setDuration(150)
                        .setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR)
                        .setListener(mHideSliderListener).start();
            } else {
                mSliderPanel.setTranslationY(mSliderPanel.getHeight());
                mSliderPanel.setVisibility(View.VISIBLE);
                mSliderPanel.animate().translationY(0.0f).setDuration(150)
                        .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR)
                        .setListener(null).start();
                // Request layout ensure show it
                SimpleHandler.getInstance().post(mRequestLayoutSliderTask);
            }
        }

        private void onLongPressPage(final int index) {
            Resources resources = GalleryActivity.this.getResources();
            new AlertDialog.Builder(GalleryActivity.this)
                    .setTitle(resources.getString(R.string.page_menu_title, index + 1))
                    .setItems(R.array.page_menu_entries, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mGalleryProvider == null) {
                                return;
                            }

                            switch (which) {
                                case 0: // Refresh
                                    mGalleryProvider.forceRequest(index);
                                    break;
                                case 1: // Share
                                    // TODO
                                    break;
                                case 2: // Save
                                    // TODO
                                    break;
                                case 3: // Add a bookmark
                                    break;
                            }
                        }
                    }).show();
        }

        @Override
        public void run() {
            switch (mKey) {
                case KEY_LAYOUT_MODE:
                    GalleryActivity.this.mLayoutMode = mValue;
                    updateSlider();
                    break;
                case KEY_SIZE:
                    GalleryActivity.this.mSize = mValue;
                    updateSlider();
                    break;
                case KEY_CURRENT_INDEX:
                    GalleryActivity.this.mCurrentIndex = mValue;
                    updateSlider();
                    break;
                case KEY_TAP_MENU_AREA:
                    onTapMenuArea();
                    break;
                case KEY_TAP_SLIDER_AREA:
                    onTapSliderArea();
                    break;
                case KEY_LONG_PRESS_PAGE:
                    onLongPressPage(mValue);
                    break;
            }
            mNotifyTaskPool.push(this);
        }
    }

    private class GalleryAdapter extends GalleryView.Adapter {

        @Override
        public String getError() {
            if (mGalleryProvider == null) {
                return getString(R.string.error_no_provider);
            } else if (mGalleryProvider.size() <= GalleryProvider.STATE_ERROR) {
                return mGalleryProvider.getError();
            }
            return null;
        }

        @Override
        public int size() {
            if (mGalleryProvider == null) {
                return GalleryProvider.STATE_ERROR;
            } else {
                return mGalleryProvider.size();
            }
        }

        @Override
        public void onBind(GalleryPageView view, int index) {
            if (mGalleryProvider != null && mGalleryView != null) {
                mGalleryProvider.request(index);
                view.showInfo();
                view.setImage(null);
                view.setPage(index + 1);
                view.setProgress(GalleryPageView.PROGRESS_INDETERMINATE);
                view.setError(null, null);
            }
        }

        @Override
        public void onUnbind(GalleryPageView view) {
            view.setImage(null);
            view.setError(null, null);
        }
    }
}