package org.faudroids.mrhyde.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Post;

import java.util.List;

import rx.Observable;

public class PostsActivity extends AbstractJekyllActivity<Post> {

	public PostsActivity() {
		super(R.string.posts);
	}


	@Override
	protected void onAddClicked() {
		jekyllUiUtils.showNewPostDialog(jekyllManager, repository);
	}

	@Override
	protected Observable<List<Post>> doLoadItems() {
		return jekyllManager.getAllPosts();
	}

	@Override
	protected AbstractAdapter createAdapter() {
		return new PostsAdapter();
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
			protected void setItem(Post item) {
				jekyllUiUtils.setPostOverview(view, item, repository);
			}

		}
	}

}
