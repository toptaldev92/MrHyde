package org.faudroids.mrhyde.ui;

import org.faudroids.mrhyde.R;

import roboguice.inject.ContentView;

@ContentView(R.layout.activity_commit)
public final class CommitActivity extends AbstractActionBarActivity {

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	/*
	@Inject RepositoryManager repositoryManager;
	@InjectView(R.id.changed_files) TextView changesView;
	@InjectView(R.id.commit) Button commitButton;

	private FileManager fileManager;
	private Repository repository;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(getString(R.string.title_commit));
		repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = repositoryManager.getFileManager(repository);

		compositeSubscription.add(Observable.zip(
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
				}));

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
	}


	private static class Change {

		private final Set<String> files;
		private final String diff;

		public Change(Set<String> files, String diff) {
			this.files = files;
			this.diff = diff;
		}

	}
	*/

}
