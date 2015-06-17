package org.faudroids.mrhyde.ui.utils;


import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.ActivityIntentFactory;

import java.text.DateFormat;

import javax.inject.Inject;

/**
 * Jekyll specific UI utils.
 */
public class JekyllUiUtils {

	private static final Typeface SANS_SERIF_LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();

	private final Context context;
	private final ActivityIntentFactory intentFactory;

	@Inject
	JekyllUiUtils(Context context, ActivityIntentFactory intentFactory) {
		this.context = context;
		this.intentFactory = intentFactory;
	}


	public void setDraftOverview(View view, final Draft draft, final Repository repository) {
		// set title
		TextView titleView = (TextView) view.findViewById(R.id.text_title);
		titleView.setText(draft.getTitle());
		titleView.setTypeface(SANS_SERIF_LIGHT);

		// set on click
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(intentFactory.createTextEditorIntent(repository, draft.getFileNode(), false));
			}
		});
	}


	public void setPostOverview(View view, final Post post, final Repository repository) {
		// set title
		TextView titleView = (TextView) view.findViewById(R.id.text_title);
		titleView.setText(post.getTitle());
		titleView.setTypeface(SANS_SERIF_LIGHT);

		// set date
		TextView dateView = (TextView) view.findViewById(R.id.text_date);
		dateView.setText(DATE_FORMAT.format(post.getDate()));

		// set on click
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(intentFactory.createTextEditorIntent(repository, post.getFileNode(), false));
			}
		});
	}

}
