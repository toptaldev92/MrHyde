package org.faudroids.mrhyde.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.faudroids.mrhyde.github.ApiWrapper;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public final class DirFragment extends AbstractListFragment {

	private static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	public static DirFragment createInstance(Repository repository) {
		DirFragment fragment = new DirFragment();
		Bundle extras = new Bundle();
		extras.putSerializable(EXTRA_REPOSITORY, repository);
		fragment.setArguments(extras);
		return fragment;
	}



	@Inject ApiWrapper apiWrapper;

	private FilesListAdapter listAdapter;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		listAdapter = new FilesListAdapter();
		setListAdapter(listAdapter);

		final Repository repository = (Repository) getArguments().getSerializable(EXTRA_REPOSITORY);

		apiWrapper.getCommits(repository)
				.flatMap(new Func1<List<RepositoryCommit>, Observable<Tree>>() {
					@Override
					public Observable<Tree> call(List<RepositoryCommit> repositoryCommits) {
						String sha = repositoryCommits.get(0).getSha();
						return apiWrapper.getTree(repository, sha, true);
					}
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Tree>() {
					@Override
					public void call(Tree tree) {
						listAdapter.setItems(tree.getTree());
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable throwable) {
						Toast.makeText(getActivity(), "That didn't work, check log", Toast.LENGTH_LONG).show();
						Timber.e(throwable, "failed to get content");
					}
				});
	}


	private final class FilesListAdapter extends AbstractListAdapter<TreeEntry> {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).getPath());
			return view;
		}

	}


}
