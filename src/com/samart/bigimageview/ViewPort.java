package com.samart.bigimageview;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.concurrent.locks.ReentrantLock;

class ViewPort {


    private int inSample = 1;                                  // = 1/scaleFactor


    private int sViewW, sViewH;
    private final Rect lowResSrcRect = new Rect();
    private int viewW, viewH;
    private final Rect dstRect = new Rect();
    private int x, y;                                                  //center


    private final Rect hiResSrcRect = new Rect();
    private final Rect loadedRect = new Rect();

    private final Rect rectToLoad = new Rect();

    private final Rect mustBeLoaded = new Rect();
    private int w0, h0;                                                //originalSizes

    private final int availablePixels = (int) ((Runtime.getRuntime()
            .maxMemory() * 10) / 100 / 4);
    private float scale = 1;
    private static final Config BMP_CONFIG = Config.ALPHA_8;
    private Bitmap bitmap;
    private int loadedInSample = 1;
    private int centerX, centerY;
    private int fsviewW2, fsviewH2;
    private int lowResSample = 1;
    private int sAvailablePixels = availablePixels;
    private int totalPixels = 0;
    private int sViewWinSample;
    private int sViewHinSample;
    private int sViewWlowResSample;
    private int sViewHlowResSample;
    private final ReentrantLock rectLock = new ReentrantLock();

    public ViewPort(final int w, final int h) {
        viewW = w;
        viewH = h;

        dstRect.set(0, 0, w, h);

        recreateBitmap();
        updateScale();

    }

    private final Rect lowResDstRect = new Rect();

