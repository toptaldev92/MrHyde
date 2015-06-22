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

public class DraftsActivity extends AbstractJekyllActivity<Draft> {

	public DraftsActivity() {
		super(R.string.drafts, R.string.action_publish_draft);
	}

	@Override
	protected void onAddClicked(JekyllUiUtils.OnContentCreatedListener<Draft> contentListener) {
		jekyllUiUtils.showNewDraftDialog(jekyllManager, repository, contentListener);
	}

	@Override
	protected Observable<List<Draft>> doLoadItems() {
		return jekyllManager.getAllDrafts();
	}

	@Override
	protected AbstractAdapter createAdapter() {
		return new DraftsAdapter();
	}

	@Override
	protected int getEmptyStringResource() {
		return R.string.no_drafts;
	}

	@Override
	public void onMove(final Draft draft) {
		jekyllManager.publishDraft(draft)
				.compose(new DefaultTransformer<Post>())
				.subscribe(new Action1<Post>() {
					@Override
					public void call(Post post) {
						adapter.removeItem(draft);
						Toast.makeText(DraftsActivity.this, getString(R.string.draft_published), Toast.LENGTH_SHORT).show();
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(DraftsActivity.this, "failed to publish draft"))
						.build());
	}

	public class DraftsAdapter extends AbstractAdapter {

		@Override
		public AbstractViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_overview_draft, parent, false);
			return new DraftViewHolder(view);
		}

		public class DraftViewHolder extends AbstractViewHolder {

			public DraftViewHolder(View view) {
				super(view);
			}

			@Override
			protected void doSetItem(Draft item) {
				jekyllUiUtils.setDraftOverview(view, item, repository);
			}

		}
	}

}
