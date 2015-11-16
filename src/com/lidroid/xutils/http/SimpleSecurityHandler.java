/**
 * 
 */
package com.lidroid.xutils.http;

import java.util.Date;

import com.lidroid.xutils.http.client.HttpRequest;

/**
 * @author andrewlu
 * 
 */
public class SimpleSecurityHandler implements ISecureInterceptor {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lidroid.xutils.http.ISecureInterceptor#preSend(com.lidroid.xutils
	 * .http.client.HttpRequest, com.lidroid.xutils.http.RequestParams)
	 */
	@Override
	public RequestParams preSend(HttpRequest request, RequestParams params) {
		// TODO Auto-generated method stub
		params.addHeader("Date", "" + System.currentTimeMillis());
		return params;
	}

}
