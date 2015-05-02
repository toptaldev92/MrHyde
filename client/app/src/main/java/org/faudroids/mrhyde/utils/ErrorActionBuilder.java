package org.faudroids.mrhyde.utils;

import rx.functions.Action1;

public class ErrorActionBuilder {

	private AbstractErrorAction firstAction, lastAction;


	public ErrorActionBuilder add(AbstractErrorAction errorAction) {
		if (firstAction == null) {
			firstAction = errorAction;
			lastAction = errorAction;
		} else {
			lastAction.setNextAction(errorAction);
			lastAction = errorAction;
		}
		return this;
	}


	public Action1<Throwable> build() {
		return firstAction;
	}

}
