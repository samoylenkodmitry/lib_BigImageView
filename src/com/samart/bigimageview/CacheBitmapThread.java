package com.samart.bigimageview;

import com.samart.bigimageview.CacheBitmap.CacheState;

class CacheBitmapThread extends Thread {
    private final CacheBitmap mCacheBitmap;
    private boolean isDone = false;
    private boolean isNeedUpdate = true;
    private boolean isWaitForBeginUpdate = false;

    public CacheBitmapThread(
            final CacheBitmap cache) {
        setName(CacheBitmapThread.class.getSimpleName());
        mCacheBitmap = cache;
    }

    public void beginUpdate() {

        synchronized (this) {
            isNeedUpdate = true;
            if (isWaitForBeginUpdate) {
                notifyAll();
            }
        }
    }

    public void done() {
        isDone = true;
        beginUpdate();
    }

    @Override
    public void run() {
        while (!isDone) {
            waitForBeginUpdateState();
            if (isDone) {
                return;
            }
            try {
                mCacheBitmap.setCacheState(CacheState.IS_UPDATING);
                mCacheBitmap.update();
                mCacheBitmap.setCacheState(CacheState.READY);
            } catch (final OutOfMemoryError e) {
                // Out of memory error detected
                if (mCacheBitmap.getCacheState() == CacheState.IS_UPDATING) {
                    mCacheBitmap.setCacheState(CacheState.BEGIN_UPDATE);
                }
            }
        }
    }

    private void waitForBeginUpdateState() {
        synchronized (this) {
            while (!isDone && !isNeedUpdate) {
                isWaitForBeginUpdate = true;

                try {
                    this.wait();
                } catch (final InterruptedException ignored) {
                }
            }
            isWaitForBeginUpdate = false;
            isNeedUpdate = false;
        }
    }

}