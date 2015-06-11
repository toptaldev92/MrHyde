package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_repo_overview)
public final class RepoOverviewActivity extends AbstractActionBarActivity {

	public static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	@InjectView(R.id.header_posts) private View postsHeader;
	@InjectView(R.id.item_add_post) private View addPostView;

	@InjectView(R.id.header_drafts) private View draftsHeader;
	@InjectView(R.id.item_add_draft) private View addDraftView;

	@InjectView(R.id.card_all_files) private View allFilesView;

	private Repository repository;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get arguments
		repository = (Repository) this.getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		setTitle(repository.getName());

		// setup posts card
		postsHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(RepoOverviewActivity.this, "Dummy", Toast.LENGTH_SHORT).show();
			}
		});
		addPostView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(RepoOverviewActivity.this, "Dummy", Toast.LENGTH_SHORT).show();
			}
		});

		// setup drafts card
		// setup posts card
		draftsHeader.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(RepoOverviewActivity.this, "Dummy", Toast.LENGTH_SHORT).show();
			}
		});
		addDraftView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(RepoOverviewActivity.this, "Dummy", Toast.LENGTH_SHORT).show();
			}
		});

		// setup all files card
		allFilesView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RepoOverviewActivity.this, DirActivity.class);
				intent.putExtra(DirActivity.EXTRA_REPOSITORY, repository);
				startActivity(intent);
			}
		});
	}

}
