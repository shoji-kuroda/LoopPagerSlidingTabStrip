package com.github.shoji_kuroda.looppagerslidingtabstrip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * Created by shoji.kuroda on 2016/09/06.
 */
public class SlidingTabLayout extends HorizontalScrollView {


    private static final String TAG = SlidingTabLayout.class.getSimpleName();

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * {@link #setCustomTabColorizer(TabColorizer)}.
     */
    public interface TabColorizer {

        /**
         * @return return the color of the indicator used when {@code position} is selected.
         */
        int getIndicatorColor(int position);

    }

    private static final int TAB_VIEW_PADDING_DIPS = 16;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 12;

    private int tabViewLayoutId;
    private int tabViewTextViewId;
    private int lastScrollTo;
    private boolean distributeEvenly;

    private LoopViewPager viewPager;
    private InnerLayout mInnerLayout;
    private SparseArray<String> contentDescriptions = new SparseArray<String>();
    private ViewPager.OnPageChangeListener viewPagerPageChangeListener;
    private AnimeManager mAnimeManager;
    private GestureDetector mGestureDetector;
    private CustomOnGestureListener mOnGestureListener = new CustomOnGestureListener();

    private final SlidingTabStrip[] tabStrip;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        mAnimeManager = new AnimeManager();
        mGestureDetector = new GestureDetector(context, mOnGestureListener);

        mInnerLayout = new InnerLayout(context);
        mInnerLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mInnerLayout.setLayoutParams(params);
        this.addView(mInnerLayout);

        // タブを3つつなげる
        this.tabStrip = new SlidingTabStrip[3];
        this.tabStrip[0] = new SlidingTabStrip(context);
        this.tabStrip[1] = new SlidingTabStrip(context);
        this.tabStrip[2] = new SlidingTabStrip(context);
        mInnerLayout.removeAllViews();
        for (SlidingTabStrip tab : this.tabStrip) {
            mInnerLayout.addView(tab, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Set the custom {@link TabColorizer} to be used.
     * <p>
     * If you only require simple custmisation then you can use
     * {@link #setSelectedIndicatorColors(int...)} to achieve
     * similar effects.
     */
    public void setCustomTabColorizer(TabColorizer tabColorizer) {
        mInnerLayout.removeAllViews();
        for (SlidingTabStrip tab : this.tabStrip) {
            tab.setCustomTabColorizer(tabColorizer);
        }
    }

    public void setDistributeEvenly(boolean distributeEvenly) {
        this.distributeEvenly = distributeEvenly;
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same color.
     */
    public void setSelectedIndicatorColors(int... colors) {
        for (SlidingTabStrip tab : this.tabStrip) {
            tab.setSelectedIndicatorColors(colors);
        }
    }

    /**
     * Set the {@link ViewPager.OnPageChangeListener}. When using {@link SlidingTabLayout} you are
     * required to set any {@link ViewPager.OnPageChangeListener} through this method. This is so
     * that the layout can update it's scroll position correctly.
     *
     * @see ViewPager#setOnPageChangeListener(ViewPager.OnPageChangeListener)
     */
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        viewPagerPageChangeListener = listener;
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId  id of the {@link TextView} in the inflated view
     */
    public void setCustomTabView(int layoutResId, int textViewId) {
        this.tabViewLayoutId = layoutResId;
        this.tabViewTextViewId = textViewId;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    public void setViewPager(LoopViewPager viewPager) {
        for (SlidingTabStrip tab : this.tabStrip) {
            tab.removeAllViews();
        }
        this.viewPager = viewPager;
        if (this.viewPager != null) {
            this.viewPager.addOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via
     * {@link #setCustomTabView(int, int)}.
     */
    protected TextView createDefaultTabView(Context context) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true);
        textView.setBackgroundResource(outValue.resourceId);
        textView.setAllCaps(true);

        int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding, padding, padding);

        return textView;
    }

    private void populateTabStrip() {
        final PagerAdapter adapter = this.viewPager.getAdapter();
        final View.OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < adapter.getCount(); i++) {
            for (SlidingTabStrip tab : this.tabStrip) {

                View tabView = null;
                TextView tabTitleView = null;

                if (this.tabViewLayoutId != 0) {
                    // If there is a custom tab view layout id set, try and inflate it
                    tabView = LayoutInflater.from(getContext()).inflate(this.tabViewLayoutId, tab, false);
                    tabTitleView = (TextView) tabView.findViewById(this.tabViewTextViewId);
                }

                if (tabView == null) {
                    tabView = createDefaultTabView(getContext());
                }

                if (tabTitleView == null && TextView.class.isInstance(tabView)) {
                    tabTitleView = (TextView) tabView;
                }

                if (distributeEvenly) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
                    lp.width = 0;
                    lp.weight = 1;
                }

                tabTitleView.setText(adapter.getPageTitle(i));
                tabView.setOnClickListener(tabClickListener);
                String desc = contentDescriptions.get(i, null);
                if (desc != null) {
                    tabView.setContentDescription(desc);
                }

                tab.addView(tabView);
                if (i == this.viewPager.getCurrentItem()) {
                    tabView.setSelected(true);
                }
            }
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.viewPager == null) {
            return;
        }
        if (tabStrip == null) {
            return;
        }
        int width = MeasureSpec.getSize(widthMeasureSpec);
        for (SlidingTabStrip tab : this.tabStrip) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
            lp.width = width / 3 * viewPager.getAdapter().getCount();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setContentDescription(int i, String desc) {
        contentDescriptions.put(i, desc);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (this.viewPager != null) {
            scrollToTab(this.viewPager.getCurrentItem(), 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        Log.d("aaa", "scrollTab : " + tabIndex + " / " + positionOffset);
        final int tabStripChildCount = this.tabStrip[1].getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }
        View selectedChild = this.tabStrip[1].getChildAt(tabIndex);
        if (selectedChild != null && selectedChild.getMeasuredWidth() != 0) {
            int targetScrollX = ((positionOffset + selectedChild.getLeft()) - getWidth() / 2) + selectedChild.getWidth() / 2;
            if (targetScrollX != lastScrollTo) {
                int unitWidth = computeHorizontalScrollRange() / 3;
                scrollTo(targetScrollX + unitWidth, 0);
                lastScrollTo = targetScrollX;
            }
        }
    }

    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        private int scrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            Log.d("aaa", "onPageScrolled : " + position);
            position = LoopViewPager.toRealPosition(position, viewPager.getAdapter().getCount());
            int tabStripChildCount = tabStrip[1].getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return;
            }
            for (SlidingTabStrip tab : tabStrip) {
                tab.onViewPagerPageChanged(position, positionOffset);
            }
            View selectedTitle = tabStrip[1].getChildAt(position);
            int selectedOffset = (selectedTitle == null) ? 0 : selectedTitle.getWidth();
            int nextTitlePosition = position + 1;
            View nextTitle = tabStrip[1].getChildAt(nextTitlePosition);
            if (nextTitle == null) {
                nextTitle = tabStrip[1].getChildAt(0);
            }
            int nextOffset = (nextTitle == null) ? 0 : nextTitle.getWidth();
            int extraOffset = (int) (0.5F * (positionOffset * (float) (selectedOffset + nextOffset)));

            scrollToTab(position, extraOffset);

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageScrolled(position, positionOffset,
                        positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            this.scrollState = state;

            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            Log.d("aaa", "onPageSelected : " + position);
            position = LoopViewPager.toRealPosition(position, viewPager.getAdapter().getCount());
            if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
                tabStrip[1].onViewPagerPageChanged(position, 0f);
                scrollToTab(position, 0);
            }
            for (int i = 0; i < tabStrip[1].getChildCount(); i++) {
                tabStrip[1].getChildAt(i).setSelected(position == i);
            }
            if (viewPagerPageChangeListener != null) {
                viewPagerPageChangeListener.onPageSelected(position);
            }
        }
    }

    private class TabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            for (SlidingTabStrip tab : tabStrip) {
                for (int i = 0; i < tab.getChildCount(); i++) {
                    if (v == tab.getChildAt(i)) {
                        viewPager.setCurrentItem(i);
                        return;
                    }
                }
            }
        }
    }

    public class InnerLayout extends LinearLayout {
        public InnerLayout(Context context) {
            super(context);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            resizing();
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            resizing();
        }

        private void resizing() {
            if (getChildCount() == 3) {
                View clone1 = this.getChildAt(0);
                View origin = this.getChildAt(1);
                View clone2 = this.getChildAt(2);
                int width = origin.getMeasuredWidth();
                if (width < SlidingTabLayout.this.getMeasuredWidth()) {
                    mInnerLayout.removeView(clone1);
                    mInnerLayout.removeView(clone2);
                } else {
                    clone1.setMinimumWidth(width);
                    clone2.setMinimumWidth(width);
                }
            }
        }
    }

    private class CustomOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean mIsFirstScroll = true; // Note: 子要素のACTION_DOWNが届かず誤動作するので初回を無視する。

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!mIsFirstScroll) {
                mAnimeManager.startScroll(velocityX);
            }
            mIsFirstScroll = false;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mAnimeManager.startFling(-velocityX / 50);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            mAnimeManager.stopFling();
            mIsFirstScroll = true;
            return true;
        }
    }

    private class AnimeManager {
        private static final int INTERVAL = 50; // ms
        private static final float ATTENUAION_RATE = 0.90F;
        private float mDelta = 0;
        private final Runnable mUpdateRunner = new Runnable() {
            @Override
            public void run() {
                onUpdate();
            }
        };

        public void update() {
            Handler handler = getHandler();
            if (handler == null) return;
            handler.postDelayed(mUpdateRunner, INTERVAL);
        }

        public void startScroll(float delta) {
            stopFling();
            if (!loopScrollPosition()) {
                scrollBy((int) delta, 0);
            }
        }

        public void startFling(float delta) {
            mDelta = delta;
            update();
        }

        public void stopFling() {
            mDelta = 0.0F;
        }

        private void onUpdate() {
            if (Math.abs(mDelta) > 1.0F) {
                scrollBy((int) mDelta, 0);
                mDelta = mDelta * ATTENUAION_RATE;
                loopScrollPosition();
                update();
            } else {
                stopFling();
            }
        }

        private boolean loopScrollPosition() {
            int curX = computeHorizontalScrollOffset();
            int unitWidth = computeHorizontalScrollRange() / 3;
            if (curX > unitWidth * 1.8F) {
                scrollTo(curX - unitWidth, 0);
                return true;
            } else if (curX < unitWidth * 0.2F) {
                scrollTo(curX + unitWidth, 0);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) mOnGestureListener.onDown(ev);
        return super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mGestureDetector.onTouchEvent(ev);
    }


}