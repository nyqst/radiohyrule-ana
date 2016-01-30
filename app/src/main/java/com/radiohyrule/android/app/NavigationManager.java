package com.radiohyrule.android.app;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.radiohyrule.android.R;
import com.radiohyrule.android.fragments.BaseFragment;
import com.radiohyrule.android.about.HelpAboutFragment;
import com.radiohyrule.android.library.LibraryFragment;
import com.radiohyrule.android.listen.ListenFragment;

import java.util.ArrayList;
import java.util.List;

public class NavigationManager {
    protected Context context;
    protected NavigationItemChangedListener navigationItemChangedListener;

    protected List<NavigationItem> navigationItems;

    public NavigationManager(final Context context) {
        this.context = context;

        navigationItems = new ArrayList<>();
        navigationItems.add(new ParentNavigationItem<ListenFragment>(R.drawable.ic_navigation_item_listen) {
            @Override
            public ListenFragment createFragment() {
                return new ListenFragment();
            }
        });
        navigationItems.add(new ParentNavigationItem<HelpAboutFragment>(R.drawable.ic_navigation_item_about) {
            @Override
            public HelpAboutFragment createFragment() {
                return HelpAboutFragment.newInstance(R.raw.about_rh);
            }
        });
    }

    public void setNavigationItemChangedListener(NavigationItemChangedListener listener) {
        navigationItemChangedListener = listener;
    }

    public BaseFragment prepareFragmentForDisplay(int position) {
        if(position >= 0 && position < navigationItems.size()) {
            return navigationItems.get(position).prepareFragmentForDisplay();
        } else {
            return null;
        }
    }

    public int getDefaultNavigationItemPosition() {
        return 0;
    }
    public int getListenNavigationItemPosition() {
        for(int position = 0; position < navigationItems.size(); ++position) {
            NavigationItem navItem = navigationItems.get(position);
            if(navItem.getIconResource() == R.drawable.ic_navigation_item_listen) // XXX
                return position;
        }
        return -1;
    }

    protected interface NavigationItem {
        String getTitle();

        int getIconResource();

        boolean isIndented();

        BaseFragment getFragment();

        BaseFragment prepareFragmentForDisplay();
    }

    protected static abstract class ParentNavigationItem<F extends BaseFragment> implements NavigationItem {
        protected F fragment;
        protected int iconResource;

        public ParentNavigationItem(int iconResource) {
            this.iconResource = iconResource;
        }

        public abstract F createFragment();

        public synchronized F getFragment() {
            if(fragment == null) {
                fragment = createFragment();
            }
            return fragment;
        }

        @Override
        public String getTitle() {
            return getFragment().getTitleText();
        }

        @Override
        public int getIconResource() {
            return iconResource;
        }

        @Override
        public boolean isIndented() {
            return false;
        }

        @Override
        public BaseFragment prepareFragmentForDisplay() {
            return getFragment();
        }
    }

    public ListAdapter getListAdapter() {
        return new NavigationListAdapter();
    }

    protected class NavigationListAdapter extends BaseAdapter implements ListAdapter {
        protected final int itemHeight = NavigationManager.this.context.getResources().getDimensionPixelSize(R.dimen.navigation_drawer_item_height);
        protected final int itemLeftPaddingLevel1 = NavigationManager.this.context.getResources().getDimensionPixelSize(R.dimen.navigation_drawer_item_left_padding_level_1);
        protected final int itemRightPadding = NavigationManager.this.context.getResources().getDimensionPixelSize(R.dimen.navigation_drawer_item_right_padding);
        protected final int iconRightMargin = NavigationManager.this.context.getResources().getDimensionPixelSize(R.dimen.navigation_drawer_icon_right_margin);
        protected final int iconSize = NavigationManager.this.context.getResources().getDimensionPixelSize(R.dimen.navigation_drawer_icon_size);

        protected List<NavigationItem> getNavigationItems() {
            return NavigationManager.this.navigationItems;
        }

        @Override
        public int getCount() {
            return getNavigationItems().size();
        }

        @Override
        public Object getItem(int position) {
            return getItemTitle(position);
        }

        protected String getItemTitle(int position) {
            if(position >= 0 && position < getNavigationItems().size()) {
                return getNavigationItems().get(position).getTitle();
            } else {
                return null;
            }
        }

        protected boolean isItemIndented(int position) {
            if(position >= 0 && position < getNavigationItems().size()) {
                return getNavigationItems().get(position).isIndented();
            } else {
                return false;
            }
        }

        protected int getItemIconResource(int position) {
            if(position >= 0 && position < getNavigationItems().size()) {
                return getNavigationItems().get(position).getIconResource();
            } else {
                return -1;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // container layout
            LinearLayout layout = new LinearLayout(NavigationManager.this.context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, itemHeight));
            layout.setPadding(itemLeftPaddingLevel1, 0, itemRightPadding, 0);
            layout.setBackgroundResource(R.drawable.dark_trans_selector);

            // icon
            ImageView imageView = new ImageView(NavigationManager.this.context);
            imageView.setImageResource(getItemIconResource(position));
            LinearLayout.LayoutParams imageViewLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            imageViewLp.setMargins(0, (itemHeight - iconSize) / 2, iconRightMargin, (itemHeight - iconSize) / 2);
            layout.addView(imageView, imageViewLp);

            // label
            TextView textView = new TextView(NavigationManager.this.context, null, android.R.attr.textAppearanceMedium);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            textView.setTextColor(Color.LTGRAY);
            textView.setText(getItemTitle(position));
            layout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            return layout;
        }
    }

    public interface NavigationItemChangedListener {
        void OnSelectedNavigationItemChanged(int position, BaseFragment fragment);
    }
}
