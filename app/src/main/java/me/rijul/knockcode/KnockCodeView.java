package me.rijul.knockcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import me.rijul.knockcode.SettingsHelper.OnSettingsReloadedListener;

public class KnockCodeView extends View implements OnSettingsReloadedListener {
    public enum Mode {
		READY, CORRECT, INCORRECT, DISABLED
	}

	private Paint mLinePaint;
	private Paint mInnerPaint;

	private int mPosition = -1;

	private OnPositionTappedListener mListener;

	private float mLineWidth;
	private Mode mMode = Mode.READY;
	private SettingsHelper mSettingsHelper;
	protected boolean mLongClick;
	private OnLongClickListener mLongClickListener;
	public Grid mPatternSize = new Grid(2,2);
	private Context mContext;

	public interface OnPositionTappedListener {
		void onPositionTapped(int pos);
	}

	public KnockCodeView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		mLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm);
		super.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (mLongClickListener != null) {
					if (vibrateOnLongPress())
						v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
					mLongClick = true;
					return mLongClickListener.onLongClick(KnockCodeView.this);
				}
				return false;
			}
		});
		TypedValue outValue = new TypedValue();
		context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
		setBackgroundResource(outValue.resourceId);
	}

	public KnockCodeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public KnockCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public void setOnPositionTappedListener(OnPositionTappedListener listener) {
		mListener = listener;
	}

	public void setSettingsHelper(SettingsHelper settingsHelper) {
		mSettingsHelper = settingsHelper;
		//mSettingsHelper.addOnReloadListener(this);
        if (mInnerPaint!=null)
            mInnerPaint.setColor(Color.parseColor("#FFFAFAFA"));
        //setPatternSize(mSettingsHelper.getPatternSize());
	}

	private boolean shouldDrawLines() {
		if (mSettingsHelper != null) {
			return mSettingsHelper.shouldDrawLines();
		} else {
			return true;
		}
	}
/*
	private boolean shouldDrawFill() {
		if (mSettingsHelper != null) {
			return false;
		} else {
			return true;
		}
	}
*/
    private boolean vibrateOnLongPress() {
        if (mSettingsHelper!=null)
            return mSettingsHelper.vibrateOnLongPress();
        else
            return true;
    }

    private boolean vibrateOnTap() {
        if (mSettingsHelper!=null)
            return mSettingsHelper.vibrateOnTap();
        else
            return false;
    }

    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mLinePaint == null) {
			mLinePaint = new Paint();
			mLinePaint.setColor(Color.GRAY);
			mLinePaint.setStrokeWidth(mLineWidth);
			mLinePaint.setAntiAlias(true);

			mInnerPaint = new Paint();
			if (mSettingsHelper==null)
                mInnerPaint.setColor(Color.parseColor("#26212121"));
            else
                mInnerPaint.setColor(Color.parseColor("#26FAFAFA"));
			mInnerPaint.setAntiAlias(true);
			mInnerPaint.setStyle(Paint.Style.FILL);
		}

		switch (mMode) {
		case READY:
			if (mSettingsHelper==null)
				mLinePaint.setColor(Color.parseColor("#FF212121"));
			else
				mLinePaint.setColor(Color.parseColor("#FFFAFAFA"));
			break;
		case CORRECT:
			mLinePaint.setColor(Color.parseColor("#FF4CAF50"));
			break;
		case INCORRECT:
			mLinePaint.setColor(Color.parseColor("#FFF44336"));
			break;
		case DISABLED:
			mLinePaint.setColor(Color.parseColor("#FF9E9E9E"));
			break;
		}

		if (!isEnabled()) {
			mLinePaint.setColor(Color.parseColor("#FF9E9E9E"));
		}

		if (shouldDrawLines()) {
            int i;
            //canvas.drawLine(startX, startY, stopX, stopY, Paint)
            //Vertical lines
            for(i=1; i<mPatternSize.numberOfColumns; ++i) {
                canvas.drawLine(i*getWidth()/mPatternSize.numberOfColumns, 0, i*getWidth()/mPatternSize.numberOfColumns, getHeight(), mLinePaint);
            }
            //Horizontal lines
            for(i=1; i<mPatternSize.numberOfRows; ++i) {
                canvas.drawLine(0, i*getHeight()/mPatternSize.numberOfRows, getWidth(), i*getHeight()/mPatternSize.numberOfRows, mLinePaint);
            }
		}
/*
		if (mPosition != -1 && shouldDrawFill()) {
			canvas.drawRect(getRectForPosition(mPosition), mInnerPaint);
		}
		*/
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		throw new RuntimeException("Unsupported");
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mLongClickListener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled())
			return true;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mPosition = getPositionOfClick(event.getX(), event.getY());
			invalidate();
            if (vibrateOnTap())
                performHapticFeedback((Build.VERSION.SDK_INT == Build.VERSION_CODES.M) ?
                        HapticFeedbackConstants.CONTEXT_CLICK : HapticFeedbackConstants.VIRTUAL_KEY ,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
			break;
		case MotionEvent.ACTION_UP:
			if (mLongClick) {
				mPosition = -1;
				invalidate();
				mLongClick = false;
				return super.onTouchEvent(event);
			} else {
				int position = getPositionOfClick(event.getX(), event.getY());
				if (mPosition != position) {
					mPosition = -1;
					break;
				}

				if (mListener != null)
					mListener.onPositionTapped(position);
			}
			mPosition = -1;
			invalidate();
			return super.onTouchEvent(event);
		}
		invalidate();
		return super.onTouchEvent(event);
	}

	private int getPositionOfClick(float x, float y) {
        int i,j;
        for(j=1; j<=mPatternSize.numberOfColumns; ++j)
            if (x<=j*getWidth()/mPatternSize.numberOfColumns)
                for (i=1; i<=mPatternSize.numberOfRows; ++i)
                    if (y<=i*getHeight()/mPatternSize.numberOfRows) {
                        return (j + (i - 1) * mPatternSize.numberOfColumns);
                    }
		return -1;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			mMode = Mode.DISABLED;
		}
		super.setEnabled(enabled);
		invalidate();
	}

	public void setMode(Mode mode) {
		if (mMode == mode)
			return;

		mMode = mode;
		if (mode == Mode.DISABLED && isEnabled()) {
			setEnabled(false);
		}

		invalidate();
	}

	@Override
	public void onSettingsReloaded() {
		invalidate();
	}

    public void setPatternSize(Grid g) {
		mPatternSize = g;
		invalidate();
	}
}
