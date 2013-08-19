/*
 * Copyright (c) 2012-2013 eBay Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ebaysf.webclient.benchmark;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.junit.Test;

/**
 * @author Jason Brittain
 * @author <a href="http://bruno.factor45.org/">Bruno de Carvalho</a>
 */
public class HttpComponents4BenchmarkTest extends AbstractBenchmarkTest {

    private Async async;
    private HttpClient client;

    @Override
    protected void setup() {
        super.setup();
        
        // For async..
        ExecutorService threadpool = Executors.newFixedThreadPool(60);
        async = Async.newInstance().use(threadpool);

        // For sync..
        HttpParams params = new BasicHttpParams();
        params.setParameter(HttpProtocolParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        params.setBooleanParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
        params.setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false);
        params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024);
        ConnManagerParams.setMaxTotalConnections(params, 10);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(10));
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        this.client = new DefaultHttpClient(cm, params);
    }

    @Override
    protected void tearDown() {
        super.tearDown();
        
        // For sync..
        this.client.getConnectionManager().shutdown();
    }

    @Test
    public void testAsyncRequests() {
        String serverAsyncUrl = serverBaseUrl + serverAsyncUri;
		System.out.println(this.doBenchmark(serverAsyncUrl, "asyncWarmup", "runAsyncBatch"));
    }

    @Test
    public void testSyncRequests() {
        String serverSyncUrl = serverBaseUrl + serverAsyncUri;
		System.out.println(this.doBenchmark(serverSyncUrl, "syncWarmup", "runSyncBatch"));
    }
    
    @Test
    public void testAsyncLargeResponses() {
        String serverLargeUrl = serverBaseUrl + serverLargeUri;
		System.out.println(this.doBenchmark(serverLargeUrl, "asyncWarmup", "runAsyncBatch"));
    }

    @Test
    public void testSyncLargeResponses() {
        String serverLargeUrl = serverBaseUrl + serverLargeUri;
		System.out.println(this.doBenchmark(serverLargeUrl, "syncWarmup", "runSyncBatch"));
    }
    
    public void asyncWarmup(final String testUrl) {
        for (int i = 0; i < this.warmupRequests; i++) {
    		final Request request = Request.Get(testUrl);
    			Future<Content> future = async.execute(request, new FutureCallback<Content>() {
				@Override
				public void completed(final Content content) {
					// TODO
				}

				@Override
				public void failed(final Exception ex) {
					// TODO
				}

				@Override
				public void cancelled() {
					// TODO
				}
			});
    			try {
    				future.get();
    			} catch (InterruptedException e) {
    				// TODO
    			} catch (ExecutionException e) {
    				// TODO
    			}
        }
    }

    public void syncWarmup(final String testUrl) {
        for (int i = 0; i < this.warmupRequests; i++) {
            HttpGet get = new HttpGet(testUrl);
            try {
                HttpResponse response = this.client.execute(get);
                response.getEntity().consumeContent();
            } catch (IOException e) {
                get.abort();
            }
        }
    }

    public BatchResult runAsyncBatch(final String testUrl) {
        final CountDownLatch latch = new CountDownLatch(this.threads);
        final Vector<ThreadResult> threadResults = new Vector<ThreadResult>(this.threads);

        long batchStart = System.nanoTime();
        for (int i = 0; i < this.threads; i++) {
            this.executor.submit(new Runnable() {

                public void run() {
                    final AtomicInteger successful = new AtomicInteger();
                    long start = System.nanoTime();
                    for (int i = 0; i < requestsPerThreadPerBatch; i++) {
                    		final Request request = Request.Get(testUrl);
                    		Future<Content> future = async.execute(request, new FutureCallback<Content>() {
                    	        
                				@Override
                    	        public void completed(final Content content) {
                    	        		successful.incrementAndGet();
                    	        }
                    	        
                				@Override
                    	        public void failed(final Exception ex) {
                    	            // TODO
                    	        }
                    	        
                				@Override
                    	        public void cancelled() {
                    	        		// TODO
                    	        }
                    	    });
                    		try {
                    			future.get();
						} catch (InterruptedException e) {
							// TODO
						} catch (ExecutionException e) {
							// TODO
						}
                    }

                    long totalTime = System.nanoTime() - start;
                    threadResults.add(new ThreadResult(requestsPerThreadPerBatch, successful.get(), totalTime));
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        long batchTotalTime = System.nanoTime() - batchStart;

        return new BatchResult(threadResults, batchTotalTime);
    }
    
    public BatchResult runSyncBatch(final String testUrl) {
        final CountDownLatch latch = new CountDownLatch(this.threads);
        final Vector<ThreadResult> threadResults = new Vector<ThreadResult>(this.threads);

        long batchStart = System.nanoTime();
        for (int i = 0; i < this.threads; i++) {
            this.executor.submit(new Runnable() {

                public void run() {
                    final AtomicInteger successful = new AtomicInteger();
                    long start = System.nanoTime();
                    for (int i = 0; i < requestsPerThreadPerBatch; i++) {
                        HttpGet get = new HttpGet(testUrl);
                        try {
                            HttpResponse response = client.execute(get);
                            response.getEntity().consumeContent();
                            if (response.getStatusLine().getStatusCode() == 200) {
                                successful.incrementAndGet();
                            }
                        } catch (IOException e) {
                            get.abort();
                        }
                    }

                    long totalTime = System.nanoTime() - start;
                    threadResults.add(new ThreadResult(requestsPerThreadPerBatch, successful.get(), totalTime));
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        long batchTotalTime = System.nanoTime() - batchStart;

        return new BatchResult(threadResults, batchTotalTime);
    }
}
