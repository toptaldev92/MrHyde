package org.faudroids.mrhyde.ui;


import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

import org.faudroids.mrhyde.R;

import javax.inject.Inject;

/**
 * Keeps track of which help message should be shown and helps displaying those.
 */
public class HelpManager {

	private static final String PREFS_NAME = "HelpManager";

	private final Context context;

	@Inject
	HelpManager(Context context) {
		this.context = context;
	}


	/**
	 * Get and update whether the specified help should be displayed.
	 */
	public boolean shouldDisplayHelp(String key) {
		SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

		// true indicates that this help message has been shown
		return !preferences.getBoolean(key, false);
	}


	/**
	 * Helper method for setting up a help card.
	 */
	public void setupHelpView(final String key, final View helpView, int titleResource, int messageResource) {
		helpView.setVisibility(View.VISIBLE);

		// get views
		TextView titleView = (TextView) helpView.findViewById(R.id.text_help_title);
		TextView messageView = (TextView) helpView.findViewById(R.id.text_help_message);
		View okView = helpView.findViewById(R.id.text_help_ok);

		// set text
		titleView.setText(context.getString(titleResource));
		messageView.setText(context.getString(messageResource));

		// set collapse listener
		okView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// store help confirmation
				SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
				editor.putBoolean(key, true);
				editor.commit();

				// dismiss card
				final int initialHeight = helpView.getMeasuredHeight();
				Animation animation = new Animation() {
					@Override
					protected void applyTransformation(float interpolatedTime, Transformation t) {
						if(interpolatedTime == 1){
							helpView.setVisibility(View.GONE);
						} else {
							helpView.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
							helpView.requestLayout();
						}
					}

					@Override
					public boolean willChangeBounds() {
						return true;
					}
				};

				animation.setDuration((int) (initialHeight / context.getResources().getDisplayMetrics().density) * 3);
				helpView.startAnimation(animation);
			}
		});
	}

}
