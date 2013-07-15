package com.radiohyrule.android.radiohyrule;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	protected DrawerLayout navigationDrawerLayout;
	protected ListView navigationListView;
	protected ActionBarDrawerToggle navigationDrawerToggle;
	
	protected CharSequence title;
	protected CharSequence navigationDrawerTitle;
	
	protected String[] navigationItemTitles;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		title = navigationDrawerTitle = getTitle();
		navigationItemTitles = getResources().getStringArray(R.array.navigation_items);
		navigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationListView = (ListView) findViewById(R.id.navigation_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        navigationDrawerLayout.setDrawerShadow(R.drawable.navigation_drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        navigationListView.setAdapter(new ArrayAdapter<String>(this,
                R.layout.navigation_drawer_item, navigationItemTitles));
        navigationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectItem(position);
			}
		});

        // enable ActionBar app icon to behave as action to toggle navigation drawer
        this.setupHomeButton();

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
	
	protected void selectItem(int position) {
		Log.v("Foo", "selected " + Integer.valueOf(position).toString());
		
		// set new fragment
		Fragment fragment = new ListenFragment();
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.content_frame, fragment)
			.commit();
		
		// highlight the selected navigation list item
		navigationListView.setItemChecked(position, true);
		setTitle(navigationItemTitles[position]);
		navigationDrawerLayout.closeDrawer(navigationListView);
	}
	
	@Override
    public void setTitle(CharSequence title) {
        this.title = title;
        setActionBarTitle(title);
    }
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void setupHomeButton() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActionBar().setHomeButtonEnabled(true);
        }
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void setActionBarTitle(CharSequence title) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setTitle(title);
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (navigationDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        
        // handle action buttons
        return super.onOptionsItemSelected(item);
    }

}
