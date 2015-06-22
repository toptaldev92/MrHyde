package org.faudroids.mrhyde.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.getbase.floatingactionbutton.AddFloatingActionButton;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.AbstractJekyllContent;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.DividerItemDecoration;
import org.faudroids.mrhyde.ui.utils.JekyllUiUtils;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;

@ContentView(R.layout.activity_posts_or_drafts)
abstract class AbstractJekyllActivity<T extends AbstractJekyllContent & Comparable<T>>
		extends AbstractActionBarActivity
		implements JekyllActionModeListener.ActionSelectionListener<T>  {

	private static final int REQUEST_COMMIT = 42;

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";


	@InjectView(R.id.list) private RecyclerView recyclerView;
	protected AbstractAdapter adapter;
	@Inject protected JekyllUiUtils jekyllUiUtils;

	@InjectView(R.id.empty) private TextView emptyView;
	@InjectView(R.id.add) private AddFloatingActionButton addButton;

	protected Repository repository;
	@Inject private JekyllManagerFactory jekyllManagerFactory;
	protected JekyllManager jekyllManager;

	@Inject private ActivityIntentFactory intentFactory;

	private JekyllActionModeListener<T> actionModeListener;

	private final int titleStringResource;
	private final int emptyStringResource;
	private final int moveActionStringResource;

	AbstractJekyllActivity(int titleStringResource, int emptyStringResource, int moveActionStringResource) {
		this.titleStringResource = titleStringResource;
		this.emptyStringResource = emptyStringResource;
		this.moveActionStringResource = moveActionStringResource;
	}


	protected abstract void onAddClicked(JekyllUiUtils.OnContentCreatedListener<T> contentListener);
	protected abstract Observable<List<T>> doLoadItems();
	protected abstract AbstractAdapter createAdapter();


	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get arguments
		repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		jekyllManager = jekyllManagerFactory.createJekyllManager(repository);

		// set title
		setTitle(getString(titleStringResource));

		// setup list
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
		adapter = createAdapter();
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setAdapter(adapter);
		recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

		// setup add
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				actionModeListener.stopActionMode();
				onAddClicked(new JekyllUiUtils.OnContentCreatedListener<T>() {
					@Override
					public void onContentCreated(T newItem) {
						adapter.addItem(newItem);
					}
				});
			}
		});

		// prepare action mode
		actionModeListener = new JekyllActionModeListener<>(this, this, moveActionStringResource);

		// load posts
		loadItems();
	}


	private void loadItems() {
		compositeSubscription.add(doLoadItems()
				.compose(new DefaultTransformer<List<T>>())
				.subscribe(new Action1<List<T>>() {
					@Override
					public void call(List<T> items) {
						if (isSpinnerVisible()) hideSpinner();
						adapter.setItems(items);
						if (items.isEmpty()) {
							emptyView.setText(getString(emptyStringResource));
							emptyView.setVisibility(View.VISIBLE);
						} else {
							emptyView.setVisibility(View.GONE);
						}
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(AbstractJekyllActivity.this, "failed to load content"))
						.add(new HideSpinnerAction(AbstractJekyllActivity.this))
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
				loadItems();
		}
	}


	@Override
	public void onDelete(final T item) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.delete_title)
				.setMessage(getString(R.string.delete_message, item.getFileNode().getPath()))
				.setPositiveButton(getString(R.string.action_delete), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showSpinner();
						compositeSubscription.add(jekyllManager.deleteContent(item)
								.compose(new DefaultTransformer<Void>())
								.subscribe(new Action1<Void>() {
									@Override
									public void call(Void aVoid) {
										hideSpinner();
										loadItems();
									}
								}, new ErrorActionBuilder()
										.add(new DefaultErrorAction(AbstractJekyllActivity.this, "failed to delete file"))
										.add(new HideSpinnerAction(AbstractJekyllActivity.this))
										.build()));
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}


	@Override
	public void onEdit(T item) {
		startActivity(intentFactory.createTextEditorIntent(repository, item.getFileNode(), false));
	}


	@Override
	public void onStopActionMode() {
		adapter.notifyDataSetChanged();
	}


	abstract class AbstractAdapter extends RecyclerView.Adapter<AbstractAdapter.AbstractViewHolder> {

		private final List<T> itemsList = new ArrayList<>();

		@Override
		public void onBindViewHolder(AbstractViewHolder holder, int position) {
			holder.setItem(itemsList.get(position));
		}

		@Override
		public int getItemCount() {
			return itemsList.size();
		}

		public void setItems(List<T> itemsList) {
			this.itemsList.clear();
			this.itemsList.addAll(itemsList);
			notifyDataSetChanged();
		}

		public void addItem(T item) {
			itemsList.add(item);
			Collections.sort(itemsList);
			notifyDataSetChanged();
		}

		public void removeItem(T item) {
			itemsList.remove(item);
			notifyDataSetChanged();
		}


		abstract class AbstractViewHolder extends RecyclerView.ViewHolder {

			protected final View view;

			public AbstractViewHolder(View view) {
				super(view);
				this.view = view;
			}

			public void setItem(final T item) {
				// set on click
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						actionModeListener.stopActionMode();
						startActivity(intentFactory.createTextEditorIntent(repository, item.getFileNode(), false));
					}
				});

				// set long click starts action mode
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						if (actionModeListener.startActionMode(item)) {
							v.setSelected(true);
						}
						return true;
					}
				});

				// check if item is selected
				if (item.equals(actionModeListener.getSelectedItem())) {
					view.setSelected(true);
				} else {
					view.setSelected(false);
				}

				doSetItem(item);
			}

			protected abstract void doSetItem(T item);
		}
	}

}
