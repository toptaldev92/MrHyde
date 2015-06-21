package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.git.AbstractNode;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileNode;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import timber.log.Timber;

@ContentView(R.layout.dialog_select_dir)
public final class SelectDirActivity extends AbstractDirActivity {

	static final String EXTRA_SELECTED_DIR = "EXTRA_SELECTED_DIR";	// part of result of this activity

	@InjectView(R.id.back) private View backView;
	@InjectView(R.id.cancel) private View cancelView;
	@InjectView(R.id.confirm) private View confirmView;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// hide action bar
		getSupportActionBar().hide();

		// make dialog fill screen
		getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		// setup buttons
		backView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		cancelView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		confirmView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// return result
				for (String key : getIntent().getExtras().keySet()) Timber.d("found key " + key);
				Intent resultIntent = new Intent(getIntent());
				for (String key : resultIntent.getExtras().keySet()) Timber.d("found key " + key);
				nodeUtils.saveNode(EXTRA_SELECTED_DIR, resultIntent, pathNodeAdapter.getSelectedNode());
				for (String key : getIntent().getExtras().keySet()) Timber.d("found key " + key);
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		});
	}


	@Override
	protected PathNodeAdapter createAdapter() {
		return new AlphaPathNodeAdapter();
	}


	@Override
	protected void onDirSelected(DirNode node) {
		// nothing to do
	}


	@Override
	protected void onFileSelected(FileNode node) {
		// nothing to do
	}


	public class AlphaPathNodeAdapter extends PathNodeAdapter {

		@Override
		public AlphaPathNodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
			return new AlphaPathNodeViewHolder(view);
		}


		public class AlphaPathNodeViewHolder extends PathNodeAdapter.PathNodeViewHolder {

			public AlphaPathNodeViewHolder(View view) {
				super(view);
			}

			@Override
			public void setViewForNode(final AbstractNode pathNode) {
				super.setViewForNode(pathNode);

				// reduce alpha for files
				float alpha = (pathNode instanceof DirNode) ? 1f : 0.3f;
				iconView.setAlpha(alpha);
				titleView.setAlpha(alpha);
			}
		}
	}

}
