package com.samart.bigimageview;

import android.content.Context;
import android.graphics.BitmapRegionDecoder;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;

public class BigSurfaceView extends android.view.SurfaceView implements
        SurfaceHolder.Callback {

    private final BitmapRenderer mSurfaceRenderer = new BitmapRenderer();
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private TouchHandler mTouchHandler;

    {
        // Allow focus
        setFocusable(true);
    }

    private boolean isDestroyed = false;
    private boolean isAlreadyCreated;
    private File mFile;
    private boolean isNeedToSetFile;

    public BigSurfaceView(final Context context) {
        super(context);
        init(context);

    }

    public BigSurfaceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BigSurfaceView(final Context context, final AttributeSet attrs,
                          final int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public BitmapRegionDecoder getBitmapDecoder() {
        return mSurfaceRenderer.getDecoder();
    }

    public File getFile() {
        return mSurfaceRenderer.getFile();
    }

    /**
     * return selection rect if selection enabled, null otherwise
     *
     * @return null if surface renderer is null
     */
    public Rect getSelectionRect() {
        Rect rect = null;
        if (mSurfaceRenderer.isSelectionEnabled()) {
            rect = mSurfaceRenderer.getSelectionRect();
        }
        return rect;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        final boolean consumed = mGestureDetector.onTouchEvent(event);
        if (consumed && (MotionEvent.ACTION_UP != action)) {
            return true;
        }
        mScaleGestureDetector.onTouchEvent(event);

        return mTouchHandler.handleTouchEvent(event,
                mScaleGestureDetector.isInProgress());
    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Utils.log("change focus " + Boolean.toString(hasFocus));
        mSurfaceRenderer.changeFocus(hasFocus);
        if (isDestroyed && hasFocus) {
            if (null != mTouchHandler) {
                mTouchHandler.start();
            }

            try {
                Utils.log("resume");
                mSurfaceRenderer.resume();
            } catch (final IOException e) {
                Utils.log("could not resume renderer" + e.getMessage());
            }
            isDestroyed = false;
        }

    }

    public BitmapRegionDecoder pauseAndGetDecoder() {
        Utils.log("pause and get decoder");
        return mSurfaceRenderer.pauseAndGetDecoder();

    }

    public void resumeFromPause() {
        Utils.log("resume from pause");
        mSurfaceRenderer.resumeFromPause();

    }

    public void scaleToSelection() {
        mSurfaceRenderer.scaleToSelection();
    }

    public void setBitmapFile(final File file) throws IOException {

        synchronized (this) {

            mFile = file;
            if (!isAlreadyCreated) {
                isNeedToSetFile = true;
            } else {
                Utils.log("setBitmapFile");
                mSurfaceRenderer.setBitmap(file);
            }
        }
    }

    public void setSelectEnabled(final boolean enabled) {
        mSurfaceRenderer.setSelectEnabled(enabled);
    }

    public void setSelectionRequestedSizes(final int requestedWidth,
                                           final int requestedHeight) {
        mSurfaceRenderer.setSelectionSizes(requestedWidth, requestedHeight);
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,
                               final int w, final int h) {

        synchronized (this) {

            Utils.log("surfaceChanged ");
            mSurfaceRenderer.setViewSizes(w, h);
            if (!isAlreadyCreated) {
                isAlreadyCreated = true;
                if (isNeedToSetFile) {
                    try {
                        mSurfaceRenderer.setBitmap(mFile);
                        isNeedToSetFile = false;
                    } catch (final IOException e) {
                        Utils.log("set bitmap error" + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        mSurfaceRenderer.setSurfaceHolder(holder);
        mTouchHandler.start();
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        Utils.log("surfaceDestroyed");
        mTouchHandler.stop();
        mSurfaceRenderer.stop();

        isDestroyed = true;
    }

    private void init(final Context context) {

        if (isInEditMode()) {
            return;
        }
        getHolder().setFormat(PixelFormat.RGB_565);
        // Set SurfaceHolder callback
        getHolder().addCallback(this);
        mTouchHandler = new TouchHandler(this, context, mSurfaceRenderer);
        TouchEventListener mTouchEventListener = new TouchEventListener(this, mSurfaceRenderer,
                mTouchHandler);
        mGestureDetector = new GestureDetector(context, mTouchEventListener);
        mScaleGestureDetector = new ScaleGestureDetector(context,
                mTouchEventListener);

    }

}
