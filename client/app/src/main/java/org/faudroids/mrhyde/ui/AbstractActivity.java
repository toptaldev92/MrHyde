package org.faudroids.mrhyde.ui;

import javax.inject.Inject;

import roboguice.activity.RoboActivity;
import rx.subscriptions.CompositeSubscription;


abstract class AbstractActivity extends RoboActivity {

	protected final CompositeSubscription compositeSubscription = new CompositeSubscription();

	@Override
	public void onDestroy() {
		super.onDestroy();
		compositeSubscription.unsubscribe();
	}

}
