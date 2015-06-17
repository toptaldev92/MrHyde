package org.faudroids.mrhyde.ui;

import android.view.View;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.List;

import rx.functions.Action1;

public class PostsActivity extends AbstractJekyllActivity<Post> {

	public PostsActivity() {
		super(R.string.posts);
	}


	@Override
	protected void onAddClicked() {
		jekyllUiUtils.showNewPostDialog(jekyllManager, repository);
	}

	@Override
	protected void loadItems() {
		compositeSubscription.add(jekyllManager.getAllPosts()
				.compose(new DefaultTransformer<List<Post>>())
				.subscribe(new Action1<List<Post>>() {
					@Override
					public void call(List<Post> posts) {
						if (isSpinnerVisible()) hideSpinner();
						adapter.setItems(posts);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(PostsActivity.this, "failed to load posts"))
						.add(new HideSpinnerAction(PostsActivity.this))
						.build()));
	}

	@Override
	protected AbstractAdapter createAdapter() {
		return new PostsAdapter();
	}


	public class PostsAdapter extends AbstractAdapter {

		@Override
		protected AbstractViewHolder doOnCreateViewHolder(View view) {
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
