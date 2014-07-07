package com.samart.bigimageview;

import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.view.SurfaceHolder;
import com.samart.bigimageview.CacheBitmap.CacheState;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

class BitmapRenderer {
    private static final Config LR_CONFIG = Config.RGB_565;
    private static final int LOW_RES_MEMORY_PERCENT = 1;
    private static final Config HR_CONFIG = Config.RGB_565;
    final int pixelsAvailable = (int) ((Runtime
            .getRuntime()
            .maxMemory() * LOW_RES_MEMORY_PERCENT) / 100 / 2);
    private final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    private final CacheBitmap mCacheBitmap = new CacheBitmap(
            this);
    private final ViewPort mViewPort = new ViewPort(
            1, 1);
    public ReentrantLock lock = new ReentrantLock();
    private Bitmap lowResBitmap;
    private int lowResSampleSize = 2;
    private float minScaleFactor = 1.0f;
    private BitmapRegionDecoder mRegionDecoder;
    private Point originalSizes = new Point();
    private DrawerSurfaceCanvasThread drawerSurfaceThread;
    private boolean selectionEnabled;
    private SelectionRect mSelectionRect;
    private float selectionRatio = -100;
    private File file;
    private SurfaceHolder surfaceHolder;
    private boolean hasFocus;
    private int requestedHeight;

    public BitmapRenderer() {
        mBitmapOptions.inPreferredConfig = HR_CONFIG;
        mBitmapOptions.inDither = true;

        Paint paint = new Paint();
        paint.setTextSize(14f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.CYAN);

    }

    public void changeFocus(final boolean hasFocus) {
        this.hasFocus = hasFocus;
        if (null != drawerSurfaceThread) {
            Utils.log("changing focus " + hasFocus);
            drawerSurfaceThread.changeFocus(hasFocus);
            drawerSurfaceThread.setChanged();
        }
    }

    public void changeScaleFactor(final float multiplier) {
        setScaleFactor(multiplier * mViewPort.getScaleFactor());
    }

    public BitmapRegionDecoder getDecoder() {
        return mRegionDecoder;
    }

    public File getFile() {
        return file;
    }

    public Point getOriginalSizes() {

        return new Point(originalSizes);
    }

    public float getScaleFactor() {
        return mViewPort.getScaleFactor();
    }

    private void setScaleFactor(final float s) {
        mViewPort.setScaleFactor(s);
        onViewRectChange();
    }

    public Rect getSelectionRect() {
        final Rect viewRect = mViewPort.getViewRect();
        final float scale = mViewPort.getScaleFactor();
        final RectF selectionRect = mSelectionRect.getSelectionRect();
        final int left = (int) ((selectionRect.left / scale) + viewRect.left);
        final int top = (int) ((selectionRect.top / scale) + viewRect.top);
        final int right = (int) (left + (selectionRect.width() / scale));
        final int bottom = (int) (top + (selectionRect.height() / scale));
        return new Rect(left, top, right, bottom);
    }

    public Point getViewCenterPosition() {
        return mViewPort.getViewCenterPosition();
    }

    public ViewPort getViewPort() {
        return mViewPort;
    }

    public boolean isSelectionConsumed() {
        return null != mSelectionRect && mSelectionRect.isSelected();
    }

    public boolean isSelectionEnabled() {
        return selectionEnabled;
    }

    /**
     * Loads the relevant slice of the background bitmap that needs to be kept
     * in memory.
     * <p/>
     * The loading can take a long time depending on the size.
     */
    public void loadCachedBitmap() {
        if (!mViewPort.isNeedReload()) {
            return;
        }
        mBitmapOptions.inSampleSize = mViewPort.getInSampleSize();
        try {
            final Rect rectToLoad = mViewPort.getRectToLoad();
            final Bitmap bmp = mRegionDecoder.decodeRegion(rectToLoad,
                    mBitmapOptions);
            if (null != bmp) {
                lock.lock();
                try {
                    mViewPort.recycle();
                    mViewPort.setBitmap(bmp);
                    mViewPort.setLoadedRect(rectToLoad,
                            mBitmapOptions.inSampleSize);
                } finally {
                    lock.unlock();
                }
                drawerSurfaceThread.setChanged();
            }
        } catch (final Exception ignored) {
        }
    }

    public void moveSelection(final float dx, final float dy) {
        mSelectionRect.moveBy(dx, dy);
        drawerSurfaceThread.setChanged();
    }

