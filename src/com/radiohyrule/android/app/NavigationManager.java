package com.radiohyrule.android.app;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
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
	protected static final String tagNavigationManager = "com.radiohyrule.android.app.NavigationManager";
	
	protected Context context;
	
	protected List<NavigationItem> navigationItems;
	protected int defaultNavigationItemPosition = 0;
	
	public NavigationManager(Context context) {
		this.context = context;
		
		navigationItems = new ArrayList<NavigationItem>();
		navigationItems.add(new ParentNavigationItem<ListenFragment>(R.drawable.navigation_item_listen_icon) {
			@Override public ListenFragment createFragment() { return new ListenFragment(); }
		});
		ParentNavigationItem<LibraryFragment> libraryItem = new ParentNavigationItem<LibraryFragment>(R.drawable.navigation_item_library_icon) {
			@Override public LibraryFragment createFragment() { return new LibraryFragment(); }
		};
		navigationItems.add(libraryItem);
		navigationItems.add(new LibraryChildNavigationItem(libraryItem, R.drawable.navigation_item_library_albums_icon, LibraryFragment.ViewId.Albums));
		navigationItems.add(new LibraryChildNavigationItem(libraryItem, R.drawable.navigation_item_library_artists_icon, LibraryFragment.ViewId.Artists));
		navigationItems.add(new LibraryChildNavigationItem(libraryItem, R.drawable.navigation_item_library_songs_icon, LibraryFragment.ViewId.Songs));
		navigationItems.add(new ParentNavigationItem<AboutFragment>(R.drawable.navigation_item_about_icon) {
			@Override public AboutFragment createFragment() { return new AboutFragment(); }
		});
	}
	
	public BaseFragment prepareFragmentForDisplay(int position) {
		if(position > 0 && position < navigationItems.size()) {
			return navigationItems.get(position).prepareFragmentForDisplay();
		} else {
			return null;
		}
	}
	public int getDefaultNavigationItemPosition() {
		return 0;
	}
	
	protected interface NavigationItem {
		public String getTitle();
		public int getIconResource();
		public boolean isIndented();
		
		public BaseFragment prepareFragmentForDisplay();
	}
	protected abstract class ParentNavigationItem<F extends BaseFragment> implements NavigationItem {
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
			return getFragment().getTitle();
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
	protected class ChildNavigationItem<F extends BaseFragment> implements NavigationItem {
		protected ParentNavigationItem<F> parent;
		protected int iconResource;
		public ChildNavigationItem(ParentNavigationItem<F> parent, int iconResource) {
			this.parent = parent;
			this.iconResource = iconResource;
		}
		public synchronized F getFragment() {
			return parent.getFragment();
		}
		@Override
		public String getTitle() {
			return getFragment().getTitle();
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
	protected class LibraryChildNavigationItem extends ChildNavigationItem<LibraryFragment> {
		protected LibraryFragment.ViewId viewId;
		public LibraryChildNavigationItem(ParentNavigationItem<LibraryFragment> parent, int iconResource, LibraryFragment.ViewId viewId) {
			super(parent, iconResource);
			this.viewId = viewId;
		}
		@Override
		public BaseFragment prepareFragmentForDisplay() {
			BaseFragment fragment = super.prepareFragmentForDisplay();
			if(fragment != null) {
				((LibraryFragment)fragment).switchToView(viewId);
			}
			return fragment;
		}
	}
	
	public ListAdapter getListAdapter() {
		return new NavigationListAdapter();
	}

	protected class NavigationListAdapter extends BaseAdapter implements ListAdapter {
		protected static final int itemHeight = 64;
		protected static final int itemLeftPaddingLevel0 = 32;
		protected static final int itemLeftPaddingLevel1 = 64;
		protected static final int itemRightPadding = 56;
		protected static final int iconRightPadding = 16;
		protected static final int iconSize = 64;

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
			if(position > 0 && position < getNavigationItems().size()) {
				return getNavigationItems().get(position).getTitle();
			} else {
				return null;
			}
		}
		protected boolean isItemIndented(int position) {
			if(position > 0 && position < getNavigationItems().size()) {
				return getNavigationItems().get(position).isIndented();
			} else {
				return false;
			}
		}
		protected int getItemIconResource(int position) {
			if(position > 0 && position < getNavigationItems().size()) {
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
			if (!isItemIndented(position)) {
				layout.setPadding(itemLeftPaddingLevel0, 0, itemRightPadding, 0);
			} else {
				layout.setPadding(itemLeftPaddingLevel1, 0, itemRightPadding, 0);
				layout.setBackgroundResource(R.drawable.navigation_drawer_item_level1_background);
			}
			
			// icon
			ImageView imageView = new ImageView(NavigationManager.this.context);
			imageView.setImageResource(getItemIconResource(position));
			imageView.setPadding(0, (itemHeight-iconSize)/2, iconRightPadding, (itemHeight-iconSize)/2);
			layout.addView(imageView, new LinearLayout.LayoutParams(iconSize, iconSize));

			// label
			TextView textView = new TextView(NavigationManager.this.context);
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			textView.setText(getItemTitle(position));
			layout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

			return layout;
		}
	}
}
