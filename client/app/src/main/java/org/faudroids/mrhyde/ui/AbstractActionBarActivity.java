package org.faudroids.mrhyde.ui;

import roboguice.activity.RoboActionBarActivity;
import rx.subscriptions.CompositeSubscription;


abstract class AbstractActionBarActivity extends RoboActionBarActivity {

	protected final CompositeSubscription compositeSubscription = new CompositeSubscription();

	@Override
	public void onDestroy() {
		super.onDestroy();
		compositeSubscription.unsubscribe();
	}

}
