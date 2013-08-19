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

import org.asynchttpclient.Response;
import org.asynchttpclient.SimpleAsyncHttpClient;
import org.asynchttpclient.ThrowableHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="http://bruno.factor45.org/">Bruno de Carvalho</a>
 * @author Jason Brittain
 */
public class NingSimpleAhcBenchmarkTest extends AbstractBenchmarkTest {

    private SimpleAsyncHttpClient client;

    @Override
    protected void setup() {
        super.setup();

        this.client =
                new SimpleAsyncHttpClient.Builder().setMaximumConnectionsPerHost(10).setConnectionTimeoutInMs(0).build();
    }

    @Override
    protected void tearDown() {
        super.tearDown();

        this.client.close();
    }

    public void testAsyncRequests() {
		String serverAsyncUrl = serverBaseUrl + serverAsyncUri;
		System.out.println(this.doBenchmark(serverAsyncUrl, "asyncWarmup", "runAsyncBatch"));
    }

    public void asyncWarmup(final String testUrl) {
        List<Future<Response>> futures = new ArrayList<Future<Response>>(this.warmupRequests);
        for (int i = 0; i < this.warmupRequests; i++) {
            try {
                futures.add(this.client.derive().setUrl(testUrl).build().get());
            }
            catch (IOException e) {
                System.err.println("Failed to execute get at iteration #" + i);
            }
        }

        for (Future<Response> future : futures) {
            try {
                future.get();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (ExecutionException e) {
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
                    final AtomicInteger successful = new AtomicInteger();
                    long start = System.nanoTime();
                    final CountDownLatch responseReceivedLatch = new CountDownLatch(requestsPerThreadPerBatch);
                    for (int i = 0; i < requestsPerThreadPerBatch; i++) {
                        try {
                            SimpleAsyncHttpClient derived = client.derive().setUrl(testUrl).build();

                            Future<Response> future = derived.get(new ThrowableHandler() {
                                public void onThrowable(Throwable t) {
                                    responseReceivedLatch.countDown();
                                }

                            });

                            try {
                                Response response = future.get();

                                if ((response.getStatusCode() >= 200) && (response.getStatusCode() <= 299)) {
                                    successful.incrementAndGet();
                                }
                            }
                            catch (InterruptedException e) {
                                System.err.println("Failed to execute request." + e.getMessage());
                            }
                            catch (ExecutionException e) {
                                System.err.println("Failed to execute request." + e.getMessage());
                            }
                            finally {
                                responseReceivedLatch.countDown();
                                derived.close();
                            }

                        }
                        catch (IOException e) {
                            System.err.println("Failed to execute request.");
                        }
                    }

                    try {
                        responseReceivedLatch.await();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
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
        }
        catch (InterruptedException e) {
            Thread.interrupted();
        }
        long batchTotalTime = System.nanoTime() - batchStart;

        return new BatchResult(threadResults, batchTotalTime);
    }
}
