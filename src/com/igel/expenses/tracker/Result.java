package com.igel.expenses.tracker;


public class Result<T> {
	
	private int mMessageId;
	private T mResult;
	private Object[] mMessageArgs;	
	
	public Result(T result) {
		mResult = result;
	}
	
	public Result(T result, int messageId, Object ... messageArgs) {
		mResult = result;
		mMessageId = messageId;
		mMessageArgs = messageArgs;
	}
	
	public T getResult() {
		return mResult;
	}
	
	public int getMessageId() {
		return mMessageId;
	}
	
	public Object[] getMessageArgs() {
		return mMessageArgs;
	}
}
