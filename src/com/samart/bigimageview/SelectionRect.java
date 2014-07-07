package com.samart.bigimageview;

import android.graphics.*;

import java.util.ArrayList;
import java.util.List;

class SelectionRect {

    private static final int MARGIN = 10;
    private static final int RAD = 60;
    private final FrameControl main;
    private final RectF maxBounds = new RectF();
    private boolean isSelected;
    private FrameControl selectedControl;
    private float selectionRatio;

    public SelectionRect(final int pw, final int ph, final float ratio) {
        final int w = pw > 0 ? pw : MARGIN;
        final int h = ph > 0 ? ph : MARGIN;
        maxBounds.set(0, 0, w, h);
        selectionRatio = ratio;
        final float screenRatio2 = h > w ? (2 * h) / w : ((2 * w) / h);

        final RectF mainRect = new RectF(MARGIN, MARGIN, w - MARGIN, h - MARGIN);

        final float cx = mainRect.centerX();
        final float cy = mainRect.centerY();

        if (h > w) {
            final float width = mainRect.width();
            final float height = ratio * width;

            mainRect.top = cy - (height / 2);
            mainRect.bottom = cy + (height / 2);

        } else {
            final float height = mainRect.height();
            final float width = height / ratio;

            mainRect.left = cx - (width / 2);
            mainRect.right = cx + (width / 2);
        }
        final float oneScreenWidth2 = mainRect.height() / screenRatio2;
        final RectF rect = new RectF(cx - (RAD / 2), cy - (RAD / 2), cx
                + (RAD / 2), cy + (RAD / 2));
        main = new FrameControl(rect) {
            private final Paint orangePaint = new Paint();
            private final RectF rectToDraw = new RectF(rect);
            private final RectF oneDescRect = new RectF(mainRect.centerX()
                    - oneScreenWidth2,
                    mainRect.top,
                    mainRect.centerX()
                            + oneScreenWidth2,
                    mainRect.bottom);

            {
                boundsRect.set(mainRect);
                orangePaint.setStyle(Paint.Style.STROKE);
                orangePaint.setColor(Color.argb(200, 255, 200, 100));
            }

            @Override
            public void onDraw(final Canvas c) {
                c.drawArc(rectToDraw, 0f, 360f, false, controlPaint);
                //c.drawRect(boundsRect, mPaint);
                c.drawRect(oneDescRect, thinPaint);
                c.clipRect(boundsRect, Region.Op.DIFFERENCE);

                c.drawRect(maxBounds, mPaint);
                c.clipRect(maxBounds, Region.Op.REPLACE);
                c.drawRect(boundsRect, orangePaint);
            }

            @Override
            void onCreate() {

            }

            @Override
            boolean onMoveBy(final PointF d) {
                //  final RectF tmpRect = new RectF(boundsRect);
                // if (((boundsRect.left + d.x) < maxBounds.left)
                //        || ((boundsRect.right + d.x) > maxBounds.right)) {
                //   d.x = 0;
                //     }
                //    if (((boundsRect.top + d.y) < maxBounds.top)
                //           || ((boundsRect.bottom + d.y) > maxBounds.bottom)) {
                //      d.y = 0;
                // }
                // tmpRect.offset(d.x, d.y);

                //   if (!maxBounds.contains(tmpRect)) {
                //      return false;
                // }
                boundsRect.offset(d.x, d.y);
                rectToDraw.offset(d.x, d.y);
                oneDescRect.offset(d.x, d.y);
                return true;
            }

            @Override
            boolean onParentChange(final RectF d) {

                final boolean bdleft = d.left != 0;
                final boolean bdtop = d.top != 0;
                final boolean bdright = d.right != 0;
                final boolean bdbottom = d.bottom != 0;

                final boolean topLeft = bdleft && bdtop;
                final boolean topRight = bdright && bdtop;
                final boolean bottomLeft = bdleft && bdbottom;
                final boolean bottomRight = bdright && bdbottom;
                if (-100 != selectionRatio) {
                    if (topLeft) {
                        d.top = d.left * ratio;
                    } else if (bottomLeft) {
                        d.bottom = -d.left * ratio;
                    } else if (bdleft) {
                        d.top = (d.left * ratio) / 2;
                        d.bottom = (-d.left * ratio) / 2;
                    } else if (topRight) {
                        d.top = -d.right * ratio;
                    } else if (bottomRight) {
                        d.bottom = d.right * ratio;
                    } else if (bdright) {
                        d.top = (-d.right * ratio) / 2;
                        d.bottom = (d.right * ratio) / 2;
                    } else if (bdtop) {
                        d.left = d.top / ratio / 2;
                        d.right = -d.top / ratio / 2;
                    } else if (bdbottom) {
                        d.left = -d.bottom / ratio / 2;
                        d.right = d.bottom / ratio / 2;
                    }
                }
                //   if ((boundsRect.left + d.left) < maxBounds.left) {
                //      d.left = 0;
                // }
                //           if ((boundsRect.top + d.top) < maxBounds.top) {
                //              d.top = 0;
                //         }
                //        if ((boundsRect.right + d.right) > maxBounds.right) {
                //           d.right = 0;
                //      }
                //     if ((boundsRect.bottom + d.bottom) > maxBounds.bottom) {
                //        d.bottom = 0;
                //   }

                final RectF tmpRect = new RectF();
                tmpRect.set(boundsRect.left + d.left, boundsRect.top + d.top,
                        boundsRect.right + d.right, boundsRect.bottom
                        + d.bottom);

                if (//!maxBounds.contains(tmpRect)
                    //   ||
                        (tmpRect.width() < (2 * RAD)) || (tmpRect.height() < (2 * RAD))) {

                    return false;
                }
                boundsRect.set(tmpRect);

                oneDescRect.set(boundsRect.centerX()
                        - (boundsRect.height() / screenRatio2), boundsRect.top,
                        boundsRect.centerX()
                                + (boundsRect.height() / screenRatio2),
                        boundsRect.bottom);

                final float cx = boundsRect.centerX();
                final float cy = boundsRect.centerY();
                rectToDraw.left = cx - (RAD / 2);
                rectToDraw.top = cy - (RAD / 2);
                rectToDraw.right = cx + (RAD / 2);
                rectToDraw.bottom = cy + (RAD / 2);

                frameRect.set(rectToDraw);
                return true;
            }
        };
        final RectF leftTop = new RectF(mainRect.left, mainRect.top,
                mainRect.left + RAD, mainRect.top + RAD);
        main.addChild(new FrameControl(leftTop) {
            private final RectF roundRect = new RectF(leftTop);

            @Override
            void onCreate() {
            }

            @Override
            void onDraw(final Canvas c) {
                c.drawArc(roundRect, 0f, 360f, false, controlPaint);
            }

            @Override
            boolean onMoveBy(final PointF d) {
                changesRect.set(d.x, d.y, 0, 0);
                if (getParent().change(changesRect)) {
                    roundRect.offset(changesRect.left, changesRect.top);
                    d.x = changesRect.left;
                    d.y = changesRect.top;
                    return true;
                }
                return false;
            }

            @Override
            boolean onParentChange(final RectF d) {
                moveBy(d.left, d.top);
                return true;
            }
        });
        final RectF leftBottom = new RectF(mainRect.left,
                mainRect.bottom - RAD, mainRect.left + RAD, mainRect.bottom);
        main.addChild(new FrameControl(leftBottom) {
            private final RectF roundRect = new RectF(leftBottom);

            @Override
            void onCreate() {
            }

            @Override
            void onDraw(final Canvas c) {
                c.drawArc(roundRect, 0f, 360f, false, controlPaint);
            }

            @Override
            boolean onMoveBy(final PointF d) {
                changesRect.set(d.x, 0, 0, d.y);
                if (getParent().change(changesRect)) {
                    roundRect.offset(changesRect.left, changesRect.bottom);
                    d.x = changesRect.left;
                    d.y = changesRect.bottom;
                    return true;
                }
                return false;
            }

            @Override
            boolean onParentChange(final RectF d) {
                moveBy(d.left, d.bottom);
                return true;
            }
        });
        final RectF rightTop = new RectF(mainRect.right - RAD, mainRect.top,
                mainRect.right, mainRect.top + RAD);

        main.addChild(new FrameControl(rightTop) {
            private final RectF roundRect = new RectF(rightTop);

            @Override
            void onCreate() {
            }

            @Override
            void onDraw(final Canvas c) {
                c.drawArc(roundRect, 0f, 360f, false, controlPaint);
            }

            @Override
            boolean onMoveBy(final PointF d) {
                changesRect.set(0, d.y, d.x, 0);
                if (getParent().change(changesRect)) {
                    roundRect.offset(changesRect.right, changesRect.top);
                    d.x = changesRect.right;
                    d.y = changesRect.top;
                    return true;
                }
                return false;
            }

            @Override
            boolean onParentChange(final RectF d) {
                moveBy(d.right, d.top);
                return true;
            }
        });
        final RectF rightBottom = new RectF(mainRect.right - RAD,
                mainRect.bottom - RAD, mainRect.right, mainRect.bottom);

        main.addChild(new FrameControl(rightBottom) {
            private final RectF roundRect = new RectF(rightBottom);

            @Override
            void onCreate() {
            }

            @Override
            void onDraw(final Canvas c) {
                c.drawArc(roundRect, 0f, 360f, false, controlPaint);
            }

            @Override
            boolean onMoveBy(final PointF d) {
                changesRect.set(0, 0, d.x, d.y);
                if (getParent().change(changesRect)) {
                    roundRect.offset(changesRect.right, changesRect.bottom);
                    d.x = changesRect.right;
                    d.y = changesRect.bottom;
                    return true;
                }
                return false;
            }

            @Override
            boolean onParentChange(final RectF d) {
                moveBy(d.right, d.bottom);
                return true;
            }
        });
    }

