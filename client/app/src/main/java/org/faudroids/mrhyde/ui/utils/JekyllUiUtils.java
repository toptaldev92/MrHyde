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
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.ActivityIntentFactory;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;

import java.text.DateFormat;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action1;

/**
 * Jekyll specific UI utils.
 */
public class JekyllUiUtils {

	public interface OnContentCreatedListener<T> {

		void onContentCreated(T content);

	}


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


	public void showNewPostDialog(final JekyllManager jekyllManager, final Repository repository, final OnContentCreatedListener<Post> postListener) {
		showNewJekyllContentDialog(new NewJekyllContentStrategy<Post>(R.string.new_post, postListener) {
			@Override
			public String formatTitle(String title) {
				return jekyllManager.postTitleToFilename(title);
			}

			@Override
			public FileNode getFileNode(Post item) {
				return item.getFileNode();
			}

			@Override
			public Observable<Post> createNewItem(String title) {
				return jekyllManager.createNewPost(title);
			}
		}, repository);
	}


	public void showNewDraftDialog(final JekyllManager jekyllManager, Repository repository, OnContentCreatedListener<Draft> draftListener) {
		showNewJekyllContentDialog(new NewJekyllContentStrategy<Draft>(R.string.new_draft, draftListener) {
			@Override
			public String formatTitle(String title) {
				return jekyllManager.draftTitleToFilename(title);
			}

			@Override
			public FileNode getFileNode(Draft item) {
				return item.getFileNode();
			}

			@Override
			public Observable<Draft> createNewItem(String title) {
				return jekyllManager.createNewDraft(title);
			}
		}, repository);
	}


	private <T> void showNewJekyllContentDialog(final NewJekyllContentStrategy<T> strategy, final Repository repository) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setTitle(strategy.titleResource)
				.setNegativeButton(android.R.string.cancel, null);

		// create custom dialog view
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.dialog_new_post_or_draft, null, false);

		// update filename view when title changes
		final EditText titleView = (EditText) view.findViewById(R.id.input);
		final TextView fileNameView = (TextView) view.findViewById(R.id.text_filename);
		fileNameView.setText(strategy.formatTitle(context.getString(R.string.your_awesome_title)));
		titleView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void onTextChanged(CharSequence draftTitle, int start, int before, int count) {
				fileNameView.setText(strategy.formatTitle(draftTitle.toString()));
			}
		});

		// show dialog
		builder
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// open editor with new post file
						// no need for a spinner because content has been downloaded previously
						strategy.createNewItem(titleView.getText().toString())
								.compose(new DefaultTransformer<T>())
								.subscribe(new Action1<T>() {
									@Override
									public void call(T item) {
										if (strategy.contentListener != null) strategy.contentListener.onContentCreated(item);
										Intent newContentIntent = intentFactory.createTextEditorIntent(repository, strategy.getFileNode(item), false);
										context.startActivity(newContentIntent);
									}
								}, new ErrorActionBuilder()
										.add(new DefaultErrorAction(context, "failed to create jekyll content"))
										.build());
					}
				})
				.setView(view)
				.show();
	}


	private static abstract class NewJekyllContentStrategy<T> {

		private final int titleResource;
		private final OnContentCreatedListener<T> contentListener;

		public NewJekyllContentStrategy(int titleResource, OnContentCreatedListener<T> contentListener) {
			this.titleResource = titleResource;
			this.contentListener = contentListener;
		}

		public abstract String formatTitle(String title);
		public abstract FileNode getFileNode(T item);
		public abstract Observable<T> createNewItem(String title);

	}

}
