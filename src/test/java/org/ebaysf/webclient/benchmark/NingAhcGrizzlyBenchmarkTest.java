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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.asynchttpclient.providers.grizzly.GrizzlyAsyncHttpProvider;
import org.asynchttpclient.providers.grizzly.GrizzlyAsyncHttpProviderConfig;

/**
 * @author Jason Brittain
 * @author <a href="http://bruno.factor45.org/">Bruno de Carvalho</a>
 */
public class NingAhcGrizzlyBenchmarkTest extends AbstractBenchmarkTest {

    private AsyncHttpClient client;

    @Override
    protected void setup() {
        super.setup();

        GrizzlyAsyncHttpProviderConfig providerConfig = new GrizzlyAsyncHttpProviderConfig();
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
				.setAsyncHttpClientProviderConfig(providerConfig)
		                .setAsyncConnectMode(true)
				.setMaximumConnectionsTotal(-1)
				.setMaximumConnectionsPerHost(64)
				.setCompressionEnabled(false)
				.setAllowPoolingConnection(true /* keep-alive connection */)
				// .setAllowPoolingConnection(false /* no keep-alive connection */)
				.setConnectionTimeoutInMs(9000).setRequestTimeoutInMs(9000)
				.setIdleConnectionInPoolTimeoutInMs(3000).build();

        this.client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);

    }

    @Override
    protected void tearDown() {
        super.tearDown();

        //this.client.close();
    }

    @Test
    public void testAsyncRequests() {
    		String serverAsyncUrl = serverBaseUrl + serverAsyncUri;
		System.out.println(this.doBenchmark(serverAsyncUrl, "asyncWarmup", "runAsyncBatch"));
    }

    @Test
    public void testSyncRequests() {
		String serverSyncUrl = serverBaseUrl + serverSyncUri;
		System.out.println(this.doBenchmark(serverSyncUrl, "asyncWarmup", "runSyncBatch"));
    }

    @Test
    public void testAsyncLargeResponses() {
        String serverLargeUrl = serverBaseUrl + serverLargeUri;
		System.out.println(this.doBenchmark(serverLargeUrl, "asyncWarmup", "runAsyncBatch"));
    }

    @Test
    public void testSyncLargeResponses() {
        String serverLargeUrl = serverBaseUrl + serverLargeUri;
		System.out.println(this.doBenchmark(serverLargeUrl, "asyncWarmup", "runSyncBatch"));
    }
    
    public void asyncWarmup(final String testUrl) {
        List<Future<Response>> futures = new ArrayList<Future<Response>>(warmupRequests);
        for (int i = 0; i < warmupRequests; i++) {
            try {
                futures.add(this.client.prepareGet(testUrl).execute());
            } catch (IOException e) {
                System.err.println("Failed to execute get at iteration #" + i);
            }
        }

        for (Future<Response> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
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
                    final CountDownLatch responseReceivedLatch = new CountDownLatch(requestsPerThreadPerBatch);
                    //List<ListenableFuture<Response>> futures = new ArrayList<ListenableFuture<Response>>(requestsPerThreadPerBatch);
                    final AtomicInteger successful = new AtomicInteger();
                    final long start = System.nanoTime();
                    for (int i = 0; i < requestsPerThreadPerBatch; i++) {
						try {
			                //ListenableFuture<Response> future = 
			                	client.prepareGet(testUrl).execute(new AsyncCompletionHandler<Response>() {
			                    @Override
			                    public Response onCompleted(Response response) throws Exception {
			                    		// Make the response body into a String, then we throw it away because we're done.
			                    		response.getResponseBody("UTF-8");
			                    		successful.incrementAndGet();
			                    		responseReceivedLatch.countDown();
			                        return response;
			                    }

			                    @Override
			                    public void onThrowable(Throwable t) {
			                        // Something wrong happened.
			                    		//System.out.println(t);
			                    		t.printStackTrace();
			                    		responseReceivedLatch.countDown();
			                    }
			                });
			                //futures.add(future);
			            } catch (IOException e) {
			            		responseReceivedLatch.countDown();
			            }
					}

					long totalTime = 0;

					try {
						responseReceivedLatch.await();
						totalTime = System.nanoTime() - start;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					/*
			        for (ListenableFuture<Response> future : futures) {
			            try {
			                future.get();
			            } catch (InterruptedException e) {
			                e.printStackTrace();
			            } catch (ExecutionException e) {
			                e.printStackTrace();
			            }
			        }
			        */
			        
					totalTime = System.nanoTime() - start;
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
                        try {
                            Response response = client.prepareGet(testUrl).execute().get();

                            if ((response.getStatusCode() >= 200) && (response.getStatusCode() <= 299)) {
	                    		// Make the response body into a String, then we throw it away because we're done.
	                    		response.getResponseBody("UTF-8");
                                successful.incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    long totalTime = System.nanoTime() - start;
                    threadResults.add(new ThreadResult(requestsPerThreadPerBatch, successful.get(), totalTime));
                    latch.countDown();
                }
            }

            );
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
