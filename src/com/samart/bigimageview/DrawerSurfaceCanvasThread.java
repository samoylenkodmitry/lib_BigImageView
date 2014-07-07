package com.samart.bigimageview;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

class DrawerSurfaceCanvasThread extends Thread {
    private final SurfaceHolder mSurfaceHolder;
    private final BitmapRenderer surfaceRenderer;
    private boolean isChanged;
    private boolean isRendererDone;
    private boolean isViewHasFocus;
    private boolean isWaitingForChanges;
    private boolean isWaitingForFocus;

    public DrawerSurfaceCanvasThread(final BitmapRenderer surfaceRenderer,
                                     final SurfaceHolder surfaceHolder) {
        super();
        setName(getClass().getSimpleName());
        mSurfaceHolder = surfaceHolder;
        this.surfaceRenderer = surfaceRenderer;
    }

    public void changeFocus(final boolean hasFocus) {
        synchronized (this) {
            isViewHasFocus = hasFocus;
            if (isWaitingForFocus) {
                notifyAll();
            }
        }
    }

    @Override
    public void run() {
        Canvas canvas = null;
        while (!isRendererDone) {
            waitForFocus();
            waitForChanges();
            synchronized (this) {
                if (isRendererDone) {
                    break;
                }
                try {
                    canvas = mSurfaceHolder.lockCanvas();
                    if (null != canvas) {
                        isChanged = false;
                        surfaceRenderer.renderCanvas(canvas);
                    }
                } finally {
                    if (null != canvas) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
        Utils.log("exit drawer thread");
    }

    public void setChanged() {
        synchronized (this) {
            isChanged = true;
            if (isWaitingForChanges) {
                notifyAll();
            }
        }
    }

    public void surfaceDestroyed() {
        synchronized (this) {
            isRendererDone = true;
            isViewHasFocus = true;
            isChanged = true;
            notifyAll();
            interrupt();
        }
    }

    private void waitForChanges() {
        synchronized (this) {
            if (!isChanged) {
                isWaitingForChanges = true;
                while (!isChanged) {
                    try {
                        wait();
                    } catch (final InterruptedException ignored) {
                    }
                }
                isWaitingForChanges = false;
            }
        }
    }

    private void waitForFocus() {
        synchronized (this) {
            if (!isViewHasFocus) {
                isWaitingForFocus = true;
                while (!isViewHasFocus) {
                    try {
                        wait();
                    } catch (final InterruptedException ignored) {
                    }

                }
                isWaitingForFocus = false;
            }
        }
    }

}