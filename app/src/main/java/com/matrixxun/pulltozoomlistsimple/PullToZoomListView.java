package com.matrixxun.pulltozoomlistsimple;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PullToZoomListView extends ListView implements
        AbsListView.OnScrollListener {

    //    region 一堆变量的初始化
    private static final int INVALID_VALUE = -1;

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float paramAnonymousFloat) {
            float f = paramAnonymousFloat - 1.0F;
            return 1.0F + f * (f * (f * (f * f)));
        }
    };
    private final int INVALID_POINTER = -1;
    private final float INVALID_VAL = -1.0f;
    private int mActivePointerId = INVALID_POINTER;

    private float collapseFactor = 0.0f;//数值越大，顶栏图片上滚的速度越慢。


    private int endScalingTime = 200;//当用户放手时候，顶栏图片恢复正常比例的时间，默认0.2s
    private FrameLayout mHeaderContainer;
    private int mHeaderHeight;
    private ImageView mHeaderImage;
    float mLastMotionY = -1.0F;
    float mLastScale = -1.0F;
    float mMaxScale = -1.0F;
    private AbsListView.OnScrollListener mOnScrollListener;
    private ScalingRunnable mScalingRunnalable;
    private int SCREEN_HEIGHT;
    private ImageView mShadow;
    private String TAG = PullToZoomListView.class.getSimpleName();
    private boolean enableZoom=true;
    //endregion

    public PullToZoomListView(Context paramContext) {
        this(paramContext, null);

    }

    public PullToZoomListView(Context paramContext,
                              AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public PullToZoomListView(Context paramContext,
                              AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init(paramContext);
    }

    /**
     * 主要是往listview加多一个headView,
     * 这个headview由一个imageView和一个shadow构成（也是个imageView）
     * 这个shadow是为了可以加多一些阴影之类的效果用的，
     * 从而让我们的图片看起来比较。
     * 同時設置一個scrollListener
     *
     * @param paramContext
     */
    private void init(Context paramContext) {
        this.mScalingRunnalable = new ScalingRunnable();

        this.mHeaderContainer = new FrameLayout(paramContext);
        this.mShadow = new ImageView(paramContext);
        this.mHeaderImage = new ImageView(paramContext);

        this.SCREEN_HEIGHT = paramContext.getResources().getDisplayMetrics().heightPixels;
        int mScreenWidth = paramContext.getResources().getDisplayMetrics().widthPixels;
        setHeaderViewSize(mScreenWidth, (mScreenWidth * 9 / 16));

        FrameLayout.LayoutParams localLayoutParams = new FrameLayout.LayoutParams(
                -1, -2);
        localLayoutParams.gravity = 80;
        this.mShadow.setLayoutParams(localLayoutParams);
        this.mHeaderContainer.addView(this.mShadow);
        this.mHeaderContainer.addView(this.mHeaderImage);

        addHeaderView(this.mHeaderContainer);

        super.setOnScrollListener(this);
    }


    protected void onLayout(boolean paramBoolean, int paramInt1, int paramInt2,
                            int paramInt3, int paramInt4) {
        super.onLayout(paramBoolean, paramInt1, paramInt2, paramInt3, paramInt4);
        if (this.mHeaderHeight == 0)
            this.mHeaderHeight = this.mHeaderContainer.getHeight();
    }

    //region ScrollListener
    @Override
    public void onScroll(AbsListView paramAbsListView, int paramInt1,
                         int paramInt2, int paramInt3) {

        float position = this.mHeaderHeight - this.mHeaderContainer.getBottom();
        Log.d(TAG, "onScroll,headBtn=" + mHeaderContainer.getBottom() + " hh=" + mHeaderHeight + " f=" + position);

        //当f<=0,表示用户在下拉，到了需要放大图片的边缘了。
        //F>0.这时候view是向上拉，我们的headView是渐渐被隐藏起来了。
        if ((position > 0.0F) && (position < this.mHeaderHeight)) {
            Log.d(TAG, "1");
            int y = (int) (collapseFactor * position);
            this.mHeaderImage.scrollTo(0, -y);
            //靠这个I，我们的imageView和我们的listView的上滚速度形成不一致，
            // 导致看起来是我们下面的listView是超越了背后的imageView。

        } else if (this.mHeaderImage.getScrollY() != 0) {
            Log.d(TAG, "2");
            this.mHeaderImage.scrollTo(0, 0);
        }

        if (this.mOnScrollListener != null) {
            this.mOnScrollListener.onScroll(paramAbsListView, paramInt1,
                    paramInt2, paramInt3);
        }
    }

    public void onScrollStateChanged(AbsListView paramAbsListView, int paramInt) {
        if (this.mOnScrollListener != null)
            this.mOnScrollListener.onScrollStateChanged(paramAbsListView,
                    paramInt);
    }

    public void setOnScrollListener(
            AbsListView.OnScrollListener paramOnScrollListener) {
        this.mOnScrollListener = paramOnScrollListener;
    }
    //endregion

    //region touch处理内容
    public boolean onTouchEvent(MotionEvent motionEvent) {
        Log.d(TAG, "action= " + (motionEvent.getActionMasked()));

        switch (motionEvent.getActionMasked()) {

            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_DOWN:
                if (!this.mScalingRunnalable.isFinished()) {
                    this.mScalingRunnalable.abortAnimation();
                }

                this.mActivePointerId = motionEvent.getPointerId(0);
                this.mLastMotionY = motionEvent.getY(0);//默认就是0,只是显示的写出来，好理解些
                this.mMaxScale = (this.SCREEN_HEIGHT / this.mHeaderHeight);
                this.mLastScale = (this.mHeaderContainer.getBottom() / this.mHeaderHeight);
                Log.e(TAG, "onTouchEvent: maxScale=" + mMaxScale + " lastScale=" + mLastScale);
                break;

            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "mActivePointerId" + mActivePointerId);
                int pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                if (pointerIndex == INVALID_POINTER) {
                    Log.e(TAG, "Invalid pointerId="
                            + this.mActivePointerId + " in onTouchEvent");
                } else {
                    if (this.mLastMotionY == INVALID_VAL)
                        this.mLastMotionY = motionEvent.getY(pointerIndex);

                    //下拉需要放大图片的零界点
                    if (this.mHeaderContainer.getBottom() >= this.mHeaderHeight && enableZoom) {
                        ViewGroup.LayoutParams localLayoutParams = this.mHeaderContainer
                                .getLayoutParams();

                        float dy = motionEvent.getY(pointerIndex) - this.mLastMotionY;
//                        float curScale = (((dy + mHeaderContainer.getBottom()) / mHeaderHeight) - mLastScale)
//                                / 2.0F + this.mLastScale;
                        float curScale = (((dy + mHeaderContainer.getBottom()) / mHeaderHeight) / 2 + mLastScale / 2);

                        if ((this.mLastScale <= 1.0D) && (curScale < this.mLastScale)) {
                            localLayoutParams.height = this.mHeaderHeight;
                            this.mHeaderContainer
                                    .setLayoutParams(localLayoutParams);
                            return super.onTouchEvent(motionEvent);
                        }

                        this.mLastScale = Math.min(Math.max(curScale, 1.0F),
                                this.mMaxScale);

                        localLayoutParams.height = ((int) (this.mHeaderHeight * this.mLastScale));

                        if (localLayoutParams.height < this.SCREEN_HEIGHT)
                            this.mHeaderContainer
                                    .setLayoutParams(localLayoutParams);

                        this.mLastMotionY = motionEvent.getY(pointerIndex);
                        return true;
                    }
                    this.mLastMotionY = motionEvent.getY(pointerIndex);
                }
                break;
            case MotionEvent.ACTION_UP:
                reset();
                endScaling();
                break;
            case MotionEvent.ACTION_CANCEL:
                int actionIndex = motionEvent.getActionIndex();
                this.mLastMotionY = motionEvent.getY(actionIndex);
                this.mActivePointerId = motionEvent.getPointerId(actionIndex);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onSecondaryPointerUp(motionEvent);
                this.mLastMotionY = motionEvent.getY(motionEvent
                        .findPointerIndex(this.mActivePointerId));
                break;
            case MotionEvent.ACTION_POINTER_UP:
        }
        return super.onTouchEvent(motionEvent);
    }

    private void onSecondaryPointerUp(MotionEvent paramMotionEvent) {
//        int pointerIndex = (paramMotionEvent.getAction()) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        int pointerIndex = paramMotionEvent.getActionIndex();

        if (paramMotionEvent.getPointerId(pointerIndex) != this.mActivePointerId)
//            if (pointerIndex != 0) {
            this.mLastMotionY = paramMotionEvent.getY(pointerIndex);
        this.mActivePointerId = paramMotionEvent.getPointerId(pointerIndex);
//            }
    }

    private void reset() {
        this.mActivePointerId = INVALID_POINTER;
        this.mLastMotionY = INVALID_VAL;
        this.mMaxScale = INVALID_VAL;
        this.mLastScale = INVALID_VAL;
    }


    private void endScaling() {
        if (this.mHeaderContainer.getBottom() >= this.mHeaderHeight)
            Log.d(TAG, "endScaling");
        this.mScalingRunnalable.startAnimation(endScalingTime);
    }


    class ScalingRunnable implements Runnable {
        long mDuration;
        boolean mIsFinished = true;
        float mScale;//这个是我们放手时候的scale
        long mStartTime;

        private final float DEFAULT_SCALE_1 = 1.0F;

        ScalingRunnable() {
        }

        public void abortAnimation() {
            this.mIsFinished = true;
        }

        public boolean isFinished() {
            return this.mIsFinished;
        }

        public void run() {
            float curScale;
            ViewGroup.LayoutParams localLayoutParams;
            if ((!this.mIsFinished) && (this.mScale > DEFAULT_SCALE_1)) {
                float timeInterpolator = ((float) SystemClock.currentThreadTimeMillis() - (float) this.mStartTime)
                        / (float) this.mDuration;
                curScale = this.mScale - (this.mScale - DEFAULT_SCALE_1)
                        * PullToZoomListView.sInterpolator.getInterpolation(timeInterpolator);

                if (curScale > DEFAULT_SCALE_1) {
                    localLayoutParams = PullToZoomListView.this.mHeaderContainer
                            .getLayoutParams();
                    localLayoutParams.height = ((int) (curScale * PullToZoomListView.this.mHeaderHeight));
                    PullToZoomListView.this.mHeaderContainer
                            .setLayoutParams(localLayoutParams);

                    PullToZoomListView.this.post(this);//还没恢复到1,继续更新界面
                    return;
                }
                this.mIsFinished = true;
            }
        }

        public void startAnimation(long paramLong) {
            this.mStartTime = SystemClock.currentThreadTimeMillis();
            this.mDuration = paramLong;
            this.mScale = ((float) (PullToZoomListView.this.mHeaderContainer
                    .getBottom()) / PullToZoomListView.this.mHeaderHeight);
            this.mIsFinished = false;
            PullToZoomListView.this.post(this);
        }
    }
    //endregion

    //对外的几个接口
    //region public method
    public void setShadow(@DrawableRes int paramInt) {
        this.mShadow.setImageResource(paramInt);
    }

    public ImageView getHeaderView() {
        return this.mHeaderImage;
    }

    public void setHeaderViewSize(int width, int height) {
        ViewGroup.LayoutParams mHeaderContainerLayoutParams = this.mHeaderContainer.getLayoutParams();
        if (mHeaderContainerLayoutParams == null)
            mHeaderContainerLayoutParams = new AbsListView.LayoutParams(width, height);

        mHeaderContainerLayoutParams.width = width;
        mHeaderContainerLayoutParams.height = height;

        this.mHeaderContainer
                .setLayoutParams(mHeaderContainerLayoutParams);

        this.mHeaderHeight = height;
    }


    /**
     * 设置顶栏图片的上滚效果，
     * 0为直接上去，像一般的listview那样
     * 1为折叠效果
     *
     * @param collapseFactor 0~1.0f
     */
    public void setCollapseFactor(float collapseFactor) {
        if (collapseFactor < 0 || collapseFactor > 1.0f) {
            collapseFactor = 0.5f;
        }
        this.collapseFactor = collapseFactor;
    }


    public static final int PARALLAX = 3;
    public static final int STICK = 2;
    public static final int NORMAL = 1;

    @IntDef({PARALLAX, NORMAL, STICK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SCROLL_MODEL {
    }

    /***
     * {@link PullToZoomListView#setCollapseFactor(float)} 类似于这个函数作用
     *
     * @param model
     */
    public void setHeadScrollModel(@SCROLL_MODEL int model) {

        if (model == PARALLAX) {
            this.collapseFactor = 0.5f;
        } else if (model == STICK) {
            this.collapseFactor = 1;
        } else if (model == NORMAL) {
            this.collapseFactor = 0;
        }
    }

    public void setEndScalingTime(int endScalingTime) {
        this.endScalingTime = endScalingTime;
    }

    public void allowZoom(boolean enable) {
        this.enableZoom = enable;
    }

    public boolean isEnableZoom() {
        return enableZoom;
    }

    //endregion

}
