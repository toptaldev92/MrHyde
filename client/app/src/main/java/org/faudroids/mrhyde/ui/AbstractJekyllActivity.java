package org.faudroids.mrhyde.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.getbase.floatingactionbutton.AddFloatingActionButton;

import org.eclipse.egit.github.core.Repository;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.jekyll.JekyllManager;
import org.faudroids.mrhyde.jekyll.JekyllManagerFactory;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.ui.utils.DividerItemDecoration;
import org.faudroids.mrhyde.ui.utils.JekyllUiUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_posts_or_drafts)
abstract class AbstractJekyllActivity<T> extends AbstractActionBarActivity {

	private static final int REQUEST_COMMIT = 42;

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";


	@InjectView(R.id.list) private RecyclerView recyclerView;
	protected AbstractAdapter adapter;
	@Inject JekyllUiUtils jekyllUiUtils;

	@InjectView(R.id.add) AddFloatingActionButton addButton;

	protected Repository repository;
	@Inject private JekyllManagerFactory jekyllManagerFactory;
	protected JekyllManager jekyllManager;

	@Inject private ActivityIntentFactory intentFactory;

	private final int titleStringResource;

	AbstractJekyllActivity(int titleStringResource) {
		this.titleStringResource = titleStringResource;
	}


	protected abstract void onAddClicked();
	protected abstract void loadItems();
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
				onAddClicked();
			}
		});

		// load posts
		loadItems();
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


		abstract class AbstractViewHolder extends RecyclerView.ViewHolder {

			protected final View view;

			public AbstractViewHolder(View view) {
				super(view);
				this.view = view;
			}

			protected abstract void setItem(T item);
		}
	}

}
