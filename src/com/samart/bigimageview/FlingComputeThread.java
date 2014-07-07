package com.samart.bigimageview;

import android.widget.Scroller;

/**
 * Touch Handler Thread
 */
class FlingComputeThread extends Thread {
    private final BitmapRenderer renderer;
    private final Scroller scroller;
    private final TouchHandler touchHandler;
    private boolean isDone = false;
    private boolean isWaiting = false;

    public FlingComputeThread(final BitmapRenderer mBitmapSurfaceRenderer,
                              final Scroller scroller, final TouchHandler touchHandler) {
        super();
        this.touchHandler = touchHandler;
        this.scroller = scroller;
        renderer = mBitmapSurfaceRenderer;
        setName(getClass().getSimpleName());
    }

    public void beginFlingCalc() {
        synchronized (this) {
            if (isWaiting) {
                notifyAll();
            }
        }
    }

    public void done() {
        synchronized (this) {
            isDone = true;
            if (isWaiting) {
                notifyAll();
            }
        }
    }

    @Override
    public void run() {
        while (!isDone) {
            waitForFling();
            // fling!
            beginFling();
            while (!(isDone || scroller.isFinished() || (TouchState.ON_FLING != touchHandler
                    .getState()))) {
                scroller.computeScrollOffset();
                renderer.setViewPosition(scroller.getCurrX(),
                        scroller.getCurrY());

            }
            endFling();
        }
    }

    private void beginFling() {
        renderer.suspend(true);
    }

    private void endFling() {
        renderer.suspend(false);
        touchHandler.setState(TouchState.NO_TOUCH);
    }

    private void waitForFling() {
        synchronized (this) {
            while ((TouchState.ON_FLING != touchHandler.getState()) && !isDone) {
                try {
                    scroller.forceFinished(true);
                    isWaiting = true;
                    wait();
                } catch (final InterruptedException ignored) {
                }
            }
        }
        isWaiting = false;
    }
}