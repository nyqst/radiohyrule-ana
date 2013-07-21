package com.radiohyrule.android.radiohyrule;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity {
	protected DrawerLayout navigationDrawerLayout;
	protected ListView navigationListView;
	protected ActionBarDrawerToggle navigationDrawerToggle;

	protected CharSequence title;
	protected CharSequence navigationDrawerTitle;

	protected String[] navigationItemTitles;

	protected ListenFragment listenFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock); // http://stackoverflow.com/questions/12864298/java-lang-runtimeexception-theme-sherlock
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		title = navigationDrawerTitle = getTitle();
		navigationItemTitles = getResources().getStringArray(R.array.navigation_items);
		navigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navigationListView = (ListView) findViewById(R.id.navigation_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		navigationDrawerLayout.setDrawerShadow(
				R.drawable.navigation_drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener
		navigationListView.setAdapter(new NavigationListAdapter());
		navigationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectItem(position);
			}
		});

		// enable ActionBar app icon to behave as action to toggle navigation drawer
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		navigationDrawerToggle = new ActionBarDrawerToggle(this, navigationDrawerLayout,
				R.drawable.ic_navigation_drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
			public void onDrawerClosed(View view) {
				setActionBarTitle(title);
			}
			public void onDrawerOpened(View drawerView) {
				setActionBarTitle(navigationDrawerTitle);
			}
		};
		navigationDrawerLayout.setDrawerListener(navigationDrawerToggle);

		// initially activate Listen fragment
		if (savedInstanceState == null) {
			selectItem(0);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		navigationDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		navigationDrawerToggle.onConfigurationChanged(newConfig);
	}

	protected Fragment getFragmentAtPosition(int position) {
		switch (position) {
		case 0:
			synchronized (this) {
				if (this.listenFragment == null)
					this.listenFragment = new ListenFragment();
			}
			return this.listenFragment;
		}

		return null;
	}

	protected void selectItem(int position) {
		Fragment fragment = getFragmentAtPosition(position);
		if (fragment != null) {
			// set new fragment
			getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.content_frame, fragment)
				.commit();

			// highlight the selected navigation list item
			navigationListView.setItemChecked(position, true);
			setTitle(navigationItemTitles[position]);
		} else {
			navigationListView.setItemChecked(0, true);
		}

		// close drawer
		navigationDrawerLayout.closeDrawer(navigationListView);
	}

	@Override
	public void setTitle(CharSequence title) {
		this.title = title;
		setActionBarTitle(title);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void setActionBarTitle(CharSequence title) {
		getSupportActionBar().setTitle(title);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		if (item.getItemId() == android.R.id.home) {
			if (navigationDrawerLayout.isDrawerOpen(navigationListView)) {
				navigationDrawerLayout.closeDrawer(navigationListView);
			} else {
				navigationDrawerLayout.openDrawer(navigationListView);
			}
		}

		// handle action buttons
		return super.onOptionsItemSelected(item);
	}

	public class NavigationListAdapter extends BaseAdapter implements
			ListAdapter {
		protected String[] parents  = { "Listen", "Library", null,     null,      null,  "Info" };
		protected String[] children = {  null,     null,    "Albums", "Artists", "Songs", null };

		protected static final int itemHeight = 64;
		protected static final int itemLeftPaddingLevel0 = 32;
		protected static final int itemLeftPaddingLevel1 = 64;
		protected static final int itemRightPadding = 56;

		@Override
		public int getCount() {
			return parents.length;
		}

		public boolean isParent(int position) {
			return parents[position] != null;
		}

		@Override
		public Object getItem(int position) {
			String parent = parents[position];
			if (parent != null) {
				return parent;
			} else {
				return children[position];
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// container layout
			LinearLayout layout = new LinearLayout(MainActivity.this);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			layout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, itemHeight));
			if (isParent(position)) {
				layout.setPadding(itemLeftPaddingLevel0, 0, itemRightPadding, 0);
			} else {
				layout.setPadding(itemLeftPaddingLevel1, 0, itemRightPadding, 0);
				layout.setBackgroundResource(R.drawable.navigation_drawer_item_level1_background);
			}

			// group label
			TextView textView = new TextView(MainActivity.this);
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			textView.setText(getItem(position).toString());
			layout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

			return layout;
		}
	}
}
