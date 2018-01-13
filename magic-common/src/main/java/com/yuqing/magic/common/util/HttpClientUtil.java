package com.yuqing.magic.common.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

/**
 * HTTP客户端工具，提供同步请求和异步请求
 * <p>
 *     1.同步调用 HttpClientUtil.get(url);
 * </p>
 * <p>
 *     <code>
 *     2.异步调用 Future<String> result = HttpClientUtil.getAsync(url);
 *     result.get();
 *     </code>
 * </p>
 * @author yuqing
 *
 * @since 1.0.1
 */
public class HttpClientUtil {

    public static final String GET = "GET";

    public static final String POST = "POST";

    public static final String PUT = "PUT";

    public static final String DELETE = "DELETE";

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    public static final int SHORT_TIMEOUT = 5000;

    public static final int DEFAULT_TIMEOUT = 30000;

    public static final int LONG_TIMEOUT = 60000;

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final int BRIEF_LENGTH = 32;

    public static final String TAILING = "......";

    private static boolean hasUnsafeClass;

    private static Object unsafeObject;

    private static long hOffset;

    private static CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault();

    private static CloseableHttpClient httpClient = HttpClients.createDefault();

    static {
        httpAsyncClient.start();
        try {
            hasUnsafeClass = ReflectionUtil.hasClass(ReflectionUtil.SUN_MISC_UNSAFE);
            unsafeObject = ReflectionUtil.getUnsafe();
            if (unsafeObject != null) {
                hOffset = ReflectionUtil.getOffset(Proxy.class, "h");
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            hasUnsafeClass = false;
        }
    }

    private static class MyHttpEntityWrapper extends HttpEntityWrapper {

        private byte[] dataArray;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        /**
         * Creates a new entity wrapper.
         *
         * @param wrappedEntity
         */
        public MyHttpEntityWrapper(HttpEntity wrappedEntity) {
            super(wrappedEntity);
        }

        public long getContentLength2() {
            long len = getContentLength();
            if (len >= 0) {
                return len;
            }

            if (dataArray != null) {
                return dataArray.length;
            }

            // 读取数据
            try {
                dataArray = EntityUtils.toByteArray(this);
            } catch (IOException e) {
                return len;
            }

            if (dataArray != null) {
                return dataArray.length;
            }
            return len;
        }

        @Override
        public InputStream getContent() throws IOException {
            if (dataArray == null && outputStream.size() > 0) {
                dataArray = outputStream.toByteArray();
            }
            if (dataArray != null) {
                return new ByteArrayInputStream(dataArray);
            }
            return new TeeInputStream(super.getContent(), outputStream);
        }
    }

    private static class HttpResponseHandler implements InvocationHandler {

        private HttpResponse target;

        private Object httpClient;

        private volatile long startTime;

        private volatile long endTime;

        private volatile boolean closeHttpClient;

        public HttpResponseHandler(Object httpClient,
                                   HttpResponse target) {
            this.target = target;
            this.httpClient = httpClient;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object ret = null;
            if (method.getName().equals("close")) {
                try {
                    ret = method.invoke(target, args);
                } finally {
                    if (closeHttpClient && httpClient != null) {
                        if (httpClient instanceof CloseableHttpClient) {
                            ((CloseableHttpClient) httpClient).close();
                        } else if (httpClient instanceof CloseableHttpAsyncClient) {
                            ((CloseableHttpAsyncClient) httpClient).close();
                        }
                    }
                }
            } else {
                ret = method.invoke(target, args);
            }
            return ret;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public boolean isCloseHttpClient() {
            return closeHttpClient;
        }

        public void setCloseHttpClient(boolean closeHttpClient) {
            this.closeHttpClient = closeHttpClient;
        }

        public long getCostTime() {
            return endTime - startTime;
        }
    }

    private static class FutureCallbackProxy implements InvocationHandler {

        private FutureCallback target;

        private HttpUriRequest request;

        private long start = System.currentTimeMillis();

        private volatile long end = start;

        public FutureCallbackProxy(FutureCallback target,
                                   HttpUriRequest request) {
            this.target = target;
            this.request = request;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("completed")) {
                end = System.currentTimeMillis();
                Object param1 = args != null && args.length > 0 ? args[0] : null;
                if (param1 != null && param1 instanceof HttpResponse) {
                    HttpResponse result = (HttpResponse) param1;
                    try {
                        wrapResponseEntity(result);
                        result = proxyHttpResponse(null, result, HttpResponse.class);
                        args[0] = result;

                        HttpResponseHandler handler = getHttpResponseHandler(result);
                        if (handler != null) handler.setStartTime(start);
                        if (handler != null) handler.setEndTime(end);
                        if (handler != null) handler.setCloseHttpClient(false);

                        String brief = getContentString(result, BRIEF_LENGTH, TAILING, DEFAULT_CHARSET);

                        logger.info("[sendAsync][{}ms]{} {} -> {} {} {}",
                                getCostTime(),
                                request.getMethod(), request.getURI().toString(),
                                getStatusCode(result),
                                getContentLength(result),
                                brief);

                        return method.invoke(target, args);
                    } finally {
                        if (result instanceof CloseableHttpResponse) {
                            try {
                                ((CloseableHttpResponse) result).close();
                            } catch (IOException e) {
                                logger.error("", e);
                            }
                        }
                    }
                }
            }

            return method.invoke(target, args);
        }

        private long getCostTime() {
            return end - start;
        }
    }

    private static class FutureBody implements Future<String> {

        private volatile String body;

        private volatile boolean done;

        private CountDownLatch latch = new CountDownLatch(1);

        private void setDone(boolean done) {
            this.done = done;
            if (done) {
                latch.countDown();
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public String get() throws InterruptedException, ExecutionException {
            latch.await();
            return body;
        }

        @Override
        public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            latch.await(timeout, unit);
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    /**
     * 同步GET请求
     * @param url
     * @return
     */
    public static String get(String url) {
        return get(url, null);
    }

    /**
     * 同步GET请求
     * @param url
     * @param headers
     * @return
     */
    public static String get(String url, Map<String, String> headers) {
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = send(url,
                    GET,
                    headers,
                    null,
                    SHORT_TIMEOUT,
                    DEFAULT_CHARSET);
            return getContentString(httpResponse, 0, null, DEFAULT_CHARSET);
        } catch (IOException e) {
            logger.error(GET + " " + url, e);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    logger.error(GET + " " + url, e);
                }
            }
        }
        return null;
    }

    /**
     * 同步POST请求
     * @param url
     * @param headers
     * @param data 如果data为string，系统将装换为字节，如果data为Map，系统将拼接成form表单格式，再转换为字节，如果为其他类型，转换为json字符串，再转换为字节
     * @return
     */
    public static String post(String url, Map<String, String> headers, Object data) {
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = send(url,
                    POST,
                    headers,
                    serializeForEntity(data, DEFAULT_CHARSET, false),
                    SHORT_TIMEOUT,
                    DEFAULT_CHARSET);
            return getContentString(httpResponse, 0, null, DEFAULT_CHARSET);
        } catch (IOException e) {
            logger.error(POST + " " + url, e);
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    logger.error(POST + " " + url, e);
                }
            }
        }
        return null;
    }

    /**
     * 异步POST请求
     * @param url
     * @param headers
     * @param data 如果data为string，系统将装换为字节，如果data为Map，系统将拼接成form表单格式，再转换为字节，如果为其他类型，转换为json字符串，再转换为字节
     * @return
     */
    public static Future<String> postAsync(final String url, Map<String, String> headers, Object data) {
        final FutureBody futureBody = new FutureBody();
        sendAsync(url, POST, headers, serializeForEntity(data, DEFAULT_CHARSET, false), SHORT_TIMEOUT,
                new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse result) {
                        processCompleted(result, url, futureBody);
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error(POST + " " + url, ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.info("{} {} cancelled", POST, url);
                    }
                });
        return futureBody;
    }

    /**
     * 异步GET请求
     * @param url
     * @param headers
     * @return
     */
    public static Future<String> getAsync(final String url, Map<String, String> headers) {
        final FutureBody futureBody = new FutureBody();
        sendAsync(url, GET, headers, null, SHORT_TIMEOUT,
                new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse result) {
                        processCompleted(result, url, futureBody);
                    }

                    @Override
                    public void failed(Exception ex) {
                        futureBody.setDone(true);
                        logger.error(POST + " " + url, ex);
                    }

                    @Override
                    public void cancelled() {
                        futureBody.setDone(true);
                        logger.info("{} {} cancelled", POST, url);
                    }
                });
        return futureBody;
    }

    /**
     * 异步GET请求
     * @param url
     * @return
     */
    public static Future<String> getAsync(final String url) {
        return getAsync(url, null);
    }

    private static void processCompleted(HttpResponse result, String url, FutureBody futureBody) {
        try {
            String body = getContentString(result, 0, null, DEFAULT_CHARSET);
            futureBody.setBody(body);
        } catch (IOException e) {
            logger.error(POST + " " + url, e);
        } finally {
            futureBody.setDone(true);
        }
    }

    /**
     * 发起一个同步请求
     * 例如：
     * <pre>
     * <code>
     *     CloseableHttpResponse response = null;
     *     try {
     *         response = send(url, method, headers, entity, timeout, charset);
     *         if (response != null) {
     *             // do something.
     *         }
     *     } finally {
     *         if (response != null) {
     *             response.close()
     *         }
     *     }
     * </code>
     * </pre>
     * @param url
     * @param method 请求方法，如GET、POST
     * @param headers HTTP的HEADER
     * @param entity 消息体字节数组，可以为null
     * @param timeout 超时时间，小于或者等于0表示不设置超时时间
     * @param charset 字符集
     * @return 调用者需要调用CloseableHttpResponse.close()
     * @throws IOException
     */
    public static CloseableHttpResponse send(String url,
                                             String method,
                                             Map<String, String> headers,
                                             byte entity[],
                                             int timeout,
                                             String charset) throws IOException {

        HttpUriRequest httpRequest = buildHttpRequest(url, method);
        CloseableHttpResponse httpResponse = null;

        addHeaders(httpRequest, headers);
        attachEntity(httpRequest, entity);

        setTimeout(httpRequest, timeout);

        long start = System.currentTimeMillis();
        long end = start;

        httpResponse = httpClient.execute(httpRequest);

        if (httpResponse != null) {
            wrapResponseEntity(httpResponse);
            httpResponse = proxyHttpResponse(httpClient,
                    httpResponse,
                    CloseableHttpResponse.class);

            end = System.currentTimeMillis();

            HttpResponseHandler handler = getHttpResponseHandler(httpResponse);
            if (handler != null) handler.setStartTime(start);
            if (handler != null) handler.setEndTime(end);
            if (handler != null) handler.setCloseHttpClient(false);
        } else {
            end = System.currentTimeMillis();
        }

        String brief = getContentString(httpResponse, BRIEF_LENGTH, TAILING, charset);

        logger.info("[send][{}ms]{} {} -> {} {} {}",
                end - start,
                method, url,
                getStatusCode(httpResponse),
                getContentLength(httpResponse),
                brief);

        return httpResponse;
    }

    private static void wrapResponseEntity(HttpResponse httpResponse) {
        httpResponse.setEntity(new MyHttpEntityWrapper(httpResponse.getEntity()));
    }

    private static <T> T proxyHttpResponse(Object httpClient,
                                           HttpResponse httpResponse,
                                           Class<T> type) {
        HttpResponseHandler handler = new HttpResponseHandler(httpClient, httpResponse);

        T t = (T) Proxy.newProxyInstance(type.getClassLoader(),
                new Class[]{type},
                handler);

        return t;
    }

    private static HttpResponseHandler getHttpResponseHandler(Object target) {
        if (hasUnsafeClass && unsafeObject != null) {
            Object o = ((Unsafe) unsafeObject).getObject(target, hOffset);
            if (o != null && o instanceof HttpResponseHandler) {
                return (HttpResponseHandler) o;
            }
        } else {
            // 使用反射
            try {
                Field field = target.getClass().getDeclaredField("h");
                if (field != null) {
                    Object o = field.get(target);
                    if (o instanceof HttpResponseHandler) {
                        return (HttpResponseHandler) o;
                    }
                }
            } catch (NoSuchFieldException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("", e);
                }
            } catch (IllegalAccessException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("", e);
                }
            }
        }

        return null;
    }

    /**
     * 异步发送HTTP请求
     * @param url
     * @param method
     * @param headers
     * @param entity
     * @param timeout
     * @return 获取到HttpResponse后需要调用HttpResponse.close()是否资源
     */
    public static Future<HttpResponse> sendAsync(String url,
                                                 String method,
                                                 Map<String, String> headers,
                                                 byte entity[],
                                                 int timeout) {

        HttpUriRequest httpRequest = buildHttpRequest(url, method);

        addHeaders(httpRequest, headers);
        attachEntity(httpRequest, entity);

        setTimeout(httpRequest, timeout);

        if (logger.isDebugEnabled()) {
            logger.debug("[sendAsync].start {} {},form={}", method, url, new String(entity));
        }

        return httpAsyncClient.execute(httpRequest, null);
    }

    /**
     * 异步发送HTTP请求
     * @param url
     * @param method
     * @param headers
     * @param entity
     * @param timeout
     * @param callback 异步回调
     */
    public static void sendAsync(String url,
                                 String method,
                                 Map<String, String> headers,
                                 byte entity[],
                                 int timeout,
                                 FutureCallback<HttpResponse> callback) {

        HttpUriRequest httpRequest = buildHttpRequest(url, method);

        addHeaders(httpRequest, headers);
        attachEntity(httpRequest, entity);

        setTimeout(httpRequest, timeout);

        if (logger.isDebugEnabled()) {
            logger.debug("[sendAsync].start {} {},form={}", method, url, entity != null ? new String(entity) : null);
        }

        FutureCallback proxy = (FutureCallback) Proxy.newProxyInstance(FutureCallbackProxy.class.getClassLoader(),
                new Class[]{FutureCallback.class},
                new FutureCallbackProxy(callback, httpRequest));
        httpAsyncClient.execute(httpRequest, proxy);
    }

    public static String getBody(HttpResponse httpResponse) {
        return getBody(httpResponse, DEFAULT_CHARSET);
    }

    public static String getBody(HttpResponse httpResponse, String charset) {
        try {
            return getContentString(httpResponse, 0, null, charset);
        } catch (IOException e) {
            logger.error("", e);
        }
        return "";
    }

    public static String getContentString(HttpResponse httpResponse) throws IOException {
        return getContentString(httpResponse, 0, null, DEFAULT_CHARSET);
    }

    public static String getContentString(HttpResponse httpResponse, int maxLength, String tailing, String charset) throws IOException {
        if (httpResponse == null || httpResponse.getEntity() == null) {
            return null;
        }

        HttpEntity httpEntity = httpResponse.getEntity();

        if (maxLength > 0) {
            byte[] datas = EntityUtils.toByteArray(httpEntity);
            if (datas == null) {
                return "";
            }
            int limit = Math.min(datas.length, (maxLength + 1) * 3);

            ContentType contentType = ContentType.get(httpEntity);

            if (contentType != null && contentType.getCharset() != null && contentType.getCharset().name() != null) {
                charset = contentType.getCharset().name();
            }

            if (charset == null) {
                charset = DEFAULT_CHARSET;
            }

            String body = new String(datas, 0, limit, charset);

            if (body.length() > maxLength) {
                body = body.substring(0, maxLength);
                if (tailing != null) {

                    body = body + tailing;
                }
            }

            return body;
        } else {
            return EntityUtils.toString(httpEntity, charset);
        }
    }

    public static int getStatusCode(HttpResponse httpResponse) {
        if (httpResponse == null) {
            return -1;
        }

        if (httpResponse.getStatusLine() == null) {
            return -1;
        }

        return httpResponse.getStatusLine().getStatusCode();
    }

    public static long getContentLength(HttpResponse httpResponse) {
        if (httpResponse == null || httpResponse.getEntity() == null) {
            return -1;
        }
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity instanceof MyHttpEntityWrapper) {
            return ((MyHttpEntityWrapper) httpEntity).getContentLength2();
        }
        return httpEntity.getContentLength();
    }

    private static void setTimeout(HttpRequest httpRequest, int timeout) {
        if (timeout <= 0) {
            return;
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .build();

        if (httpRequest instanceof HttpRequestBase) {
            ((HttpRequestBase) httpRequest).setConfig(requestConfig);
        }
    }

    private static HttpUriRequest buildHttpRequest(String url, String method) {
        if (GET.equals(method)) {
            return new HttpGet(url);
        } else if (POST.equals(method)) {
            return new HttpPost(url);
        } else if (PUT.equals(method)) {
            return new HttpPut(url);
        } else if (DELETE.equals(method)) {
            return new HttpDelete(method);
        } else {
            return new HttpGet(url);
        }
    }

    private static void addHeaders(HttpRequest request, Map<String, String> headers) {
        if (headers == null) {
            return;
        }
        Iterator<Map.Entry<String, String>> iterator = headers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> item = iterator.next();
            request.addHeader(item.getKey(), item.getValue());
        }
    }

    private static void attachEntity(HttpRequest httpRequest, byte entity[]) {
        if (entity == null || entity.length == 0) {
            return;
        }

        if (!(httpRequest instanceof HttpPost)) {
            throw new IllegalArgumentException("method must be " + POST);
        }

        ((HttpPost) httpRequest).setEntity(buildEntity(entity));
    }

    private static HttpEntity buildEntity(byte entity[]) {
        return new ByteArrayEntity(entity);
    }

    /**
     * 将form序列化为字节，字节格式是form表单格式
     * @param form
     * @param charset
     * @param urlEncode 是否要进行URL Encode
     * @return
     */
    public static byte[] serializeForEntity(Map<String, String> form, String charset, boolean urlEncode) {
        return serializeForEntity(serializeToStringForEntity(form, charset, urlEncode), charset);
    }

    /**
     * serializeForEntity(Map<String, String> form, String charset, boolean urlEncode)的别名
     * @param form
     * @param charset
     * @param urlEncode
     * @return
     */
    public static byte[] m2b(Map<String, String> form, String charset, boolean urlEncode) {
        return serializeForEntity(form, charset, urlEncode);
    }

    /**
     * 将form序列化为字符串，字符串格式是form表单格式
     * @param form
     * @param charset
     * @param urlEncode 是否要进行URL Encode
     * @return
     */
    public static String serializeToStringForEntity(Map<String, String> form, String charset, boolean urlEncode) {
        if (form == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        Iterator<Map.Entry<?, ?>> iterator = (Iterator) form.entrySet().iterator();

        int count = 0;

        while (iterator.hasNext()) {
            Map.Entry<?, ?> item = iterator.next();

            String key = null;
            String value = null;

            if (item.getKey() instanceof String) {
                key = (String) item.getKey();
            } else if (item.getKey() != null) {
                key = item.getValue().toString();
            }

            if (item.getValue() instanceof String) {
                value = (String) item.getValue();
            } else if (item.getValue() != null) {
                value = item.getValue().toString();
            }

            if (count > 0) {
                sb.append("&");
            }

            try {
                sb.append(key).append("=");
                if (urlEncode) {
                    sb.append(URLEncoder.encode(value, charset));
                } else {
                    sb.append(value);
                }
            } catch (UnsupportedEncodingException e) {
                logger.error("", e);
            }

            count++;
        }
        return sb.toString();
    }

    /**
     * serializeToStringForEntity(Map<String, String> form, String charset, boolean urlEncode)的别名
     * @param form
     * @param charset
     * @param urlEncode
     * @return
     */
    public static String m2s(Map<String, String> form, String charset, boolean urlEncode) {
        return serializeToStringForEntity(form, charset, urlEncode);
    }

    /**
     * 将formDate序列化为字节
     * @param formData
     * @param charset
     * @return
     */
    public static byte[] serializeForEntity(String formData, String charset) {
        try {
            return formData.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            logger.error("", e);
        }
        return null;
    }

    /**
     * serializeForEntity(String formData, String charset)的别名
     * @param formData
     * @param charset
     * @return
     */
    public static byte[] s2b(String formData, String charset) {
        return serializeForEntity(formData, charset);
    }

    /**
     * 将data序列化为字节<br/>
     * <p>处理方式如下</p>
     * <p>
     *     1.如果data为字节或者为null，不做任何处理。直接返回data
     * </p>
     * <p>
     *     2.如果data为字符串，序列化为字节
     * </p>
     * <p>
     *     3.如果data为Map，先生成form表单内容的字符串，然后转换为字节
     * </p>
     * @param data
     * @param charset
     * @param urlEncode
     * @return
     */
    public static byte[] serializeForEntity(Object data, String charset, boolean urlEncode) {
        if (data == null) {
            return null;
        }
        if (data instanceof String) {
            return serializeForEntity((String) data, charset);
        }

        if (data instanceof Map) {
            return serializeForEntity((Map) data, charset, urlEncode);
        }

        if (data instanceof byte[]) {
            return (byte[]) data;
        }

        return serializeForEntity(JSONObject.toJSONString(data), charset);
    }

    /**
     * serializeForEntity(Object data, String charset, boolean urlEncode)的别名
     * @param data
     * @param charset
     * @param urlEncode
     * @return
     */
    public static byte[] o2b(Object data, String charset, boolean urlEncode) {
        return serializeForEntity(data, charset, urlEncode);
    }

    /**
     * params 为 Query String，将其与url拼接
     * @param url
     * @param params
     * @return
     */
    public static String concatQueryString(String url, Map<String, String> params) {
        return concatQueryString(url, serializeToStringForEntity(params, DEFAULT_CHARSET, true));
    }

    /**
     * queryString 为 String，将其与url拼接
     * @param url
     * @param queryString
     * @return
     */
    public static String concatQueryString(String url, String queryString) {
        StringBuilder sb = new StringBuilder();
        sb.append(url);

        if (url.indexOf("?") == -1) {
            sb.append("?");
        } else {
            sb.append("&");
        }
        sb.append(queryString);
        return sb.toString();
    }

    /**
     * 释放资源
     */
    public static void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("", e);
        }

        try {
            httpAsyncClient.close();
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    /**
     * 获取请求的执行时间
     * @param httpResponse
     * @return 返回值小于0表示无法获取
     */
    public static long getCostTime(HttpResponse httpResponse) {
        HttpResponseHandler handler = getHttpResponseHandler(httpResponse);
        if (handler == null) {
            return -1;
        }
        return handler.getCostTime();
    }

}
