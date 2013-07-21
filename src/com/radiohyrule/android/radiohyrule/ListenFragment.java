package com.radiohyrule.android.radiohyrule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ListenFragment extends BaseFragment {
	public ListenFragment() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_listen, container, false);
	}

	@Override
	String getTitle() {
		return "Listen";
	}
}
