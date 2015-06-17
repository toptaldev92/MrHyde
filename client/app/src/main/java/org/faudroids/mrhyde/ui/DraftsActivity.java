package org.faudroids.mrhyde.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.List;

import rx.functions.Action1;

public class DraftsActivity extends AbstractJekyllActivity<Draft> {

	public DraftsActivity() {
		super(R.string.drafts);
	}

	@Override
	protected void onAddClicked() {
		jekyllUiUtils.showNewDraftDialog();
	}

	@Override
	protected void loadItems() {
		compositeSubscription.add(jekyllManager.getAllDrafts()
				.compose(new DefaultTransformer<List<Draft>>())
				.subscribe(new Action1<List<Draft>>() {
					@Override
					public void call(List<Draft> drafts) {
						if (isSpinnerVisible()) hideSpinner();
						adapter.setItems(drafts);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(DraftsActivity.this, "failed to load drafts"))
						.add(new HideSpinnerAction(DraftsActivity.this))
						.build()));
	}

	@Override
	protected AbstractAdapter createAdapter() {
		return new DraftsAdapter();
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
			protected void setItem(Draft item) {
				jekyllUiUtils.setDraftOverview(view, item, repository);
			}

		}
	}

}