    public BitmapRegionDecoder pauseAndGetDecoder() {
        synchronized (this) {
            mCacheBitmap.stop();
            drawerSurfaceThread.surfaceDestroyed();
            while (drawerSurfaceThread.isAlive()) {
                try {
                    drawerSurfaceThread.join();
                } catch (final InterruptedException e) {
                    // Repeat until success
                }
            }
            mViewPort.recycle();
            if ((null != lowResBitmap) && !lowResBitmap.isRecycled()) {
                lowResBitmap.recycle();
            }
            return mRegionDecoder;
        }
    }

    /**
     * godlike void
     * <p/>
     * called thousand times in second
     *
     * @param c canvas
     */
    public void renderCanvas(final Canvas c) {
        //  mViewPort.lockRects();
        c.drawARGB(255, 55, 55, 55);
        c.drawBitmap(lowResBitmap, mViewPort.getLowResSrcRect(),
                mViewPort.getLowResDstRect(), null);

        if (lock.tryLock()) {
            try {
                c.drawBitmap(mViewPort.getBitmap(),
                        mViewPort.getHiResSrcRect(), mViewPort.getDstRect(),
                        null);
            } finally {
                lock.unlock();
            }
       }
     //        c.drawText("hires" + mViewPort.getHiResSrcRect().flattenToString(), 10,
     //               100, paint);
     //      c.drawText("lowres" + mViewPort.getLowResSrcRect().flattenToString(),
     //             10, 180, paint);
     //    c.drawText("toLoad" + mViewPort.getRectToLoad().flattenToString(), 10,
     //           120, paint);
     //  c.drawText("loaded" + mViewPort.getLoadedRect().flattenToString(), 10,
     //                150, paint);
     //       c.drawText("dst rect" + mViewPort.getLowResDstRect().flattenToString(),
     //              10, 190, paint);
     //
     //       c.drawText(
     //              "mustloaded" + mViewPort.getMustBeLoaded().flattenToString(),
     //             10, 170, paint);
     //
     //  c.drawText(Integer.toString(mViewPort.getInSampleSize()), 10, 140,
     //         paint);
     //
     //  c.drawText(Float.toString(mViewPort.getScaleFactor()), 10, 160, paint);
     // c.drawText(Float.toString(lowResSampleSize), 10, 190, paint);

        if (isSelectionEnabled()) {
            //          c.drawText(getSelectionRect().toString(), 10, 100, paint);
            mSelectionRect.draw(c);
        }
        // mViewPort.unlockRects();
    }

    public void resume() throws IOException {
        synchronized (this) {
            if (null != file) {
                lock.lock();
                mViewPort.recreateBitmap();
                setBitmap(file);
                lock.unlock();
                drawerSurfaceThread.setChanged();
            }
        }
    }

    public void resumeFromPause() {
        synchronized (this) {
            if ((null == mRegionDecoder) || mRegionDecoder.isRecycled()) {
                Utils.log("null or recycled bitmap region decoder");
                return;
            }
            createLowResBitmap(file);
            mCacheBitmap.start();
            lock.lock();
            mViewPort.recreateBitmap();
            lock.unlock();
            startRendererThread();
            onViewRectChange();
        }
    }

    public void scaleToSelection() {
        final RectF selection = mSelectionRect.getSelectionRect();
        final float ratio = selection.height() / requestedHeight;
        setScaleFactor(ratio);
    }

    public void selectionTouchDown(final Point p) {
        mSelectionRect.touchDown(p);
    }

    public void selectionTouchUp() {
        mSelectionRect.touchUp();
    }

    public void setBitmap(final File file) throws IOException {
        synchronized (this) {
            this.file = file;
            readOriginalSizes(file);
            calcMinScaleFactor();
            calcLowResSampleSize();
            createLowResBitmap(file);
            mRegionDecoder = BitmapRegionDecoder.newInstance(
                    file.getAbsolutePath(), false);
            setScaleFactor(minScaleFactor);
            centerViewPosition();
            mCacheBitmap.setCacheState(CacheState.IS_INITIALIZED);
            mCacheBitmap.start();
            startRendererThread();
        }
    }

    public void setSelectEnabled(final boolean enabled) {
        selectionEnabled = enabled;
        drawerSurfaceThread.setChanged();
    }

    public void setSelectionSizes(final int requestedWidth,
                                  final int requestedHeight) {
        this.requestedHeight = requestedHeight;
        selectionRatio = (float) requestedHeight / (float) requestedWidth;
        if (null != mSelectionRect) {
            mSelectionRect.setSelectionRatio(selectionRatio);
        }

    }

    public void setSurfaceHolder(final SurfaceHolder holder) {
        surfaceHolder = holder;
        recreateDrawerSurfaceCanvasThread();
    }

    /**
     * Set the position (center) of the view
     */
    public void setViewPosition(final int x, final int y) {
        this.setViewPosition(x, y, true);
    }

