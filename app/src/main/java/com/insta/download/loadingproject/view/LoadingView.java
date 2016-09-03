package com.insta.download.loadingproject.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import java.util.ArrayList;

/**
 * Created by sunjinxue on 2016/8/30.
 */
public class LoadingView extends View {
    private static final String TAG = "LoadingView";

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator MATERIAL_INTERPOLATOR = new FastOutSlowInInterpolator();

    private static final float FULL_ROTATION = 1080.0f;
    private Rect mBounds;
    private float mRightLength;
    private static final float STROKE_WIDTH = 2.f;
    private final int[] COLORS = new int[] {
            Color.WHITE
    };

    /**
     * The value in the linear interpolator for animating the drawable at which
     * the color transition should start
     */
    private static final float COLOR_START_DELAY_OFFSET = 0.75f;
    private static final float END_TRIM_START_DELAY_OFFSET = 0.5f;
    private static final float START_TRIM_DURATION_OFFSET = 0.5f;
    private static final float START_RIGHT_DURATION_OFFSET = 0.3f;

    /** The duration of a single progress spin in milliseconds. */
    private static final int ANIMATION_DURATION = 1332;

    /** The number of points in the progress "star". */
    private static final float NUM_POINTS = 5f;
    /** The list of animators operating on this drawable. */
    private final ArrayList<Animation> mAnimators = new ArrayList<Animation>();

    /** The indicator ring, used to manage animation state. */
    private final Ring mRing;

    /** Canvas rotation in degrees. */
    private float mRotation;

    /** Layout info for the arrowhead for the large spinner in dp */
    private static final float MAX_PROGRESS_ARC = .8f;

    private Resources mResources;
    private View mParent;
    private Animation mAnimation;
    private float mRotationCount;
    boolean mFinishing;
    boolean mRestart;
    //绘制对号相关
    private boolean mDrawRight;
    private PathMeasure mPathMeasure;
    private final Paint mRightPaint = new Paint();
    private Path mRightPath;
    private Path mRightDst;
    private float mRightProgress;
    private ValueAnimator mRightAnimator;
    private float screenDensity;


