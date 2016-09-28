package com.github.shoji_kuroda.looppagerslidingtabstrip;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

/**
 * Created by shoji.kuroda on 2016/09/06.
 */

public class LoopViewPager extends ViewPager {

    private static final boolean DEFAULT_BOUNDARY_CASHING = false;

    OnPageChangeListener outerPageChangeListener;
    private LoopPagerAdapterWrapper adapter;
    private boolean boundaryCaching = DEFAULT_BOUNDARY_CASHING;

    public LoopViewPager(Context context) {
        this(context, null);
    }

    public LoopViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        super.setOnPageChangeListener(onPageChangeListener);
    }

    public static int toRealPosition(int position, int count) {
        position = position - 1;
        if (position < 0) {
            position += count;
        } else {
            position = position % count;
        }
        return position;
    }

    public void setBoundaryCaching(boolean flag) {
        boundaryCaching = flag;
        if (adapter != null) {
            adapter.setBoundaryCaching(flag);
        }
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        this.adapter = new LoopPagerAdapterWrapper(adapter);
        this.adapter.setBoundaryCaching(boundaryCaching);
        super.setAdapter(this.adapter);
    }

    @Override
    public PagerAdapter getAdapter() {
        return adapter != null ? adapter.getRealAdapter() : adapter;
    }

    @Override
    public int getCurrentItem() {
        return adapter != null ? adapter.toRealPosition(super.getCurrentItem()) : 0;
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        int realItem = adapter.toInnerPosition(item);
        super.setCurrentItem(realItem, smoothScroll);
    }

    @Override
    public void setCurrentItem(int item) {
        if (getCurrentItem() != item) {
            setCurrentItem(item, true);
        }
    }

    private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
        private float mPreviousOffset = -1;
        private float mPreviousPosition = -1;

        @Override
        public void onPageSelected(int position) {

            int realPosition = adapter.toRealPosition(position);
            if (mPreviousPosition != realPosition) {
                mPreviousPosition = realPosition;
                if (outerPageChangeListener != null) {
                    outerPageChangeListener.onPageSelected(realPosition);
                }
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
            int realPosition = position;
            if (adapter != null) {
                realPosition = adapter.toRealPosition(position);

                if (positionOffset == 0
                        && mPreviousOffset == 0
                        && (position == 0 || position == adapter.getCount() - 1)) {
                    setCurrentItem(realPosition, false);
                }
            }

            mPreviousOffset = positionOffset;
            if (outerPageChangeListener != null) {
                if (realPosition != adapter.getRealCount() - 1) {
                    outerPageChangeListener.onPageScrolled(realPosition,
                            positionOffset, positionOffsetPixels);
                } else {
                    if (positionOffset > .5) {
                        outerPageChangeListener.onPageScrolled(0, 0, 0);
                    } else {
                        outerPageChangeListener.onPageScrolled(realPosition,
                                0, 0);
                    }
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (adapter != null) {
                int position = LoopViewPager.super.getCurrentItem();
                int realPosition = adapter.toRealPosition(position);
                if (state == ViewPager.SCROLL_STATE_IDLE
                        && (position == 0 || position == adapter.getCount() - 1)) {
                    setCurrentItem(realPosition, false);
                }
            }
            if (outerPageChangeListener != null) {
                outerPageChangeListener.onPageScrollStateChanged(state);
            }
        }
    };
}