    public void draw(final Canvas canvas) {
        main.draw(canvas);
    }

    public RectF getSelectionRect() {
        return main.getBoundsRect();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void moveBy(final float dx, final float dy) {
        if (null == selectedControl) {
            return;
        }
        selectedControl.moveBy(dx, dy);
    }

    public void setSelectionRatio(final float r) {
        selectionRatio = r;
    }

    public void touchDown(final Point p) {
        isSelected = consumeSelected(p);

    }

    public void touchUp() {
        if (null != selectedControl) {
            selectedControl.setTouched(false);
        }
        selectedControl = null;
        isSelected = false;
    }

    private boolean consumeSelected(final Point p) {
        selectedControl = main.getSelected(p);
        return null != selectedControl;
    }

    private abstract class FrameControl {
        public final RectF boundsRect = new RectF();
        protected final RectF frameRect = new RectF();
        protected final Paint selectedControlPaint = new Paint();
        private final Paint normalPaint = new Paint();
        private final List<FrameControl> childs = new ArrayList<FrameControl>();
        private final Paint noPaint = new Paint();
        private final Paint activePaint = new Paint();
        protected Paint mPaint;
        protected Paint controlPaint;
        protected Paint thinPaint = new Paint();
        protected RectF changesRect = new RectF();
        private FrameControl parent;
        private boolean isMover;
        private boolean isChanger;
        private boolean isTouched;

        FrameControl(final RectF frameRect) {
            this.frameRect.set(frameRect);
            selectedControlPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            selectedControlPaint.setColor(Color.argb(90, 100, 100, 100));
            normalPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            normalPaint.setColor(Color.argb(180, 10, 10, 10));
            normalPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            activePaint.setColor(Color.argb(10, 10, 10, 10));
            thinPaint.setStyle(Paint.Style.STROKE);
            thinPaint.setColor(Color.argb(180, 10, 10, 10));
            thinPaint.setAntiAlias(true);
            noPaint.setStyle(Paint.Style.STROKE);
            noPaint.setColor(Color.argb(20, 20, 20, 20));
            activePaint.setAntiAlias(true);
            normalPaint.setAntiAlias(true);
            selectedControlPaint.setAntiAlias(true);
            onCreate();
        }

        public void addChild(final FrameControl child) {
            childs.add(child);
            child.setParent(this);
        }

        public void draw(final Canvas c) {
            if (isSelected) {
                mPaint = selectedControlPaint;
            } else {
                mPaint = normalPaint;
            }
            if (isTouched()) {
                controlPaint = activePaint;
            } else if (isSelected) {
                controlPaint = noPaint;
            } else {
                controlPaint = thinPaint;
            }
            //      mPaint.setColor(Color.argb(rnd.nextInt(255), rnd.nextInt(255),
            ////             rnd.nextInt(255), rnd.nextInt(255)));
            onDraw(c);
            for (final FrameControl child : childs) {
                child.draw(c);
            }
        }

        public RectF getBoundsRect() {
            return boundsRect;
        }

        public FrameControl getSelected(final Point p) {
            FrameControl selectedControl = null;
            for (final FrameControl child : childs) {
                selectedControl = child.getSelected(p);
                if (null != selectedControl) {
                    selectedControl.setTouched(true);
                    break;
                }
            }
            if ((null == selectedControl) && this.isSelected(p)) {

                setTouched(true);
                return this;
            }
            return selectedControl;
        }

        public boolean isTouched() {
            return isTouched;
        }

        public void setTouched(final boolean isTouched) {
            this.isTouched = isTouched;
        }

        public boolean moveBy(final float dx, final float dy) {

            if (isMover || isChanger) {
                return false;
            }

            isMover = true;

            final PointF movePoint = new PointF(dx, dy);
            if (!onMoveBy(movePoint)) {
                isMover = false;
                return false;
            }
            frameRect.offset(movePoint.x, movePoint.y);

            for (final FrameControl child : childs) {
                child.moveBy(movePoint.x, movePoint.y);
            }

            isMover = false;

            return true;
        }

        protected boolean change(final RectF changesRect) {
            if (isChanger || isMover) {
                return true;
            }
            isChanger = true;
            if (!onParentChange(changesRect)) {
                isChanger = false;
                return false;
            }

            for (final FrameControl child : childs) {
                child.onParentChange(changesRect);
            }

            isChanger = false;
            return true;

        }

        FrameControl getParent() {
            return parent;
        }

        void setParent(final FrameControl parent) {
            this.parent = parent;
        }

        boolean isSelected(final Point p) {
            return frameRect.contains(p.x, p.y);
        }

        abstract void onCreate();

        abstract void onDraw(Canvas c);

        abstract boolean onMoveBy(final PointF dMove);

        abstract boolean onParentChange(final RectF changesRect);
    }
}
