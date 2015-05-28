package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.NodeUtils;
import org.faudroids.mrhyde.git.RepositoryManager;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

@ContentView(R.layout.activity_text_editor)
public final class TextEditorActivity extends AbstractActionBarActivity {

	static final String
			EXTRA_REPOSITORY = "EXTRA_REPOSITORY",
			EXTRA_IS_NEW_FILE = "EXTRA_IS_NEW_FILE";

	private static final String
			STATE_CONTENT = "STATE_CONTENT",
			STATE_EDIT_MODE = "STATE_EDIT_MODE";

	private static final String
			PREFS_NAME = TextEditorActivity.class.getSimpleName();

	private static final String
			KEY_SHOW_LINE_NUMBERS = "KEY_SHOW_LINE_NUMBERS";

	@Inject private RepositoryManager repositoryManager;
	@Inject private InputMethodManager inputMethodManager;

	@InjectView(R.id.content) private EditText editText;
	@InjectView(R.id.edit) private FloatingActionButton editButton;
	@InjectView(R.id.line_numbers) private TextView numLinesTextView;

	@Inject private NodeUtils nodeUtils;
	private FileManager fileManager;
	private FileData fileData; // file currently being edited
	private boolean showingLineNumbers;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// load arguments
		final boolean isNewFile = getIntent().getBooleanExtra(EXTRA_IS_NEW_FILE, false);
		final Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = repositoryManager.getFileManager(repository);

		// hide line numbers by default
		showingLineNumbers = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SHOW_LINE_NUMBERS, false);

		// start editing on long click
		editText.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (isEditMode()) return false;
				startEditMode();
				return true;
			}
		});

		// setup line numbers
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

			@Override
			public void afterTextChanged(Editable s) {
				updateLineNumbers();
			}
		});

		// setup edit button
		editButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startEditMode();
			}
		});

		// load selected file
		if (savedInstanceState != null && savedInstanceState.getSerializable(STATE_CONTENT) != null) {
			fileData = (FileData) savedInstanceState.getSerializable(STATE_CONTENT);
			boolean startEditMode = savedInstanceState.getBoolean(STATE_EDIT_MODE);
			setupContent(startEditMode);

		} else {
			showSpinner();
			compositeSubscription.add(fileManager.getTree()
					.flatMap(new Func1<DirNode, Observable<FileData>>() {
						@Override
						public Observable<FileData> call(DirNode rootNode) {
							FileNode node = (FileNode) nodeUtils.restoreInstanceState(getIntent().getExtras(), rootNode);

							if (!isNewFile) {
								return fileManager.getFile(node);
							} else {
								return Observable.just(new FileData(node, new byte[0]));
							}
						}
					})
					.compose(new DefaultTransformer<FileData>())
					.subscribe(new Action1<FileData>() {
						@Override
						public void call(FileData file) {
							hideSpinner();
							TextEditorActivity.this.fileData = file;
							setupContent(isNewFile);
						}
					}, new ErrorActionBuilder()
							.add(new DefaultErrorAction(this, "failed to get file content"))
							.add(new HideSpinnerAction(this))
							.build()));
		}

	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(STATE_CONTENT, fileData);
		outState.putBoolean(STATE_EDIT_MODE, isEditMode());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.text_editor, menu);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.action_show_line_numbers);
		if (showingLineNumbers) item.setChecked(true);
		else item.setChecked(false);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Timber.d("back pressed");
				if (isEditMode()) stopEditMode();
				else onBackPressed();
				return true;

			case R.id.action_show_line_numbers:
				if (item.isChecked()) item.setChecked(false);
				else item.setChecked(true);
				toggleLineNumbers();
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onBackPressed() {
		if (isEditMode()) {
			if (!isDirty()) {
				returnResult();
			} else {

				new AlertDialog.Builder(this)
						.setTitle(R.string.save_title)
						.setMessage(R.string.save_message)
						.setCancelable(false)
						.setPositiveButton(getString(R.string.save_ok), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								saveFile();
								returnResult();
							}
						})
						.setNegativeButton(getString(R.string.save_cancel), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								returnResult();
							}
						})
						.show();
			}
		} else {
			returnResult();
		}
	}


	private void setupContent(boolean startEditMode) {
		// set title and text
		setTitle(fileData.getFileNode().getPath());
		try {
			editText.setText(new String(fileData.getData(), "UTF-8"));
			editText.setTypeface(Typeface.MONOSPACE);
			if (startEditMode) startEditMode();
			else stopEditMode();
		} catch (UnsupportedEncodingException uee) {
			Timber.e(uee, "failed to read content");
		}
		updateLineNumbers();
	}


	private void saveFile() {
		Timber.d("saving file");
		try {
			fileManager.writeFile(new FileData(fileData.getFileNode(), editText.getText().toString().getBytes()));
		} catch (IOException ioe) {
			Timber.e(ioe, "failed to write file");
			// TODO
		}
	}


	/**
	 * @return true if the file has been changed
	 */
	private boolean isDirty() {
		if (fileData == null) return false;
		try {
			return !new String(fileData.getData(), "UTF-8").equals(editText.getText().toString());
		} catch (UnsupportedEncodingException uee) {
			Timber.e(uee, "failed to encoding content");
			return false;
		}
	}


	private void returnResult() {
		setResult(RESULT_OK);
		finish();
	}


	private void startEditMode() {
		editText.setFocusable(true);
		editText.setFocusableInTouchMode(true);
		editText.requestFocus();
		editButton.setVisibility(View.GONE);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_done);
		inputMethodManager.showSoftInput(editText, 0);
	}


	private void stopEditMode() {
		inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
		editText.setFocusable(false);
		editText.setFocusableInTouchMode(false);
		editButton.setVisibility(View.VISIBLE);
		if (isDirty()) saveFile();
	}


	private boolean isEditMode() {
		return editText.isFocusable();
	}


	private void toggleLineNumbers() {
		showingLineNumbers = !showingLineNumbers;
		SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
		editor.putBoolean(KEY_SHOW_LINE_NUMBERS, showingLineNumbers);
		editor.commit();
		updateLineNumbers();
	}


	private void updateLineNumbers() {
		if (showingLineNumbers) {
			numLinesTextView.setVisibility(View.VISIBLE);
		} else {
			numLinesTextView.setVisibility(View.GONE);
			return;
		}

		// delay updating lines until internal layout has been built
		editText.post(new Runnable() {
			@Override
			public void run() {
				numLinesTextView.setText("");
				int numLines = editText.getLineCount();
				int numCount = 1;
				for (int i = 0; i < numLines; ++i) {
					int start = editText.getLayout().getLineStart(i);
					if (start == 0) {
						numLinesTextView.append(numCount + "\n");
						numCount++;

					} else if (editText.getText().charAt(start - 1) == '\n') {
						numLinesTextView.append(numCount + "\n");
						numCount++;

					} else {
						numLinesTextView.append("\n");
					}
				}
				numLinesTextView.setTypeface(Typeface.MONOSPACE);
			}
		});
	}


}
