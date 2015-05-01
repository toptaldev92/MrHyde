package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func3;
import timber.log.Timber;

@ContentView(R.layout.activity_commit)
public final class CommitActivity extends AbstractActionBarActivity {

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	private static final String
			STATE_FILES_EXPAND = "STATE_FILES_EXPAND",
			STATE_DIFF_EXPAND = "STATE_DIFF_EXPAND";

	@Inject RepositoryManager repositoryManager;

	@InjectView(R.id.changed_files_title) TextView changedFilesTitleView;
	@InjectView(R.id.changed_files) TextView changedFilesView;
	@InjectView(R.id.diff) TextView diffView;
	@InjectView(R.id.commit_button) Button commitButton;

	@InjectView(R.id.changed_files_expand) ImageButton changedFilesExpandButton;
	@InjectView(R.id.diff_expand) ImageButton diffExpandButton;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(getString(R.string.title_commit));
		changedFilesTitleView.setText(getString(R.string.commit_changed_files, ""));
		final Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		final FileManager fileManager = repositoryManager.getFileManager(repository);

		// load file content
		compositeSubscription.add(Observable.zip(
				fileManager.getChangedFiles(),
				fileManager.getDeletedFiles(),
				fileManager.getDiff(),
				new Func3<Set<String>, Set<String>, String, Change>() {
					@Override
					public Change call(Set<String> changedFiles, Set<String> deletedFiles, String diff) {
						return new Change(changedFiles, deletedFiles, diff);
					}
				})
				.compose(new DefaultTransformer<Change>())
				.subscribe(new Action1<Change>() {
					@Override
					public void call(Change change) {
						// updated changed files list
						List<String> filesList = new ArrayList<>();
						filesList.addAll(change.changedFiles);
						for (String deletedFile : change.deletedFiles) filesList.add(getString(R.string.commit_deleted, deletedFile));
						Collections.sort(filesList);

						StringBuilder builder = new StringBuilder();
						for (String file : filesList) {
							builder.append(file).append('\n');
						}
						changedFilesView.setText(builder.toString());
						changedFilesTitleView.setText(getString(R.string.commit_changed_files, String.valueOf(filesList.size())));

						// update diff
						diffView.setText(change.diff);
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Timber.e(throwable, "failed to load changes");
					}
				}));

		// setup commit button
		commitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uiUtils.showSpinner(CommitActivity.this);
				compositeSubscription.add(fileManager.commit()
						.compose(new DefaultTransformer<Void>())
						.subscribe(new Action1<Void>() {
							@Override
							public void call(Void nothing) {
								uiUtils.hideSpinner(CommitActivity.this);
								Timber.d("commit success");
								setResult(RESULT_OK);
								finish();
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								uiUtils.hideSpinner(CommitActivity.this);
								Timber.e(throwable, "commit failed");
								Toast.makeText(CommitActivity.this, "Commit error", Toast.LENGTH_SHORT).show();

							}
						}));
			}
		});

		// setup expand buttons
		changedFilesExpandButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleExpand(changedFilesExpandButton, changedFilesView);
			}
		});

		diffExpandButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleExpand(diffExpandButton, diffView);
			}
		});

		// restore expansions
		if (savedInstanceState != null) {
			if (savedInstanceState.getBoolean(STATE_FILES_EXPAND)) {
				changedFilesView.setVisibility(View.VISIBLE);
				changedFilesExpandButton.setBackgroundResource(R.drawable.ic_expand_less);
			}
			if (savedInstanceState.getBoolean(STATE_DIFF_EXPAND)) {
				diffView.setVisibility(View.VISIBLE);
				diffExpandButton.setBackgroundResource(R.drawable.ic_expand_less);
			}
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_FILES_EXPAND, changedFilesView.getVisibility() != View.GONE);
		outState.putBoolean(STATE_DIFF_EXPAND, diffView.getVisibility() != View.GONE);
	}


	private void toggleExpand(ImageButton button, View targetView) {
		if (targetView.getVisibility() == View.GONE) {
			expandView(targetView);
			button.setBackgroundResource(R.drawable.ic_expand_less);
		} else {
			collapseView(targetView);
			button.setBackgroundResource(R.drawable.ic_expand_more);
		}
	}


	/* Thanks to http://stackoverflow.com/a/13381228 */
	private void expandView(final View v) {
		v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		final int targetHeight = v.getMeasuredHeight();

		v.getLayoutParams().height = 0;
		v.setVisibility(View.VISIBLE);
		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				v.getLayoutParams().height = interpolatedTime == 1
						? LayoutParams.WRAP_CONTENT
						: (int)(targetHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
		v.startAnimation(a);
	}


	private void collapseView(final View v) {
		final int initialHeight = v.getMeasuredHeight();

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				if(interpolatedTime == 1){
					v.setVisibility(View.GONE);
				}else{
					v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
					v.requestLayout();
				}
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
		v.startAnimation(a);
	}


	private static class Change {

		private final Set<String> changedFiles, deletedFiles;
		private final String diff;

		public Change(Set<String> changedFiles, Set<String> deletedFiles, String diff) {
			this.changedFiles = changedFiles;
			this.deletedFiles = deletedFiles;
			this.diff = diff;
		}

	}

}
