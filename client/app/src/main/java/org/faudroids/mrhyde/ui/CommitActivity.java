package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.util.Set;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;
import timber.log.Timber;

@ContentView(R.layout.activity_commit)
public final class CommitActivity extends AbstractActionBarActivity {

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	@Inject RepositoryManager repositoryManager;
	@InjectView(R.id.changes) TextView changesView;
	@InjectView(R.id.commit) Button commitButton;

	private FileManager fileManager;
	private Repository repository;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(getString(R.string.title_commit));
		repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = repositoryManager.getFileManager(repository);

		Observable.zip(
				fileManager.getChangedFiles(),
				fileManager.getDiff(),
				new Func2<Set<String>, String, Change>() {
					@Override
					public Change call(Set<String> files, String diff) {
						return new Change(files, diff);
					}
				})
				.compose(new DefaultTransformer<Change>())
				.subscribe(new Action1<Change>() {
					@Override
					public void call(Change change) {
						StringBuilder builder = new StringBuilder();
						for (String file : change.files) {
							builder.append(file).append('\n');
						}
						builder.append('\n');
						builder.append(change.diff);
						changesView.setText(builder.toString());
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Timber.e(throwable, "failed to load changes");
					}
				});

		commitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fileManager.commit()
						.compose(new DefaultTransformer<Void>())
						.subscribe(new Action1<Void>() {
							@Override
							public void call(Void nothing) {
								Toast.makeText(CommitActivity.this, "Commit success", Toast.LENGTH_SHORT).show();
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								Timber.e(throwable, "commit failed");
								Toast.makeText(CommitActivity.this, "Commit error", Toast.LENGTH_SHORT).show();

							}
						});
			}
		});
	}


	private static class Change {

		private final Set<String> files;
		private final String diff;

		public Change(Set<String> files, String diff) {
			this.files = files;
			this.diff = diff;
		}

	}

}
