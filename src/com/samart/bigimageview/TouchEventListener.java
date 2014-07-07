package com.samart.bigimageview;

import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

class TouchEventListener implements OnGestureListener, OnDoubleTapListener,
        OnScaleGestureListener {
    private final BigSurfaceView mBigSurfaceView;
    private final BitmapRenderer mBitmapRenderer;
    private final TouchHandler mTouchHandler;

    public TouchEventListener(final BigSurfaceView bigSurfaceView,
                              final BitmapRenderer bitmapRenderer, final TouchHandler touchHandler) {
        mBigSurfaceView = bigSurfaceView;
        mBitmapRenderer = bitmapRenderer;
        mTouchHandler = touchHandler;
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        mBitmapRenderer.switchScale(e.getX(), e.getY());
        mBigSurfaceView.invalidate();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        return mTouchHandler.fling(velocityX, velocityY);
    }

    @Override
    public void onLongPress(final MotionEvent e) {
    }

    @Override
    public boolean onScale(final ScaleGestureDetector detector) {
        mBitmapRenderer.changeScaleFactor(detector.getScaleFactor());
        mBigSurfaceView.invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(final ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(final ScaleGestureDetector detector) {

    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(final MotionEvent e) {
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        return false;
    }

}
