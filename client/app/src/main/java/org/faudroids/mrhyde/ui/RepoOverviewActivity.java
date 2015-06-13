package org.faudroids.mrhyde.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;

@ContentView(R.layout.activity_repo_overview)
public final class RepoOverviewActivity extends AbstractActionBarActivity {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();

	public static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	@InjectView(R.id.header_posts) private View postsHeader;
	@InjectView(R.id.list_posts) private ListView postsListView;
	private PostsListAdapter postsListAdapter;
	@InjectView(R.id.item_no_posts) private View noPostsView;
	@InjectView(R.id.item_add_post) private View addPostView;

	@InjectView(R.id.header_drafts) private View draftsHeader;
	@InjectView(R.id.list_drafts) private ListView draftsListView;
	@InjectView(R.id.item_no_drafts) private View noDraftsView;
	@InjectView(R.id.item_add_draft) private View addDraftView;

	@InjectView(R.id.card_all_files) private View allFilesView;

	@Inject private JekyllManagerFactory jekyllManagerFactory;
	private JekyllManager jekyllManager;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get arguments
		final Repository repository = (Repository) this.getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		jekyllManager = jekyllManagerFactory.createJekyllManager(repository);
		setTitle(repository.getName());

		// setup posts lists
		postsListAdapter = new PostsListAdapter(this);
		postsListView.setAdapter(postsListAdapter);

		// load posts
		showSpinner();
		compositeSubscription.add(jekyllManager.getAllPosts()
				.compose(new DefaultTransformer<List<Post>>())
				.subscribe(new Action1<List<Post>>() {
					@Override
					public void call(List<Post> posts) {
						hideSpinner();

						// setup list
						if (posts.isEmpty()) {
							noPostsView.setVisibility(View.VISIBLE);

						} else {
							noPostsView.setVisibility(View.GONE);

							// get first 3 posts
							List<Post> firstPosts = new ArrayList<>();
							for (int i = 0; i < 3 && i < posts.size(); ++i) {
								firstPosts.add(posts.get(i));
							}
							postsListAdapter.clear();
							postsListAdapter.addAll(firstPosts);
							postsListAdapter.notifyDataSetChanged();
						}
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(RepoOverviewActivity.this, "failed to load posts"))
						.add(new HideSpinnerAction(RepoOverviewActivity.this))
						.build()));

		// setup posts clicks
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
		if (jekyllManager.getAllDrafts().isEmpty()) {
			noDraftsView.setVisibility(View.VISIBLE);
		} else {
			noDraftsView.setVisibility(View.GONE);
		}
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


	private static class PostsListAdapter extends ArrayAdapter<Post> {

		public PostsListAdapter(Context context) {
			super(context, R.layout.item_overview_post);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			Post post = getItem(position);

			// create view
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.item_overview_post, parent, false);

			// set title
			TextView titleView = (TextView) view.findViewById(R.id.text_title);
			titleView.setText(post.getTitle());

			// set date
			TextView dateView = (TextView) view.findViewById(R.id.text_date);
			dateView.setText(DATE_FORMAT.format(post.getDate()));

			return view;
		}
	}

}
