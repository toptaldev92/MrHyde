package org.faudroids.mrhyde.ui.utils;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.ActivityIntentFactory;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;

import java.text.DateFormat;

import javax.inject.Inject;

import rx.functions.Action1;

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


	public void showNewPostDialog(final JekyllManager jekyllManager, final Repository repository) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setTitle(R.string.new_post)
				.setNegativeButton(android.R.string.cancel, null);

		// create custom dialog view
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.dialog_new_post, null, false);

		// update filename view when title changes
		final EditText titleView = (EditText) view.findViewById(R.id.input);
		final TextView fileNameView = (TextView) view.findViewById(R.id.text_filename);
		fileNameView.setText(jekyllManager.postTitleToFilename(context.getString(R.string.your_awesome_title)));
		titleView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void onTextChanged(CharSequence postTitle, int start, int before, int count) {
				fileNameView.setText(jekyllManager.postTitleToFilename(postTitle.toString()));
			}
		});

		// show dialog
		builder
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// open editor with new post file
						// no need for a spinner because content has been downloaded previously
						jekyllManager.createNewPost(titleView.getText().toString())
								.compose(new DefaultTransformer<Post>())
								.subscribe(new Action1<Post>() {
									@Override
									public void call(Post post) {
										Intent newPostIntent = intentFactory.createTextEditorIntent(repository, post.getFileNode(), false);
										context.startActivity(newPostIntent);
									}
								}, new ErrorActionBuilder()
										.add(new DefaultErrorAction(context, "failed to create post"))
										.build());
					}
				})
				.setView(view)
				.show();

	}


	public void showNewDraftDialog() {
		// TODO
	}

}
