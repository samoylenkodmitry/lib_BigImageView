package com.samart.bigimageview;

class CacheBitmap {
    private final BitmapRenderer bitmapSurfaceRenderer;
    /**
     * The cache bitmap loading thread
     */
    private CacheBitmapThread mCacheBitmapThread;

    /** The currently cached bitmap */
    /**
     * The current state of the cache
     */
    private CacheState mCacheState = CacheState.NOT_INITIALIZED;

    public CacheBitmap(final BitmapRenderer bitmapSurfaceRenderer) {
        this.bitmapSurfaceRenderer = bitmapSurfaceRenderer;
    }

    /**
     * call thread to wake up if it sleep
     * and to go to update loop
     */
    public void callUpdateBitmap() {
        if (CacheState.DISABLED != mCacheState) {
            setCacheState(CacheState.BEGIN_UPDATE);
        }
    }

    public CacheState getCacheState() {
        return mCacheState;
    }

    public void setCacheState(final CacheState newState) {
        if (null != mCacheBitmapThread) {
            mCacheState = newState;
            if (CacheState.BEGIN_UPDATE == newState) {
                mCacheBitmapThread.beginUpdate();
            }
        }
    }

    public boolean isInitialized() {
        synchronized (this) {
            return CacheState.NOT_INITIALIZED != mCacheState;
        }
    }

    public void start() {
        if (null != mCacheBitmapThread) {
            stop();
        }
        mCacheBitmapThread = new CacheBitmapThread(this);
        mCacheBitmapThread.start();
    }

    public void stop() {
        if (null != mCacheBitmapThread) {
            mCacheBitmapThread.done();
            mCacheBitmapThread.interrupt();
            while (mCacheBitmapThread.isAlive()) {
                try {
                    mCacheBitmapThread.join();
                } catch (final InterruptedException ignored) {
                }
            }
        }
        mCacheBitmapThread = null;

    }

    public void suspend(final boolean suspend) {
        // Suspends or resume the cache thread.
        synchronized (this) {
            if (suspend) {
                setCacheState(CacheState.DISABLED);
            } else {
                if (CacheState.DISABLED == mCacheState) {
                    setCacheState(CacheState.BEGIN_UPDATE);
                }
            }
        }
    }

    /**
     * called directly from cacheBitmapThread
     */
    public void update() {
        bitmapSurfaceRenderer.loadCachedBitmap();
    }

    /**
     * The current state of the cached bitmap
     */
    enum CacheState {
        BEGIN_UPDATE, DISABLED, IS_INITIALIZED, IS_UPDATING, NOT_INITIALIZED, READY
    }

}
