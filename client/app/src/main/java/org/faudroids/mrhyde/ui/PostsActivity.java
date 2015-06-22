package org.faudroids.mrhyde.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.JekyllUiUtils;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;

import java.util.List;

import rx.Observable;
import rx.functions.Action1;

public class PostsActivity extends AbstractJekyllActivity<Post> {

	public PostsActivity() {
		super(R.string.posts, R.string.no_posts, R.string.action_unpublish_post);
	}

	@Override
	protected void onAddClicked(JekyllUiUtils.OnContentCreatedListener<Post> contentListener) {
		jekyllUiUtils.showNewPostDialog(jekyllManager, repository, contentListener);
	}

	@Override
	protected Observable<List<Post>> doLoadItems() {
		return jekyllManager.getAllPosts();
	}

	@Override
	protected AbstractAdapter createAdapter() {
		return new PostsAdapter();
	}

	@Override
	public void onMove(final Post post) {
		jekyllManager.unpublishPost(post)
				.compose(new DefaultTransformer<Draft>())
				.subscribe(new Action1<Draft>() {
					@Override
					public void call(Draft draft) {
						adapter.removeItem(post);
						Toast.makeText(PostsActivity.this, getString(R.string.post_unpublished), Toast.LENGTH_SHORT).show();
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(PostsActivity.this, "failed to unpublish post"))
						.build());
	}

	public class PostsAdapter extends AbstractAdapter {

		@Override
		public AbstractViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_overview_post, parent, false);
			return new PostViewHolder(view);
		}

		public class PostViewHolder extends AbstractViewHolder {

			public PostViewHolder(View view) {
				super(view);
			}

			@Override
			protected void doSetItem(Post item) {
				jekyllUiUtils.setPostOverview(view, item, repository);
			}

		}
	}

}
