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

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.junit.Test;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author Jason Brittain
 */
public class Jetty8BenchmarkTest extends AbstractBenchmarkTest {

    private HttpClient client;

    @Override
    protected void setup() {
        super.setup();
        
        client = new HttpClient();
        client.setRequestBufferSize(8 * 1024);
        client.setResponseBufferSize(8 * 1024);
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        client.setMaxConnectionsPerAddress(100);
        try {
           client.start();
        } catch (Exception e) {
           assertTrue(false);
        }
    }

    @Override
    protected void tearDown() {
        super.tearDown();

        try {
            this.client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAsyncRequests() {
    		String serverAsyncUrl = serverBaseUrl + serverAsyncUri;
		System.out.println(this.doBenchmark(serverAsyncUrl, "asyncWarmup", "runAsyncBatch"));
    }
    
    @Test
    public void testSyncRequests() {
    		String serverSyncUrl = serverBaseUrl + serverSyncUri;
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
        try {
            this.client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < this.warmupRequests; i++) {

            ContentExchange exchange = new ContentExchange();

            exchange.setURL(testUrl);

            try {
                this.client.send(exchange);
                try {
                    exchange.waitForDone();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException ex) {
            }
        }
    }
    
    public void syncWarmup(final String testUrl) {
        for (int i = 0; i < this.warmupRequests; i++) {
        		ContentExchange exchange = new ContentExchange(true);
        		exchange.setURL(testUrl);
        		try {
				client.send(exchange);
				exchange.waitForDone();        	
			} catch (Exception e) {}
        }
    }

    public BatchResult runAsyncBatch(final String testUrl) {
        final CountDownLatch latch = new CountDownLatch(this.threads);
        final Vector<ThreadResult> threadResults = new Vector<ThreadResult>(this.threads);

        long batchStart = System.nanoTime();
        for (int i = 0; i < this.threads; i++) {
            this.executor.submit(new Runnable() {

                @Override
                public void run() {
                    final CountDownLatch responseReceivedLatch = new CountDownLatch(requestsPerThreadPerBatch);
                    final AtomicInteger successful = new AtomicInteger();
                    long start = System.nanoTime();
                    for (int i = 0; i < requestsPerThreadPerBatch; i++) {
                        ContentExchange exchange = new ContentExchange() {
                        		@Override
                        		protected void onResponseComplete() throws IOException {
                        			if (getResponseStatus() == 200) {
                        				getResponseContent();
                        				successful.incrementAndGet();
                        			}
                        			responseReceivedLatch.countDown();
                        		}
                        };

                        exchange.setURL(testUrl);

                        try {
                            client.send(exchange);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    
					long totalTime = 0;
					try {
						responseReceivedLatch.await();
						totalTime = System.nanoTime() - start;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
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
						ContentExchange exchange = new ContentExchange(true);
						exchange.setURL(testUrl);
						try {
							client.send(exchange);

							// Wait until the exchange is terminated
							int exchangeState = exchange.waitForDone();
							if (exchangeState == HttpExchange.STATUS_COMPLETED) {
								if ((exchange.getStatus() >= 200) && (exchange.getStatus() <= 299)) {
		                    		// Make the response body into a String, then we throw it away because we're done.
		                    		exchange.getResponseContent();
	                                successful.incrementAndGet();
	                            }
							}
						} catch (IOException e) {
							// Failed request.. it doesn't get counted as successful.
						} catch (InterruptedException e) {
							// also doesn't get counted as successful.
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