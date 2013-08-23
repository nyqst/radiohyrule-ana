package com.radiohyrule.android.app;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.radiohyrule.android.R;

public class MainActivity
	extends SherlockFragmentActivity
	implements NavigationManager.NavigationItemChangedListener {
	
	protected static final String tagMainActivity = "com.radiohyrule.android.radiohyrule.MainActivity";
	
	protected DrawerLayout navigationDrawerLayout;
	protected ListView navigationListView;
	protected ActionBarDrawerToggle navigationDrawerToggle;
	
	protected NavigationManager navigationManager;

	protected CharSequence title;
	protected CharSequence navigationDrawerTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock); // http://stackoverflow.com/questions/12864298/java-lang-runtimeexception-theme-sherlock
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		navigationManager = new NavigationManager(this);
		navigationManager.setNavigationItemChangedListener(this);

		title = navigationDrawerTitle = getTitle();
		navigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navigationListView = (ListView) findViewById(R.id.navigation_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		navigationDrawerLayout.setDrawerShadow(R.drawable.navigation_drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener
		navigationListView.setAdapter(navigationManager.getListAdapter());
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
			selectItem(navigationManager.getDefaultNavigationItemPosition());
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

	protected void selectItem(int position) {
		// get fragment for selected position
		BaseFragment fragment = navigationManager.prepareFragmentForDisplay(position);
		if (fragment == null) {
			Log.e(tagMainActivity, "no fragment for position " + String.valueOf(position));
			position = navigationManager.getDefaultNavigationItemPosition();
			fragment = navigationManager.prepareFragmentForDisplay(position);

			if(fragment == null) {
				Log.e(tagMainActivity, "no fragment for fallback position " + String.valueOf(position));
				return;
			}
		}
		
		// set new fragment
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.content_frame, fragment)
			.commit();

		// set activity title
		setTitle(fragment.getTitle());

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

	@Override
	public void OnSelectedNavigationItemChanged(int position, BaseFragment fragment) {
		// set activity title
		setTitle(fragment.getTitle());
	}
}
