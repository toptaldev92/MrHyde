package org.faudroids.mrhyde.ui.utils;

import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.widget.EditText;

import java.util.LinkedList;


/**
 * Handles undo / redo operations on a {@link EditText}.
 * Courtesy (mostly) to https://code.google.com/p/android/issues/attachmentText?id=6458&aid=64580123000&name=TextViewUndoRedo.java
 */
public class UndoRedoEditText {

	/**
	 * Is undo/redo being performed? This member signals if an undo/redo
	 * operation is currently being performed. Changes in the text during
	 * undo/redo are not recorded because it would mess up the undo history.
	 */
	private boolean isUndoRedoRunning = false;

	/**
	 * The edit history.
	 */
	private EditHistory editHistory;

	/**
	 * The change listener.
	 */
	private EditTextChangeListener textChangeListener;

	/**
	 * The edit text.
	 */
	private EditText editText;

	// =================================================================== //

	/**
	 * Create a new TextViewUndoRedo and attach it to the specified TextView.
	 */
	public UndoRedoEditText(EditText editText) {
		this.editText = editText;
		this.editHistory = new EditHistory();
		this.textChangeListener = new EditTextChangeListener();
		this.editText.addTextChangedListener(textChangeListener);
	}

	// =================================================================== //

	/**
	 * Disconnect this undo/redo from the text view.
	 */
	public void disconnect() {
		editText.removeTextChangedListener(textChangeListener);
	}

	/**
	 * Set the maximum history size. If size is negative, then history size is
	 * only limited by the device memory.
	 */
	public void setMaxHistorySize(int maxHistorySize) {
		editHistory.setMaxHistorySize(maxHistorySize);
	}

	/**
	 * Clear history.
	 */
	public void clearHistory() {
		editHistory.clear();
	}

	/**
	 * Can undo be performed?
	 */
	public boolean getCanUndo() {
		return (editHistory.mmPosition > 0);
	}

	/**
	 * Perform undo.
	 */
	public void undo() {
		EditItem edit = editHistory.getPrevious();
		if (edit == null) {
			return;
		}

		Editable text = editText.getEditableText();
		int start = edit.mmStart;
		int end = start + (edit.mmAfter != null ? edit.mmAfter.length() : 0);

		isUndoRedoRunning = true;
		text.replace(start, end, edit.mmBefore);
		isUndoRedoRunning = false;

		// This will get rid of underlines inserted when editor tries to come
		// up with a suggestion.
		for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
			text.removeSpan(o);
		}