    public LoadingView(Context context) {
        this(context,null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mParent = this;
        mResources = context.getResources();
        final DisplayMetrics metrics = mResources.getDisplayMetrics();
        screenDensity = metrics.density;
        mRing = new Ring();
        mRing.setColors(COLORS);

        mRightPaint.setColor(Color.WHITE);
        mRightPaint.setAntiAlias(true);
        mRightPaint.setStyle(Paint.Style.STROKE);
        mRightPaint.setStrokeWidth(STROKE_WIDTH*screenDensity);
        setupAnimators();
    }


    private void setSizeParameters(double progressCircleWidth, double progressCircleHeight,double centerRadius, double strokeWidth) {
        final Ring ring = mRing;
        float width = (float) (progressCircleWidth * screenDensity);
        float height = (float) (progressCircleHeight * screenDensity);
        ring.setStrokeWidth((float) strokeWidth * screenDensity);
        ring.setCenterRadius(centerRadius * screenDensity);
        ring.setColorIndex(0);
        ring.setInsets((int) width, (int) height);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mBounds = new Rect(0,0,getMeasuredWidth(),getMeasuredHeight());
        float diameter = Math.min(mBounds.width(),mBounds.height());
        setSizeParameters(diameter,diameter,diameter/2 - STROKE_WIDTH ,STROKE_WIDTH);
        //绘制对号path
        mRightPath = new Path();
        mRightDst = new Path();
        mRightPath.moveTo(diameter * 0.2428f,diameter * 0.4712f);
        mRightPath.lineTo(diameter * 0.4571f,diameter * 0.6642f);
        mRightPath.lineTo(diameter * 0.7642f,diameter * 0.3285f);
        mPathMeasure = new PathMeasure(mRightPath,false);
        mRightLength = mPathMeasure.getLength();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int saveCount = canvas.save();
        canvas.rotate(mRotation, mBounds.exactCenterX(), mBounds.exactCenterY());
        mRing.draw(canvas, mBounds);
        canvas.restoreToCount(saveCount);
        //draw right
        if (mDrawRight){
            mRightDst.reset();
            mRightDst.lineTo(0,0);
            mPathMeasure.getSegment(0,mRightLength*mRightProgress,mRightDst,true);
            canvas.drawPath(mRightDst, mRightPaint);
        }
    }

    public void setAlpha(int alpha) {
        mRing.setAlpha(alpha);
    }

    public float getAlpha() {
        return mRing.getAlpha();
    }

    public void setColorFilter(ColorFilter colorFilter) {
        mRing.setColorFilter(colorFilter);
    }

    public void setRotation(float rotation) {
        mRotation = rotation;
    }

    public float getRotation() {
        return mRotation;
    }

    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public boolean isRunning() {
        final ArrayList<Animation> animators = mAnimators;
        final int N = animators.size();
        for (int i = 0; i < N; i++) {
            final Animation animator = animators.get(i);
            if (animator.hasStarted() && !animator.hasEnded()) {
                return true;
            }
        }
        return false;
    }

    public void start() {
        mAnimation.reset();
        mRing.storeOriginals();
        if(mRightAnimator!=null&&mRightAnimator.isRunning()){
            mRightAnimator.end();
            mRing.resetOriginals();
        }
        // Already showing some part of the ring
        if (mRing.getEndTrim() != mRing.getStartTrim()) {
            mRestart = true;
            mAnimation.setDuration(ANIMATION_DURATION/2);
            mParent.startAnimation(mAnimation);
        } else {
            mRing.setColorIndex(0);
            mRing.resetOriginals();
            mAnimation.setDuration(ANIMATION_DURATION);
            mParent.startAnimation(mAnimation);
        }
    }

    public void stop() {
        mParent.clearAnimation();
        setRotation(0);
        mRing.setColorIndex(0);
        mRing.resetOriginals();
    }

    public void finish(){
        mAnimation.reset();
        mRing.storeOriginals();
        mFinishing = true;
        mAnimation.setRepeatCount(0);
        mAnimation.setDuration(ANIMATION_DURATION/2);
        mParent.startAnimation(mAnimation);
    }

    private float getMinProgressArc(Ring ring) {
        return (float) Math.toRadians(
                ring.getStrokeWidth() / (2 * Math.PI * ring.getCenterRadius()));
    }

    // Adapted from ArgbEvaluator.java
    private int evaluateColorChange(float fraction, int startValue, int endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                (int)((startB + (int)(fraction * (endB - startB))));
    }

    /**
     * Update the ring color if this is within the last 25% of the animation.
     * The new ring color will be a translation from the starting ring color to
     * the next color.
     */
    private void updateRingColor(float interpolatedTime, Ring ring) {
        if (interpolatedTime > COLOR_START_DELAY_OFFSET) {
            // scale the interpolatedTime so that the full
            // transformation from 0 - 1 takes place in the
            // remaining time
            ring.setColor(evaluateColorChange((interpolatedTime - COLOR_START_DELAY_OFFSET)
                            / (1.0f - COLOR_START_DELAY_OFFSET), ring.getStartingColor(),
                    ring.getNextColor()));
        }
    }

    private void applyFinishTranslation(float interpolatedTime, Ring ring) {
        // shrink back down and complete a full rotation before
        // starting other circles
        // Rotation goes between [0..1].
        updateRingColor(interpolatedTime, ring);
        float targetRotation = (float) (Math.floor(ring.getStartingRotation() / MAX_PROGRESS_ARC)
                + 1f);
        final float minProgressArc = getMinProgressArc(ring);
        final float startTrim = ring.getStartingStartTrim()
                + (1+ring.getStartingEndTrim() - ring.getStartingStartTrim()) * interpolatedTime;
        ring.setStartTrim(startTrim);
        ring.setEndTrim(ring.getStartingEndTrim());
        final float rotation = ring.getStartingRotation()
                + ((targetRotation - ring.getStartingRotation()) * interpolatedTime);
        ring.setRotation(rotation);
    }

    private void applyRestartTranslation(float interpolatedTime, Ring ring) {
        // shrink back down and complete a full rotation before
        // starting other circles
        // Rotation goes between [0..1].
        updateRingColor(interpolatedTime, ring);
        float targetRotation = (float) (Math.floor(ring.getStartingRotation() / MAX_PROGRESS_ARC) + 1f);
        final float minProgressArc = getMinProgressArc(ring);
        final float startTrim = ring.getStartingStartTrim()
                + (ring.getStartingEndTrim() - minProgressArc - ring.getStartingStartTrim()) * interpolatedTime;
        ring.setStartTrim(startTrim);
        ring.setEndTrim(ring.getStartingEndTrim());
        final float rotation = ring.getStartingRotation()
                + ((targetRotation - ring.getStartingRotation()) * interpolatedTime);
        ring.setRotation(rotation);
    }


    private void setupAnimators() {
        final Ring ring = mRing;
        final Animation animation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                Log.i(TAG, "interpolatedTime: "+interpolatedTime);
                if (mFinishing||this.hasEnded()) {
                    applyFinishTranslation(interpolatedTime, ring);
                }else if(mRestart){
                    applyRestartTranslation(interpolatedTime, ring);
                } else {
                    // The minProgressArc is calculated from 0 to create an
                    // angle that matches the stroke width.
                    final float minProgressArc = getMinProgressArc(ring);
                    final float startingEndTrim = ring.getStartingEndTrim();
                    final float startingTrim = ring.getStartingStartTrim();
                    final float startingRotation = ring.getStartingRotation();

                    updateRingColor(interpolatedTime, ring);

                    // Moving the start trim only occurs in the first 50% of a
                    // single ring animation
                    if (interpolatedTime <= START_TRIM_DURATION_OFFSET) {
                        // scale the interpolatedTime so that the full
                        // transformation from 0 - 1 takes place in the
                        // remaining time
                        final float scaledTime = (interpolatedTime)
                                / (1.0f - START_TRIM_DURATION_OFFSET);
                        final float startTrim = startingTrim
                                + ((MAX_PROGRESS_ARC - minProgressArc) * MATERIAL_INTERPOLATOR
                                .getInterpolation(scaledTime));
                        ring.setStartTrim(startTrim);
                    }

                    // Moving the end trim starts after 50% of a single ring
                    // animation completes
                    if (interpolatedTime > END_TRIM_START_DELAY_OFFSET) {
                        // scale the interpolatedTime so that the full
                        // transformation from 0 - 1 takes place in the
                        // remaining time
                        final float minArc = MAX_PROGRESS_ARC - minProgressArc;
                        float scaledTime = (interpolatedTime - START_TRIM_DURATION_OFFSET)
                                / (1.0f - START_TRIM_DURATION_OFFSET);
                        final float endTrim = startingEndTrim
                                + (minArc * MATERIAL_INTERPOLATOR.getInterpolation(scaledTime));
                        ring.setEndTrim(endTrim);
                    }

                    final float rotation = startingRotation + (0.25f * interpolatedTime);
                    ring.setRotation(rotation);

                    float groupRotation = ((FULL_ROTATION / NUM_POINTS) * interpolatedTime)
                            + (FULL_ROTATION * (mRotationCount / NUM_POINTS));
                    setRotation(groupRotation);
                }
                invalidate();
            }
        };
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.RESTART);
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        animation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                mRotationCount = 0;
                mDrawRight = false;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(mFinishing){
                    Log.i(TAG, "onAnimationEnd mFinishing: "+mFinishing);
                    // finished closing the last ring from the swipe gesture; go
                    // into progress mode
                    mFinishing = false;
                    mRightAnimator.start();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                Log.i(TAG, "onAnimationRepeat mFinishing: "+mFinishing);
                ring.storeOriginals();
                ring.goToNextColor();
                ring.setStartTrim(ring.getEndTrim());
                if (mFinishing) {
                    // finished closing the last ring from the swipe gesture; go
                    // into progress mode
                    mFinishing = false;
                    animation.setDuration(ANIMATION_DURATION);
                }else if(mRestart){
                    // finished closing the last ring from the swipe gesture; go
                    // into progress mode
                    mRestart = false;
                    animation.setDuration(ANIMATION_DURATION);
                } else {
                    mRotationCount = (mRotationCount + 1) % (NUM_POINTS);
                }
            }
        });
        mAnimation = animation;

        //绘制对号动画
        ValueAnimator valueAnimator = new ValueAnimator().ofFloat(0, 1);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
