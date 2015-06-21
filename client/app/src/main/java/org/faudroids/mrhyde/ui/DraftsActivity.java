package org.faudroids.mrhyde.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.Draft;

import java.util.List;

import rx.Observable;

public class DraftsActivity extends AbstractJekyllActivity<Draft> {

	public DraftsActivity() {
		super(R.string.drafts);
	}

	@Override
	protected void onAddClicked() {
		jekyllUiUtils.showNewDraftDialog(jekyllManager, repository);
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