		Selection.setSelection(text, edit.mmBefore == null ? start
				: (start + edit.mmBefore.length()));
	}

	/**
	 * Can redo be performed?
	 */
	public boolean getCanRedo() {
		return (editHistory.mmPosition < editHistory.mmHistory.size());
	}

	/**
	 * Perform redo.
	 */
	public void redo() {
		EditItem edit = editHistory.getNext();
		if (edit == null) {
			return;
		}

		Editable text = editText.getEditableText();
		int start = edit.mmStart;
		int end = start + (edit.mmBefore != null ? edit.mmBefore.length() : 0);

		isUndoRedoRunning = true;
		text.replace(start, end, edit.mmAfter);
		isUndoRedoRunning = false;

		// This will get rid of underlines inserted when editor tries to come
		// up with a suggestion.
		for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
			text.removeSpan(o);
		}

		Selection.setSelection(text, edit.mmAfter == null ? start
				: (start + edit.mmAfter.length()));
	}


	public void saveInstanceState(Bundle outState, String prefix) {
		outState.putInt(prefix + ".maxSize", editHistory.mmMaxHistorySize);
		outState.putInt(prefix + ".position", editHistory.mmPosition);
		outState.putInt(prefix + ".size", editHistory.mmHistory.size());

		int i = 0;
		for (EditItem ei : editHistory.mmHistory) {
			String pre = prefix + "." + i;

			outState.putInt(pre + ".start", ei.mmStart);
			outState.putString(pre + ".before", ei.mmBefore.toString());
			outState.putString(pre + ".after", ei.mmAfter.toString());
			i++;
		}
	}


	/**
	 * Restore preferences.
	 *
	 * @param prefix
	 *            The preference key prefix used when state was stored.
	 * @return did restore succeed? If this is false, the undo history will be
	 *         empty.
	 */
	public boolean restoreInstanceState(Bundle inState, String prefix) {
		boolean ok = doRestoreInstanceState(inState, prefix);
		if (!ok) {
			editHistory.clear();
		}
		return ok;
	}

	private boolean doRestoreInstanceState(Bundle inState, String prefix) {
		editHistory.clear();
		editHistory.mmMaxHistorySize = inState.getInt(prefix + ".maxSize", -1);

		int count = inState.getInt(prefix + ".size", -1);
		if (count == -1) {
			return false;
		}

		for (int i = 0; i < count; i++) {
			String pre = prefix + "." + i;

			int start = inState.getInt(pre + ".start", -1);
			String before = inState.getString(pre + ".before", null);
			String after = inState.getString(pre + ".after", null);

			if (start == -1 || before == null || after == null) {
				return false;
			}
			editHistory.add(new EditItem(start, before, after));
		}

		editHistory.mmPosition = inState.getInt(prefix + ".position", -1);
		if (editHistory.mmPosition == -1) {
			return false;
		}

		return true;
	}

	// =================================================================== //

	/**
	 * Keeps track of all the edit history of a text.
	 */
	private final class EditHistory {

		/**
		 * The position from which an EditItem will be retrieved when getNext()
		 * is called. If getPrevious() has not been called, this has the same
		 * value as mmHistory.size().
		 */
		private int mmPosition = 0;

		/**
		 * Maximum undo history size.
		 */
		private int mmMaxHistorySize = -1;

		/**
		 * The list of edits in chronological order.
		 */
		private final LinkedList<EditItem> mmHistory = new LinkedList<EditItem>();

		/**
		 * Clear history.
		 */
		private void clear() {
			mmPosition = 0;
			mmHistory.clear();
		}

		/**
		 * Adds a new edit operation to the history at the current position. If
		 * executed after a call to getPrevious() removes all the future history
		 * (elements with positions >= current history position).
		 */
		private void add(EditItem item) {
			while (mmHistory.size() > mmPosition) {
				mmHistory.removeLast();
			}
			mmHistory.add(item);
			mmPosition++;

			if (mmMaxHistorySize >= 0) {
				trimHistory();
			}
		}

		/**
		 * Set the maximum history size. If size is negative, then history size
		 * is only limited by the device memory.
		 */
		private void setMaxHistorySize(int maxHistorySize) {
			mmMaxHistorySize = maxHistorySize;
			if (mmMaxHistorySize >= 0) {
				trimHistory();
			}
		}

		/**
		 * Trim history when it exceeds max history size.
		 */
		private void trimHistory() {
			while (mmHistory.size() > mmMaxHistorySize) {
				mmHistory.removeFirst();
				mmPosition--;
			}

			if (mmPosition < 0) {
				mmPosition = 0;
			}
		}

		/**
		 * Traverses the history backward by one position, returns and item at
		 * that position.
		 */
		private EditItem getPrevious() {
			if (mmPosition == 0) {
				return null;
			}
			mmPosition--;
			return mmHistory.get(mmPosition);
		}

		/**
		 * Traverses the history forward by one position, returns and item at
		 * that position.
		 */
		private EditItem getNext() {
			if (mmPosition >= mmHistory.size()) {
				return null;
			}

			EditItem item = mmHistory.get(mmPosition);
			mmPosition++;
			return item;
		}

		@Override
		public String toString() {
			String value = mmPosition + " (" + mmMaxHistorySize;
			for (EditItem item : mmHistory) {
				value += "\n" + item;
			}
			return value;
		}
	}

	/**
	 * Represents the changes performed by a single edit operation.
	 */
	private final class EditItem {
		private final int mmStart;
		private final CharSequence mmBefore;
		private final CharSequence mmAfter;

		/**
		 * Constructs EditItem of a modification that was applied at position
		 * start and replaced CharSequence before with CharSequence after.
		 */
		public EditItem(int start, CharSequence before, CharSequence after) {
			mmStart = start;
			mmBefore = before;
			mmAfter = after;
		}


		@Override
		public String toString() {
			return mmStart + " " + mmBefore + " -> " + mmAfter;
		}
	}

	/**
	 * Class that listens to changes in the text.
	 */
	private final class EditTextChangeListener implements TextWatcher {

		/**
		 * The text that will be removed by the change event.
		 */
		private CharSequence mBeforeChange;

		/**
		 * The text that was inserted by the change event.
		 */
		private CharSequence mAfterChange;

		public void beforeTextChanged(CharSequence s, int start, int count,
									  int after) {
			if (isUndoRedoRunning) {
				return;
			}

			mBeforeChange = s.subSequence(start, start + count);
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (isUndoRedoRunning) {
				return;
			}

			mAfterChange = s.subSequence(start, start + count);
			editHistory.add(new EditItem(start, mBeforeChange, mAfterChange));
		}

		public void afterTextChanged(Editable s) {
		}
	}
}


