package org.faudroids.mrhyde.utils;

import rx.exceptions.OnErrorThrowable;
import rx.functions.Action1;

abstract class AbstractErrorAction implements Action1<Throwable> {

	private Action1<Throwable> nextAction = null;

	public void setNextAction(Action1<Throwable> nextAction) {
		this.nextAction = nextAction;
	}


	@Override
	public void call(Throwable throwable) {
		if (throwable instanceof OnErrorThrowable) {
			call(throwable.getCause());
			return;
		}

		doCall(throwable);
		if (nextAction != null) nextAction.call(throwable);
	}


	protected abstract void doCall(Throwable throwable);

}
