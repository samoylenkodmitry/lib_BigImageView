package com.samart.bigimageview;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.widget.Scroller;

enum TouchState {
    IN_TOUCH, NO_TOUCH, ON_FLING
}

class TouchHandler {
    private static final String TAG = TouchHandler.class
            .getSimpleName();
    private final BigSurfaceView bigSurfaceView;
    private final BitmapRenderer mBitmapSurfaceRenderer;
    // Scroller
    private final Scroller mScroller;
    // Point initially touched
    private final Point touchDown = new Point(0, 0);
    // View Center onTouchDown
    private final Point viewCenterAtDown = new Point(0, 0);
    private final PointF lastMove = new PointF();
    // Current Touch State
    private TouchState currState = TouchState.NO_TOUCH;
    // Thread for handling
    private FlingComputeThread mFlingComputeThread;

    public TouchHandler(final BigSurfaceView bigSurfaceView,
                        final Context context, final BitmapRenderer r) {
        this.bigSurfaceView = bigSurfaceView;
        mScroller = new Scroller(context);
        mBitmapSurfaceRenderer = r;
    }

    public boolean fling(final float velocityX, final float velocityY) {
        if (mBitmapSurfaceRenderer.isSelectionConsumed()) {
            return false;
        }
        if (mScroller.isFinished()) {
            final Point viewCenter = mBitmapSurfaceRenderer
                    .getViewCenterPosition();

            final Point originalSize = mBitmapSurfaceRenderer
                    .getOriginalSizes();

            final int startX = viewCenter.x;
            final int startY = viewCenter.y;

            final int minX = 0;
            final int minY = 0;
            final int maxX = originalSize.x;
            final int maxY = originalSize.y;

            mScroller.fling(startX, startY, (int) ((-1.5 * velocityX)),
                    (int) ((-1.5 * velocityY)), minX, maxX, minY, maxY);
            setState(TouchState.ON_FLING);
        } else {
            mScroller.forceFinished(true);
        }

        return true;
    }

    public TouchState getState() {
        return currState;
    }

    public void setState(final TouchState mTouchState) {
        currState = mTouchState;
        switch (mTouchState) {

            case ON_FLING:
                mFlingComputeThread.beginFlingCalc();
                break;
            case IN_TOUCH:
            case NO_TOUCH:
                break;
        }
    }

    public boolean handleTouchEvent(final MotionEvent event,
                                    final boolean isScale) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return eventDown(event);
            case MotionEvent.ACTION_MOVE:
                if (!isScale) {
                    return eventMove(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                return eventUp();
            case MotionEvent.ACTION_CANCEL:
                return eventCancel();
        }
        return false;
    }

    public void start() {
        if (null == mBitmapSurfaceRenderer) {
            throw new RuntimeException(TAG
                    + " start null SurfaceRenderer. Call setRenderer first");
        }
        if (null == mFlingComputeThread) {
            mFlingComputeThread = new FlingComputeThread(
                    mBitmapSurfaceRenderer, mScroller, this);
            mFlingComputeThread.start();
        }
    }

    public void stop() {
        mFlingComputeThread.done();
        mFlingComputeThread.interrupt();
        while (mFlingComputeThread.isAlive()) {
            try {
                mFlingComputeThread.join();
            } catch (final InterruptedException e) {
                // Wait until done
            }
        }
        mFlingComputeThread = null;
    }

    /**
     * Handle a cancel event
     */
    private boolean eventCancel() {
        if (TouchState.IN_TOUCH == currState) {
            setState(TouchState.NO_TOUCH);
        }
        return true;
    }

    /**
     * Handle a down event
     */
    private boolean eventDown(final MotionEvent event) {
        if (null == mBitmapSurfaceRenderer) {
            return false;
        }
        // Cancel rendering suspension
        mBitmapSurfaceRenderer.suspend(false);
        // Get position
        setState(TouchState.IN_TOUCH);
        final Point p = mBitmapSurfaceRenderer.getViewCenterPosition();
        touchDown.x = (int) event.getX();
        touchDown.y = (int) event.getY();
        lastMove.x = touchDown.x;
        lastMove.y = touchDown.y;
        viewCenterAtDown.x = p.x;
        viewCenterAtDown.y = p.y;
        if (mBitmapSurfaceRenderer.isSelectionEnabled()) {
            mBitmapSurfaceRenderer.selectionTouchDown(touchDown);
        }
        return true;
    }

    /**
     * Handle a move event
     */
    private boolean eventMove(final MotionEvent event) {
        if (null == mBitmapSurfaceRenderer) {
            return false;
        }
        if ((TouchState.ON_FLING == getState())
                || (TouchState.NO_TOUCH == getState())) {
            setState(TouchState.IN_TOUCH);
        }
        if (TouchState.IN_TOUCH == currState) {
            final int deltaX = (int) ((event.getX() - touchDown.x) / mBitmapSurfaceRenderer
                    .getScaleFactor());
            final int deltaY = (int) ((event.getY() - touchDown.y) / mBitmapSurfaceRenderer
                    .getScaleFactor());
            if (mBitmapSurfaceRenderer.isSelectionConsumed()) {
                final float dx = (event.getX() - lastMove.x);
                final float dy = (event.getY() - lastMove.y);
                lastMove.x = event.getX();
                lastMove.y = event.getY();
                mBitmapSurfaceRenderer.moveSelection(dx, dy);
            } else {
                mBitmapSurfaceRenderer.setViewPosition(viewCenterAtDown.x
                        - deltaX, viewCenterAtDown.y - deltaY);
            }
            bigSurfaceView.invalidate();
            return true;
        }
        return false;
    }

    /**
     * Handle an up event
     */
    private boolean eventUp() {
        if (TouchState.IN_TOUCH == currState) {
            setState(TouchState.NO_TOUCH);
        }

        if (mBitmapSurfaceRenderer.isSelectionEnabled()) {
            mBitmapSurfaceRenderer.selectionTouchUp();
        }
        return true;
    }

}