    public static int calcInSamplePow2(final float scale) {
        int inSampleTmp = (int) (Math.ceil(1f / scale));

        int countBits = 0;
        while (inSampleTmp > 1) {
            inSampleTmp = inSampleTmp >> 1;
            countBits++;
        }
        return 1 << countBits;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(final Bitmap bmp) {
        bitmap = bmp;
    }

    /**
     * warning !
     * it mutable
     * do not modify
     *
     * @return dstRect for canvas.drawBitmap(bmp,srcRect,dstRect,null);
     */
    public Rect getDstRect() {
        return dstRect;
    }

    public Rect getHiResSrcRect() {
        return hiResSrcRect;

    }

    public int getInSampleSize() {
        return inSample;
    }

    public Rect getLoadedRect() {
        return loadedRect;
    }

    public Rect getLowResDstRect() {
        return lowResDstRect;
    }

    public Rect getLowResSrcRect() {
        return lowResSrcRect;
    }

    public Rect getMustBeLoaded() {
        return mustBeLoaded;
    }

    public Rect getRectToLoad() {
        if (totalPixels <= sAvailablePixels) {
            rectToLoad.set(0, 0, w0, h0);
            return rectToLoad;
        }
        int left, top, right, bottom;
        final float r = (float) mustBeLoaded.height()
                / (float) mustBeLoaded.width();

        final int wmax2 = (int) (Math.sqrt(sAvailablePixels / r) / 2);
        final int hmax2 = (int) ((wmax2 * r) / 2);

        //      if ((wmax2 < w0) || (hmax2 < h0)) {
        //         rectToLoad.set(mustBeLoaded);
        //        return rectToLoad;
        //   }
        left = centerX - wmax2;
        top = centerY - hmax2;
        right = centerX + wmax2;
        bottom = centerY + hmax2;

        final int leftPadding = Math.max(-left, 0);
        final int topPadding = Math.max(-top, 0);
        final int rightPadding = Math.max(right - w0, 0);
        final int bottomPadding = Math.max(bottom - h0, 0);

        left -= rightPadding;
        top -= bottomPadding;
        right += leftPadding;
        bottom += topPadding;

        if (left < 0) {
            left = 0;
        } else if (left > mustBeLoaded.left) {
            left = mustBeLoaded.left;
        }
        if (top < 0) {
            top = 0;
        } else if (top > mustBeLoaded.top) {
            top = mustBeLoaded.top;
        }
        if (right > w0) {
            right = w0;
        } else if (right < mustBeLoaded.right) {
            right = mustBeLoaded.right;
        }

        if (bottom > h0) {
            bottom = h0;
        } else if (bottom < mustBeLoaded.bottom) {
            bottom = mustBeLoaded.bottom;
        }

        rectToLoad.set(left, top, right, bottom);

        return new Rect(rectToLoad);
    }

    public float getScaleFactor() {
        return scale;
    }

    /**
     * scales viewport
     */
    public synchronized void setScaleFactor(final float scale) {

        this.scale = scale;

        inSample = calcInSamplePow2(scale);

        sAvailablePixels = availablePixels * (inSample * inSample);

        fsviewW2 = (int) (viewW / scale / 2);
        fsviewH2 = (int) (viewH / scale / 2);

        sViewW = (int) (viewW / scale);
        sViewH = (int) (viewH / scale);

        sViewWlowResSample = (int) Math.ceil(viewW / scale / lowResSample) + 1;
        sViewHlowResSample = (int) Math.ceil(viewH / scale / lowResSample) + 1;

        sViewWinSample = (int) (viewW / scale / loadedInSample);
        sViewHinSample = (int) (viewH / scale / loadedInSample);

        x = centerX - fsviewW2;
        y = centerY - fsviewH2;

        calcRects();
    }

    public Point getTopLeftCorner() {
        return new Point(x, y);
    }

    public Point getViewCenterPosition() {
        return new Point(centerX, centerY);
    }

    public Point getViewLeftCorner() {
        return new Point((centerX - (sViewW / 2)), (centerY - (sViewH / 2)));
    }

    public Rect getViewRect() {
        return new Rect((centerX - (sViewW / 2)), (centerY - (sViewH / 2)),
                (centerX + (sViewW / 2)), (centerY + (sViewH / 2)));
    }

    public Point getViewSizes() {
        return new Point(viewW, viewH);
    }

    public synchronized boolean isNeedReload() {
        return !loadedRect.contains(mustBeLoaded)
                || (loadedInSample != inSample);
    }

    public void lockRects() {
        rectLock.lock();
    }

    public void recreateBitmap() {
        recycle();
        bitmap = Bitmap.createBitmap(1, 1, BMP_CONFIG);
    }

    public void recycle() {

        if ((null != bitmap) && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * loadedInSample used to know viewport what image was loaded
     * if loadedInSample != inSample then isLoaded = false
     *
     * @param inSampleSize coeff for options inSampleSize
     */

    public synchronized void setLoadedRect(final Rect rect,
                                           final int inSampleSize) {
        loadedRect.set(rect);
        loadedInSample = inSampleSize;
        updateScale();
    }

    public void setLowResPow2(final int lowResPow2) {
        lowResSample = 1 << lowResPow2;

    }

    /**
     * set original bitmap sizes
     * <p/>
     * it is not scaled hi resolution sizes
     */
    public void setOriginalSizes(final int w, final int h) {
        w0 = w;
        h0 = h;

        totalPixels = w * h;
        calcRects();
    }

    public void setTopLeftCorner(final int x, final int y) {
        this.x = x;
        this.y = y;
        updateScale();
    }

    /**
     * moves viewport
     *
     * @param mx coordinate in original size system
     * @param my coordinate
     */
    public synchronized void setViewCenterPosition(final int mx, final int my) {
        x = mx - fsviewW2;
        y = my - fsviewH2;

        centerX = mx;
        centerY = my;
        calcRects();

    }

    public void setViewSizes(final int w, final int h) {
        viewW = w;
        viewH = h;
        dstRect.set(0, 0, w, h);
        recreateBitmap();
        updateScale();

    }

    public void unlockRects() {
        rectLock.unlock();
    }

    private synchronized void calcRects() {

        //     lockRects();
        int left, top, right, bottom;

        left = Math.max(x, 0);
        top = Math.max(y, 0);
        right = Math.min(x + sViewW, w0);
        bottom = Math.min(y + sViewH, h0);

        mustBeLoaded.set(left, top, right, bottom);

        left = (x / lowResSample);
        top = (y / lowResSample);
        right = left + sViewWlowResSample;
        bottom = top + sViewHlowResSample;

        lowResSrcRect.set(left, top, right, bottom);

        left = (int) (((lowResSrcRect.left * lowResSample) - x) * scale);
        top = (int) (((lowResSrcRect.top * lowResSample) - y) * scale);
        right = left + (int) ((lowResSample * sViewWlowResSample) * scale);
        bottom = top + (int) ((lowResSample * sViewHlowResSample) * scale);
        lowResDstRect.set(left, top, right, bottom);

        left = (x < 0 ? ((x + mustBeLoaded.left) - loadedRect.left)
                / loadedInSample : (mustBeLoaded.left - loadedRect.left)
                / loadedInSample);
        top = (y < 0 ? ((y + mustBeLoaded.top) - loadedRect.top)
                / loadedInSample : (mustBeLoaded.top - loadedRect.top)
                / loadedInSample);
        right = left + sViewWinSample;
        bottom = top + sViewHinSample;

        hiResSrcRect.set(left, top, right, bottom);

        //    unlockRects();
    }

    private void updateScale() {
        setScaleFactor(scale);
    }
}