    /**
     * Set the dimensions of the view
     */
    public void setViewSizes(final int w, final int h) {
        mViewPort.setViewSizes(w, h);
        calcMinScaleFactor();
        setScaleFactor(minScaleFactor);
        centerViewPosition();
        mSelectionRect = new SelectionRect(w, h, selectionRatio);
    }

    /**
     * Stops the renderer
     */
    public void stop() {
        synchronized (this) {
            mCacheBitmap.stop();
            if (null != drawerSurfaceThread) {
                Utils.log("stopping renderer thread");
                drawerSurfaceThread.surfaceDestroyed();
                while (drawerSurfaceThread.isAlive()) {
                    try {
                        drawerSurfaceThread.join();
                    } catch (final InterruptedException e) {
                        // Repeat until success
                    }
                }
            }
            mViewPort.recycle();
            if ((null != lowResBitmap) && !lowResBitmap.isRecycled()) {
                lowResBitmap.recycle();
            }
            if (null != mRegionDecoder) {
                mRegionDecoder.recycle();
            }
        }
    }

    /**
     * Suspend the renderer
     */
    public void suspend(final boolean suspend) {
        mCacheBitmap.suspend(suspend);
    }

    public void switchScale(final float tapX, final float tapY) {
        final float newScale;
        if (1f == mViewPort.getScaleFactor()) {
            newScale = minScaleFactor;
            centerViewPosition(false);
        } else {
            newScale = 1f;
            final Rect viewRect = mViewPort.getViewRect();
            final float scale = mViewPort.getScaleFactor();
            final int x = (int) ((tapX / scale) + viewRect.left);
            final int y = (int) ((tapY / scale) + viewRect.top);
            setViewPosition(x, y, false);
        }
        setScaleFactor(newScale);
    }

    private void setViewPosition(int x, int y, boolean needRedraw) {
        mViewPort.setViewCenterPosition(x, y);
        if (needRedraw) onViewRectChange();
    }

    private void centerViewPosition(boolean needRedraw) {
        final int x = originalSizes.x / 2;
        final int y = originalSizes.y / 2;
        setViewPosition(x, y, needRedraw);
    }

    private void calcLowResSampleSize() {
        lowResSampleSize = (int) Math.sqrt((originalSizes.x * originalSizes.y)
                / pixelsAvailable);
        int lowResPow2 = -1;
        while (lowResSampleSize > 0) {
            lowResSampleSize = lowResSampleSize >> 1;
            lowResPow2++;
        }
        lowResSampleSize = 1 << lowResPow2;
        mViewPort.setLowResPow2(lowResPow2);
    }

    private void calcMinScaleFactor() {
        final Point screenSizes = mViewPort.getViewSizes();
        final int screenHeight = screenSizes.y;
        final int screenWidth = screenSizes.x;
        final float factorWidth = screenWidth / (float) originalSizes.x;
        final float factorHeight = screenHeight / (float) originalSizes.y;
        minScaleFactor = Math.min(factorWidth, factorHeight);
    }

    private void centerViewPosition() {
        this.centerViewPosition(true);
    }

    private void createLowResBitmap(final File file) {
        final Options opt = new Options();
        opt.inSampleSize = lowResSampleSize;
        opt.inDither = true;
        opt.inPreferredConfig = LR_CONFIG;
        lowResBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
        drawerSurfaceThread.setChanged();
    }

    /**
     * this super void must be called on every move and scale of image
     * to call thread to check bounds and reload
     */
    private void onViewRectChange() {
        //call CacheBitmapThread
        mCacheBitmap.callUpdateBitmap();
        drawerSurfaceThread.setChanged();
    }

    private void readOriginalSizes(final File file) {
        final BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = HR_CONFIG;
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), opt);
        originalSizes = new Point(opt.outWidth, opt.outHeight);
        mViewPort.setOriginalSizes(opt.outWidth, opt.outHeight);
        mViewPort.setViewCenterPosition(opt.outWidth >> 1, opt.outHeight >> 1);
    }

    private void recreateDrawerSurfaceCanvasThread() {
        if (null != drawerSurfaceThread) {
            drawerSurfaceThread.surfaceDestroyed();
            while (drawerSurfaceThread.isAlive()) {
                try {
                    drawerSurfaceThread.join();
                } catch (final InterruptedException ignored) {
                }
            }
        }
        drawerSurfaceThread = new DrawerSurfaceCanvasThread(this, surfaceHolder);
        drawerSurfaceThread.changeFocus(hasFocus);
    }

    private void startRendererThread() {
        recreateDrawerSurfaceCanvasThread();
        drawerSurfaceThread.start();
    }
}
