package com.radiohyrule.android.app;

import java.util.ArrayList;
import java.util.List;

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
import com.radiohyrule.android.about.AboutFragment;
import com.radiohyrule.android.library.LibraryFragment;
import com.radiohyrule.android.listen.ListenFragment;

public class NavigationManager {
    protected Context context;
    protected NavigationItemChangedListener navigationItemChangedListener;

    protected List<NavigationItem> navigationItems;

    public NavigationManager(Context context) {
        this.context = context;

        navigationItems = new ArrayList<NavigationItem>();
        navigationItems.add(new ParentNavigationItem<ListenFragment>(R.drawable.ic_navigation_item_listen) {
            @Override
            public ListenFragment createFragment() {
                return new ListenFragment();
            }
        });
		/*
        ParentNavigationItem<LibraryFragment> libraryItem = new ParentNavigationItem<LibraryFragment>(R.drawable.ic_navigation_item_library) {
            @Override
            public LibraryFragment createFragment() {
                LibraryFragment result = new LibraryFragment();
                result.setNavigationManager(NavigationManager.this);
                return result;
            }
        };
        navigationItems.add(libraryItem);
        navigationItems.add(new LibraryChildNavigationItem(libraryItem, R.drawable.ic_navigation_item_library_albums, "Albums", LibraryFragment.ViewId.Albums));
        navigationItems.add(new LibraryChildNavigationItem(libraryItem, R.drawable.ic_navigation_item_library_artists, "Artists", LibraryFragment.ViewId.Artists));
        navigationItems.add(new LibraryChildNavigationItem(libraryItem, R.drawable.ic_navigation_item_library_songs, "Songs", LibraryFragment.ViewId.Songs));
		*/
        navigationItems.add(new ParentNavigationItem<AboutFragment>(R.drawable.ic_navigation_item_about) {
            @Override
            public AboutFragment createFragment() {
                return new AboutFragment();
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

    // XXX this is really really ugly. will need to refactor the navigation items, base fragment and navigation manager
    public void OnSelectedLibraryChildChanged(LibraryFragment fragment, LibraryFragment.ViewId viewId) {
        if(this.navigationItemChangedListener != null) {
            boolean found = false;
            int position = 0;
            for(NavigationItem item : navigationItems) {
                if(item instanceof LibraryChildNavigationItem) {
                    LibraryChildNavigationItem child = (LibraryChildNavigationItem) item;
                    if(child.getViewId() == viewId) {
                        found = true;
                        break;
                    }
                }
                ++position;
            }
            if(found) {
                this.navigationItemChangedListener.OnSelectedNavigationItemChanged(position, fragment);
            }
        }
    }

    protected interface NavigationItem {
        public String getTitle();

        public int getIconResource();

        public boolean isIndented();

        public BaseFragment getFragment();

        public BaseFragment prepareFragmentForDisplay();
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

    protected static class ChildNavigationItem<F extends BaseFragment> implements NavigationItem {
        protected ParentNavigationItem<F> parent;
        protected int iconResource;
        protected String title;

        public ChildNavigationItem(ParentNavigationItem<F> parent, int iconResource, String title) {
            this.parent = parent;
            this.iconResource = iconResource;
            this.title = title;
        }

        public synchronized F getFragment() {
            return parent.getFragment();
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int getIconResource() {
            return iconResource;
        }

        @Override
        public boolean isIndented() {
            return true;
        }

        @Override
        public BaseFragment prepareFragmentForDisplay() {
            return getFragment();
        }
    }

    protected static class LibraryChildNavigationItem extends ChildNavigationItem<LibraryFragment> {
        protected LibraryFragment.ViewId viewId;

        public LibraryFragment.ViewId getViewId() {
            return viewId;
        }

        public LibraryChildNavigationItem(ParentNavigationItem<LibraryFragment> parent, int iconResource, String navigationListTitle, LibraryFragment.ViewId viewId) {
            super(parent, iconResource, navigationListTitle);
            this.viewId = viewId;
        }

        @Override
        public BaseFragment prepareFragmentForDisplay() {
            BaseFragment fragment = super.prepareFragmentForDisplay();
            if(fragment != null) {
                ((LibraryFragment) fragment).switchToSubview(viewId);
            }
            return fragment;
        }
    }

    public ListAdapter getListAdapter() {
        return new NavigationListAdapter();
    }

    protected class NavigationListAdapter extends BaseAdapter implements ListAdapter {
        protected final int itemHeight = NavigationManager.this.context.getResources().getDimensionPixelSize(R.dimen.navigation_drawer_item_height);
        protected final int itemLeftPaddingLevel0 = NavigationManager.this.context.getResources().getDimensionPixelSize(R.dimen.navigation_drawer_item_left_padding_level_0);
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
            if(!isItemIndented(position)) {
                layout.setPadding(itemLeftPaddingLevel0, 0, itemRightPadding, 0);
            } else {
                layout.setPadding(itemLeftPaddingLevel1, 0, itemRightPadding, 0);
                layout.setBackgroundResource(R.drawable.navigation_drawer_item_level1_background);
            }

            // icon
            ImageView imageView = new ImageView(NavigationManager.this.context);
            imageView.setImageResource(getItemIconResource(position));
            LinearLayout.LayoutParams imageViewLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            imageViewLp.setMargins(0, (itemHeight - iconSize) / 2, iconRightMargin, (itemHeight - iconSize) / 2);
            layout.addView(imageView, imageViewLp);

            // label
            TextView textView = new TextView(NavigationManager.this.context, null, android.R.attr.textAppearanceMedium);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            textView.setTextColor(NavigationManager.this.context.getResources().getColorStateList(android.R.color.secondary_text_dark));
            textView.setShadowLayer(2, 0, 2, Color.BLACK);
            textView.setText(getItemTitle(position));
            layout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            return layout;
        }
    }

    public static interface NavigationItemChangedListener {
        public void OnSelectedNavigationItemChanged(int position, BaseFragment fragment);
    }
}
