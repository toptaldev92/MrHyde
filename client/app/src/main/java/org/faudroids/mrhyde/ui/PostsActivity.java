package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.getbase.floatingactionbutton.AddFloatingActionButton;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.DividerItemDecoration;
import org.faudroids.mrhyde.ui.utils.JekyllUiUtils;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;

@ContentView(R.layout.activity_posts_or_drafts)
public final class PostsActivity extends AbstractActionBarActivity {

	private static final int REQUEST_COMMIT = 42;

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";


	@InjectView(R.id.list) private RecyclerView recyclerView;
	private PostsAdapter postsAdapter;
	@Inject JekyllUiUtils jekyllUiUtils;

	@InjectView(R.id.add) AddFloatingActionButton addButton;

	private Repository repository;
	@Inject private JekyllManagerFactory jekyllManagerFactory;
	private JekyllManager jekyllManager;

	@Inject private ActivityIntentFactory intentFactory;


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get arguments
		repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		jekyllManager = jekyllManagerFactory.createJekyllManager(repository);

		// set title
		setTitle(getString(R.string.posts));

		// setup list
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
		postsAdapter = new PostsAdapter();
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(postsAdapter);
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

		// setpu add
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				jekyllUiUtils.showNewPostDialog(jekyllManager, repository);
			}
		});

		// load posts
		loadPosts();
	}


	private void loadPosts() {
		compositeSubscription.add(jekyllManager.getAllPosts()
				.compose(new DefaultTransformer<List<Post>>())
				.subscribe(new Action1<List<Post>>() {
					@Override
					public void call(List<Post> posts) {
						if (isSpinnerVisible()) hideSpinner();
						postsAdapter.setPosts(posts);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(PostsActivity.this, "failed to load posts"))
						.add(new HideSpinnerAction(PostsActivity.this))
						.build()));
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.posts, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO
		switch (item.getItemId()) {
			case R.id.action_commit:
				startActivityForResult(intentFactory.createCommitIntent(repository), REQUEST_COMMIT);
				return true;

			case R.id.action_preview:
				startActivity(intentFactory.createPreviewIntent(repository));
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onActivityResult(int request, int result, Intent data) {
		switch (request) {
			case REQUEST_COMMIT:
				if (result != RESULT_OK) return;
				showSpinner();
				loadPosts();
		}
	}


	public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

		private final List<Post> postsList = new ArrayList<>();

		@Override
		public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_overview_post, parent, false);
			return new PostViewHolder(view);
		}

		@Override
		public void onBindViewHolder(PostViewHolder holder, int position) {
			holder.setPost(postsList.get(position));
		}

		@Override
		public int getItemCount() {
			return postsList.size();
		}

		public void setPosts(List<Post> postsList) {
			this.postsList.clear();
			this.postsList.addAll(postsList);
			notifyDataSetChanged();
		}


		public class PostViewHolder extends RecyclerView.ViewHolder {

			private final View view;

			public PostViewHolder(View view) {
				super(view);
				this.view = view;
			}

			public void setPost(Post post) {
				jekyllUiUtils.setPostOverview(view, post, repository);
			}
		}
	}

}
