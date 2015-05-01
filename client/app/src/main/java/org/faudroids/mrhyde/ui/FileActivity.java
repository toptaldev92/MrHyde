package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.NodeUtils;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.utils.DefaultTransformer;

import java.io.IOException;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

@ContentView(R.layout.activity_file)
public final class FileActivity extends AbstractActionBarActivity {

	static final String
			EXTRA_REPOSITORY = "EXTRA_REPOSITORY",
			EXTRA_IS_NEW_FILE = "EXTRA_IS_NEW_FILE";


	@Inject RepositoryManager repositoryManager;
	@InjectView(R.id.title) EditText editText;
	@InjectView(R.id.submit) Button submitButton;
	@InjectView(R.id.numLines) TextView numLinesTextView;

	@Inject NodeUtils nodeUtils;
	private FileManager fileManager;
	private FileNode fileNode; // file currently being edited


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load arguments
		final boolean isNewFile = getIntent().getBooleanExtra(EXTRA_IS_NEW_FILE, false);
		final Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = repositoryManager.getFileManager(repository);

		// setup ui
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable s)
			{
				numLinesTextView.setText("");
				int numLines = editText.getLineCount();
				int numCount = 1;
				for(int i = 0; i < numLines; ++i)
				{
					int start = editText.getLayout().getLineStart(i);
					if(start == 0)
					{
						numLinesTextView.append(numCount + "\n");
						numCount++;
					}
					else if(editText.getText().charAt(start-1) == '\n') {
						numLinesTextView.append(numCount + "\n");
						numCount++;
					}
					else {
						numLinesTextView.append("\n");
					}
				}

			}
		});
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					fileManager.writeFile(fileNode, editText.getText().toString());
				} catch (IOException ioe) {
					Timber.e(ioe, "failed to write file");
					// TODO
				}
				setResult(RESULT_OK);
				finish();
			}
		});

		// load selected file
		showSpinner();
		compositeSubscription.add(fileManager.getTree()
				.flatMap(new Func1<DirNode, Observable<LoadedFile>>() {
					@Override
					public Observable<LoadedFile> call(DirNode rootNode) {
						FileNode node = (FileNode) nodeUtils.restoreInstanceState(getIntent().getExtras(), rootNode);

						final LoadedFile loadedFile = new LoadedFile();
						loadedFile.node = node;

						if (!isNewFile) {
							return fileManager.getFile(node)
									.flatMap(new Func1<String, Observable<LoadedFile>>() {
										@Override
										public Observable<LoadedFile> call(String content) {
											loadedFile.content = content;
											return Observable.just(loadedFile);
										}
									});
						} else {
							loadedFile.content = "";
							return Observable.just(loadedFile);
						}
					}
				})
				.compose(new DefaultTransformer<LoadedFile>())
				.subscribe(new Action1<LoadedFile>() {
					@Override
					public void call(LoadedFile loadedFile) {
						hideSpinner();
						setTitle(loadedFile.node.getPath());
						editText.setText(loadedFile.content);
						FileActivity.this.fileNode = loadedFile.node;
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						hideSpinner();
						Toast.makeText(FileActivity.this, "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get content");
					}
				}));

	}


	private static final class LoadedFile {

		private FileNode node;
		private String content;

	}

}