/*                float fraction = animation.getAnimatedFraction();
                if (fraction <= START_RIGHT_DURATION_OFFSET) {
                    final float scaledTime = (fraction) / (1.0f - START_RIGHT_DURATION_OFFSET);
                    MATERIAL_INTERPOLATOR.getInterpolation(scaledTime);
                }

                if (fraction > START_RIGHT_DURATION_OFFSET) {
                    float scaledTime = (fraction - START_RIGHT_DURATION_OFFSET)
                            / (1.0f - START_RIGHT_DURATION_OFFSET);
                    mRightProgress = MATERIAL_INTERPOLATOR.getInterpolation(scaledTime);
                }*/
                mRightProgress = animation.getAnimatedFraction();
                invalidate();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mDrawRight = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.setDuration(500);
        valueAnimator.setInterpolator(new AccelerateInterpolator(1.8f));
        mRightAnimator = valueAnimator;
    }


    private static class Ring {
        private final RectF mTempBounds = new RectF();
        private final Paint mPaint = new Paint();
        private float mStartTrim = 0.0f;
        private float mEndTrim = 0.0f;
        private float mRotation = 0.0f;
        private float mStrokeWidth = 5.0f;
        private float mStrokeInset = 2.5f;

        private int[] mColors;
        // mColorIndex represents the offset into the available mColors that the
        // progress circle should currently display. As the progress circle is
        // animating, the mColorIndex moves by one to the next available color.
        private int mColorIndex;
        private float mStartingStartTrim;
        private float mStartingEndTrim;
        private float mStartingRotation;
        private double mRingCenterRadius;
        private int mAlpha;
        private int mBackgroundColor;
        private int mCurrentColor;

        public Ring() {
            mPaint.setStrokeCap(Paint.Cap.SQUARE);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.RED);
        }

        public void setBackgroundColor(int color) {
            mBackgroundColor = color;
        }


        /**
         * Draw the progress spinner
         */
        public void draw(Canvas c, Rect bounds) {
            final RectF arcBounds = mTempBounds;
            arcBounds.set(bounds);
            arcBounds.inset(mStrokeInset, mStrokeInset);
            final float startAngle = (mStartTrim + mRotation) * 360;
            final float endAngle = (mEndTrim + mRotation) * 360;
            float sweepAngle = endAngle - startAngle;
            mPaint.setColor(mCurrentColor);
            c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint);
        }


        /**
         * Set the colors the progress spinner alternates between.
         *
         * @param colors Array of integers describing the colors. Must be non-<code>null</code>.
         */
        public void setColors(@NonNull int[] colors) {
            mColors = colors;
            // if colors are reset, make sure to reset the color index as well
            setColorIndex(0);
        }

        /**
         * Set the absolute color of the progress spinner. This is should only
         * be used when animating between current and next color when the
         * spinner is rotating.
         *
         * @param color int describing the color.
         */
        public void setColor(int color) {
            mCurrentColor = color;
        }

        /**
         * @param index Index into the color array of the color to display in
         *            the progress spinner.
         */
        public void setColorIndex(int index) {
            mColorIndex = index;
            mCurrentColor = mColors[mColorIndex];
        }

        /**
         * @return int describing the next color the progress spinner should use when drawing.
         */
        public int getNextColor() {
            return mColors[getNextColorIndex()];
        }

        private int getNextColorIndex() {
            return (mColorIndex + 1) % (mColors.length);
        }

        /**
         * Proceed to the next available ring color. This will automatically
         * wrap back to the beginning of colors.
         */
        public void goToNextColor() {
            setColorIndex(getNextColorIndex());
        }

        public void setColorFilter(ColorFilter filter) {
            mPaint.setColorFilter(filter);
        }

        /**
         * @param alpha Set the alpha of the progress spinner and associated arrowhead.
         */
        public void setAlpha(int alpha) {
            mAlpha = alpha;
        }

        /**
         * @return Current alpha of the progress spinner and arrowhead.
         */
        public int getAlpha() {
            return mAlpha;
        }

        /**
         * @param strokeWidth Set the stroke width of the progress spinner in pixels.
         */
        public void setStrokeWidth(float strokeWidth) {
            mStrokeWidth = strokeWidth;
            mPaint.setStrokeWidth(strokeWidth);
        }

        @SuppressWarnings("unused")
        public float getStrokeWidth() {
            return mStrokeWidth;
        }

        @SuppressWarnings("unused")
        public void setStartTrim(float startTrim) {
            mStartTrim = startTrim;
        }

        @SuppressWarnings("unused")
        public float getStartTrim() {
            return mStartTrim;
        }

        public float getStartingStartTrim() {
            return mStartingStartTrim;
        }

        public float getStartingEndTrim() {
            return mStartingEndTrim;
        }

        public int getStartingColor() {
            return mColors[mColorIndex];
        }

        @SuppressWarnings("unused")
        public void setEndTrim(float endTrim) {
            mEndTrim = endTrim;
        }

        @SuppressWarnings("unused")
        public float getEndTrim() {
            return mEndTrim;
        }

        @SuppressWarnings("unused")
        public void setRotation(float rotation) {
            mRotation = rotation;
        }

        @SuppressWarnings("unused")
        public float getRotation() {
            return mRotation;
        }

        public void setInsets(int width, int height) {
            final float minEdge = (float) Math.min(width, height);
            float insets;
            if (mRingCenterRadius <= 0 || minEdge < 0) {
                insets = (float) Math.ceil(mStrokeWidth / 2.0f);
            } else {
                insets = (float) (minEdge / 2.0f - mRingCenterRadius);
            }
            mStrokeInset = insets;
        }

        public float getInsets() {
            return mStrokeInset;
        }

        /**
         * @param centerRadius Inner radius in px of the circle the progress
         *            spinner arc traces.
         */
        public void setCenterRadius(double centerRadius) {
            mRingCenterRadius = centerRadius;
        }

        public double getCenterRadius() {
            return mRingCenterRadius;
        }

        /**
         * @return The amount the progress spinner is currently rotated, between [0..1].
         */
        public float getStartingRotation() {
            return mStartingRotation;
        }

        /**
         * If the start / end trim are offset to begin with, store them so that
         * animation starts from that offset.
         */
        public void storeOriginals() {
            mStartingStartTrim = mStartTrim;
            mStartingEndTrim = mEndTrim;
            mStartingRotation = mRotation;
        }

        /**
         * Reset the progress spinner to default rotation, start and end angles.
         */
        public void resetOriginals() {
            mStartingStartTrim = 0;
            mStartingEndTrim = 0;
            mStartingRotation = 0;
            setStartTrim(0);
            setEndTrim(0);
            setRotation(0);
        }

    }
}
