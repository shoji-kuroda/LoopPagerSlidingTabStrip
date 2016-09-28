package com.github.shoji_kuroda.looppagerslidingtabstrip;

import android.os.Parcelable;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by shoji.kuroda on 2016/09/06.
 */
public class LoopPagerAdapterWrapper extends PagerAdapter {

    private PagerAdapter adapter;

    private SparseArray<ToDestroy> toDestroy = new SparseArray<ToDestroy>();

    private boolean boundaryCaching;

    void setBoundaryCaching(boolean flag) {
        boundaryCaching = flag;
    }

    LoopPagerAdapterWrapper(PagerAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void notifyDataSetChanged() {
        this.toDestroy = new SparseArray<>();
        super.notifyDataSetChanged();
    }

    int toRealPosition(int position) {
        int realCount = getRealCount();
        if (realCount == 0)
            return 0;
        int realPosition = (position - 1) % realCount;
        if (realPosition < 0)
            realPosition += realCount;

        return realPosition;
    }

    public int toInnerPosition(int realPosition) {
        int position = (realPosition + 1);
        return position;
    }

    private int getRealFirstPosition() {
        return 1;
    }

    private int getRealLastPosition() {
        return getRealFirstPosition() + getRealCount() - 1;
    }

    @Override
    public int getCount() {
        return this.adapter.getCount() + 2;
    }

    public int getRealCount() {
        return this.adapter.getCount();
    }

    public PagerAdapter getRealAdapter() {
        return this.adapter;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int realPosition = (this.adapter instanceof FragmentPagerAdapter || this.adapter instanceof FragmentStatePagerAdapter)
                ? position
                : toRealPosition(position);

        if (this.boundaryCaching) {
            ToDestroy toDestroy = this.toDestroy.get(position);
            if (toDestroy != null) {
                this.toDestroy.remove(position);
                return toDestroy.object;
            }
        }
        return this.adapter.instantiateItem(container, realPosition);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        int realFirst = getRealFirstPosition();
        int realLast = getRealLastPosition();
        int realPosition = (adapter instanceof FragmentPagerAdapter || adapter instanceof FragmentStatePagerAdapter)
                ? position
                : toRealPosition(position);

        if (this.boundaryCaching && (position == realFirst || position == realLast)) {
            this.toDestroy.put(position, new ToDestroy(container, realPosition,
                    object));
        } else {
            this.adapter.destroyItem(container, realPosition, object);
        }
    }

    /*
     * Delegate rest of methods directly to the inner adapter.
     */

    @Override
    public void finishUpdate(ViewGroup container) {
        this.adapter.finishUpdate(container);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return this.adapter.isViewFromObject(view, object);
    }

    @Override
    public void restoreState(Parcelable bundle, ClassLoader classLoader) {
        this.adapter.restoreState(bundle, classLoader);
    }

    @Override
    public Parcelable saveState() {
        return this.adapter.saveState();
    }

    @Override
    public void startUpdate(ViewGroup container) {
        this.adapter.startUpdate(container);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        this.adapter.setPrimaryItem(container, position, object);
    }
    
    /*
     * End delegation
     */

    /**
     * Container class for caching the boundary views
     */
    static class ToDestroy {
        ViewGroup container;
        int position;
        Object object;

        public ToDestroy(ViewGroup container, int position, Object object) {
            this.container = container;
            this.position = position;
            this.object = object;
        }
    }
}
