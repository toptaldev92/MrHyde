package org.faudroids.mrhyde.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import org.faudroids.mrhyde.R;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;


@ContentView(R.layout.activity_main)
public final class MainActivity extends RoboActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			Fragment fragment = new LoginFragment();
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment_container, fragment);
			transaction.addToBackStack(null);
			transaction.commit();
		}
	}

}
