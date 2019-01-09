package g33.uav;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;


/**
 * 普通环形进度条
 */
public class CircleProgressView extends View {
    private int mCurrent;//当前进度
    private Paint mBgPaint;//背景弧线paint
    private Paint mProgressPaint;//进度Paint
    private float mProgressWidth;//进度条宽度
    private int mProgressColor = Color.RED;//进度条颜色
    private int locationStart;//起始位置
    private float startAngle;//开始角度
    private ValueAnimator mAnimator;

    public CircleProgressView(Context context) {
        this(context, null);
    }

    public CircleProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView);
        locationStart = typedArray.getInt(R.styleable.CircleProgressView_location_start, 1);
        mProgressWidth = typedArray.getDimension(R.styleable.CircleProgressView_progress_width, dp2px(context, 4));
        mProgressColor = typedArray.getColor(R.styleable.CircleProgressView_progress_color, mProgressColor);
        typedArray.recycle();

        //背景圆弧
        mBgPaint = new Paint();
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStrokeWidth(mProgressWidth);
        mBgPaint.setStyle(Paint.Style.STROKE);
        mBgPaint.setColor(Color.parseColor("#eaecf0"));
        mBgPaint.setStrokeCap(Paint.Cap.ROUND);

        //进度圆弧
        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mProgressWidth);
        mProgressPaint.setColor(mProgressColor);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);

        //进度条起始角度
        if (locationStart == 1) {//左
            startAngle = -180;
        } else if (locationStart == 2) {//上
            startAngle = -90;
        } else if (locationStart == 3) {//右
            startAngle = 0;
        } else if (locationStart == 4) {//下
            startAngle = 90;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = width < height ? width : height;
        setMeasuredDimension(size, size);
    }

    /**
     * oval  // 绘制范围
     * startAngle  // 开始角度
     * sweepAngle  // 扫过角度
     * useCenter   // 是否使用中心
     */
    @Override
    protected void onDraw(Canvas canvas) {
        //绘制背景圆弧
        RectF rectF = new RectF(mProgressWidth / 2, mProgressWidth / 2, getWidth() - mProgressWidth / 2, getHeight() - mProgressWidth / 2);
        canvas.drawArc(rectF, 0, 360, false, mBgPaint);

        //绘制当前进度
        float sweepAngle = 360 * mCurrent / 100;
        canvas.drawArc(rectF, startAngle, sweepAngle, false, mProgressPaint);
    }

    public int getCurrent() {
        return mCurrent;
    }

    /**
     * 设置进度
     *
     * @param current
     */
    public void setCurrent(int current) {
        mCurrent = current;
        invalidate();
    }

    private int tCurrent = -1;

    /**
     * 动画效果
     *
     * @param current  精度条进度：0-100
     * @param duration 动画时间
     */
    public void startAnimProgress(int current, int duration) {
        mAnimator = ValueAnimator.ofInt(0, current);
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int current = (int) animation.getAnimatedValue();
                if (tCurrent != current) {
                    tCurrent = current;
                    setCurrent(current);
                    if (mOnAnimProgressListener != null)
                        mOnAnimProgressListener.valueUpdate(current);
                }
            }
        });
        mAnimator.start();
    }

    public interface OnAnimProgressListener {
        void valueUpdate(int progress);
    }

    private OnAnimProgressListener mOnAnimProgressListener;

    /**
     * 监听进度条进度
     *
     * @param onAnimProgressListener
     */
    public void setOnAnimProgressListener(OnAnimProgressListener onAnimProgressListener) {
        mOnAnimProgressListener = onAnimProgressListener;
    }

    public void destroy() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
