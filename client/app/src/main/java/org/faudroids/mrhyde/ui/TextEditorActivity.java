package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileData;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileManagerFactory;
import org.faudroids.mrhyde.git.FileNode;
import org.faudroids.mrhyde.git.NodeUtils;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.UndoRedoEditText;
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

	private static final int EDITOR_MAX_HISTORY = 100;

	static final String
			EXTRA_REPOSITORY = "EXTRA_REPOSITORY",
			EXTRA_IS_NEW_FILE = "EXTRA_IS_NEW_FILE";

	private static final String
			STATE_FILE_DATA = "STATE_FILE_DATA",
			STATE_TEXT = "STATE_TEXT",
			STATE_EDIT_MODE = "STATE_EDIT_MODE",
			STATE_UNDO_REDO = "STATE_UNDO_REDO";

	private static final int
			REQUEST_COMMIT = 42;

	private static final String
			PREFS_NAME = TextEditorActivity.class.getSimpleName();

	private static final String
			KEY_SHOW_LINE_NUMBERS = "KEY_SHOW_LINE_NUMBERS";

	@Inject private ActivityIntentFactory intentFactory;
	@Inject private FileManagerFactory fileManagerFactory;
	@Inject private InputMethodManager inputMethodManager;

	@InjectView(R.id.content) private EditText editText;
	private UndoRedoEditText undoRedoEditText;

	@InjectView(R.id.edit) private FloatingActionButton editButton;
	@InjectView(R.id.line_numbers) private TextView numLinesTextView;

	@Inject private NodeUtils nodeUtils;
	private Repository repository;
	private FileManager fileManager;
	private FileData fileData; // file currently being edited
	private boolean showingLineNumbers;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// load arguments
		final boolean isNewFile = getIntent().getBooleanExtra(EXTRA_IS_NEW_FILE, false);
		repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		fileManager = fileManagerFactory.createFileManager(repository);

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
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

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
		if (savedInstanceState != null && savedInstanceState.getSerializable(STATE_FILE_DATA) != null) {
			editText.post(new Runnable() {
				@Override
				public void run() {
					fileData = (FileData) savedInstanceState.getSerializable(STATE_FILE_DATA);
					boolean startEditMode = savedInstanceState.getBoolean(STATE_EDIT_MODE);
					String restoredText = savedInstanceState.getString(STATE_TEXT);
					showContent(startEditMode, restoredText);
					undoRedoEditText.restoreInstanceState(savedInstanceState, STATE_UNDO_REDO);
				}
			});

		} else {
			loadContent(isNewFile);
		}

	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(STATE_FILE_DATA, fileData);
		outState.putBoolean(STATE_EDIT_MODE, isEditMode());
		outState.putString(STATE_TEXT, editText.getText().toString());
		undoRedoEditText.saveInstanceState(outState, STATE_UNDO_REDO);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.text_editor, menu);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem lineItem = menu.findItem(R.id.action_show_line_numbers);
		if (showingLineNumbers) lineItem.setChecked(true);
		else lineItem.setChecked(false);

		// toggle undo / redo buttons
		if (!isEditMode()) {
			menu.findItem(R.id.action_undo).setVisible(false);
			menu.findItem(R.id.action_redo).setVisible(false);
		}

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

			case R.id.action_undo:
				if (undoRedoEditText.getCanUndo()) {
					undoRedoEditText.undo();
				} else {
					Toast.makeText(this, getString(R.string.nothing_to_undo), Toast.LENGTH_SHORT).show();
				}
				return true;

			case R.id.action_redo:
				if (undoRedoEditText.getCanRedo()) {
					undoRedoEditText.redo();
				} else {
					Toast.makeText(this, getString(R.string.nothing_to_redo), Toast.LENGTH_SHORT).show();
				}
				return true;

			case R.id.action_commit:
				saveFile();
				startActivityForResult(intentFactory.createCommitIntent(repository), REQUEST_COMMIT);
				return true;

			case R.id.action_preview:
				saveFile();
				startActivity(intentFactory.createPreviewIntent(repository));
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_COMMIT:
				if (resultCode != RESULT_OK) return;
				if (isEditMode()) stopEditMode();
				loadContent(false);
		}
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


	private void loadContent(final boolean isNewFile) {
		showSpinner();
		compositeSubscription.add(fileManager.getTree()
				.flatMap(new Func1<DirNode, Observable<FileData>>() {
					@Override
					public Observable<FileData> call(DirNode rootNode) {
						FileNode node = (FileNode) nodeUtils.restoreInstanceState(getIntent().getExtras(), rootNode);

						if (!isNewFile) {
							return fileManager.readFile(node);
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
						showContent(isNewFile, null);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(this, "failed to get file content"))
						.add(new HideSpinnerAction(this))
						.build()));
	}


	private void showContent(boolean startEditMode, String restoredText) {
		setTitle(fileData.getFileNode().getPath());
		try {
			// set text
			if (restoredText != null) editText.setText(restoredText);
			else editText.setText(new String(fileData.getData(), "UTF-8"));
			editText.setTypeface(Typeface.MONOSPACE);

			// setup undo / redo
			undoRedoEditText = new UndoRedoEditText(editText);
			undoRedoEditText.setMaxHistorySize(EDITOR_MAX_HISTORY);

			// start edit mode
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
		invalidateOptionsMenu();
	}


	private void stopEditMode() {
		inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
		editText.setFocusable(false);
		editText.setFocusableInTouchMode(false);
		editButton.setVisibility(View.VISIBLE);
		if (isDirty()) saveFile();
		invalidateOptionsMenu();
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
