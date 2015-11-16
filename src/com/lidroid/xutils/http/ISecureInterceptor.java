package com.lidroid.xutils.http;

/**
 * @author andrewlu
 * 用于在请求发送前对参数做额外处理.通常用来对参数进行加密.
 */
import com.lidroid.xutils.http.client.HttpRequest;

public interface ISecureInterceptor {

	public RequestParams preSend(HttpRequest request, RequestParams params);

}
