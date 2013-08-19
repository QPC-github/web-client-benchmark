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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author Jason Brittain
 * @author <a href="http://bruno.factor45.org/">Bruno de Carvalho</a>
 */
public abstract class AbstractBenchmarkTest {
	
	protected static String SERVER_BASE_URL_DEFAULT = "http://localhost:8080";
	protected static String SERVER_SYNC_URI_DEFAULT = "/index.html";
	protected static String SERVER_ASYNC_URI_DEFAULT = "/index.html";
	protected static String SERVER_LARGE_URI_DEFAULT = "/500k.json";

	protected static final int WARMUP_REQUESTS = 1000;
    protected static final int THREADS_DEFAULT = 100;
    protected static final int BATCHES_DEFAULT = 10;
    protected static final int REQS_PER_THREAD_DEFAULT = 40;
	
    protected int threads;
    protected int requestsPerThreadPerBatch;
    protected int batches;
    protected int warmupRequests;
    protected String serverBaseUrl;
    protected String serverSyncUri;
    protected String serverAsyncUri;
    protected String serverLargeUri;

    protected ExecutorService executor;
    
	@Rule
	public TestName testName = new TestName();
	
	@Rule
	public TestWatcher testWatcher = new TestWatcher() {
	    @Override
	    protected void starting(final Description description) {
	        String methodName = description.getMethodName();
	        if (methodName == null) {
	            methodName = "setUpBeforeClass..";
	        }

	        String className = description.getClassName();
	        className = className.substring(className.lastIndexOf('.') + 1);
	        System.out.println("Starting " + className + " " + methodName);
	    }
	};
	
    public AbstractBenchmarkTest() {
    		System.out.println("JAVA_HOME      : " + System.getProperty("java.home"));

    		warmupRequests = THREADS_DEFAULT;
		String warmupRequestsString = System.getProperty("warmupRequests");
		if (warmupRequestsString != null) {
			try {
				warmupRequests = Integer.parseInt(warmupRequestsString);
			} catch (NumberFormatException e) {
				System.out.println("warmupRequests value of \"" + warmupRequestsString
					+ "\" invalid, using " + warmupRequests);
			}
		}
		System.out.println("warmupRequests : " + warmupRequests);

		threads = THREADS_DEFAULT;
		String threadsString = System.getProperty("threads");
		if (threadsString != null) {
			try {
				threads = Integer.parseInt(threadsString);
			} catch (NumberFormatException e) {
				System.out.println("Threads value of \"" + threadsString
					+ "\" invalid, using " + threads);
			}
		}
		System.out.println("threads        : " + threads);

		batches = BATCHES_DEFAULT;
		String batchesString = System.getProperty("batches");
		if (batchesString != null) {
			try {
				batches = Integer.parseInt(batchesString);
			} catch (NumberFormatException e) {
				System.out.println("Batches value of \"" + batchesString
					+ "\" invalid, using " + batches);
			}
		}
		System.out.println("batches        : " + batches);
		
		requestsPerThreadPerBatch = REQS_PER_THREAD_DEFAULT;
		String requestsPerThreadPerBatchString = System.getProperty("reqsPerThread");
		if (requestsPerThreadPerBatchString != null) {
			try {
				requestsPerThreadPerBatch = Integer.parseInt(requestsPerThreadPerBatchString);
			} catch (NumberFormatException e) {
				System.out.println("Requests per thread value of \"" + requestsPerThreadPerBatchString
					+ "\" invalid, using " + requestsPerThreadPerBatch);
			}
		}
		System.out.println("reqsPerThread  : " + requestsPerThreadPerBatch);

		String serverBaseUrlDefault = System.getProperty("serverBaseUrl");
		if (serverBaseUrlDefault != null) {
			serverBaseUrl = serverBaseUrlDefault;
		} else {
			serverBaseUrl = SERVER_BASE_URL_DEFAULT;
		}
		System.out.println("server base url: " + serverBaseUrl);

		serverSyncUri = System.getProperty("serverSyncUri");
		if (serverSyncUri == null) {
			serverSyncUri = SERVER_SYNC_URI_DEFAULT;
		}
		serverAsyncUri = System.getProperty("serverAsyncUri");
		if (serverAsyncUri == null) {
			serverAsyncUri = SERVER_ASYNC_URI_DEFAULT;
		}
		serverLargeUri = System.getProperty("serverLargeUri");
		if (serverLargeUri == null) {
			serverLargeUri = SERVER_LARGE_URI_DEFAULT;
		}
	}
    
    public BenchmarkResult doBenchmark(String testUrl, String warmupMethodName, String benchmarkMethodName) {

        Method warmupMethod = null;
        try {
			warmupMethod = this.getClass().getMethod(warmupMethodName, new Class[] { String.class });
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
        
        Method benchmarkMethod = null;
        try {
			benchmarkMethod = this.getClass().getMethod(benchmarkMethodName, new Class[] { String.class });
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
        
        this.setup();
        if (warmupRequests != 0) {
        		System.err.println("Beginning warmup...");
        		try {
        			warmupMethod.invoke(this, new Object[] { testUrl });
        		} catch (Exception e) {
        			e.printStackTrace();
        			return null;
        		}
        		System.err.println("Warmup complete, running " + this.batches + " batches...");
        } else {
        		System.out.println("Skipped warmup.");
        }
        
        List<BatchResult> results = new ArrayList<BatchResult>(this.batches);

        // Run batches of benchmarks, one batch at a time.
        for (int i = 0; i < this.batches; i++) {
        	
            BatchResult result;
			try {
				result = (BatchResult) benchmarkMethod.invoke(this, new Object[] { testUrl });
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
            results.add(result);
            //System.err.println("Batch " + i + " finished: " + result);
        }

        System.err.println("Test finished, shutting down and calculating results...");
        this.tearDown();
        return new BenchmarkResult(this.threads, this.batches, results);
    }

    protected void setup() {
        this.executor = Executors.newFixedThreadPool(this.threads);
    }

    protected void tearDown() {
        //this.executor.shutdown();
    }

    public int getThreads() {
        return threads;
    }

    public int getRequestsPerThreadPerBatch() {
        return requestsPerThreadPerBatch;
    }

    public int getBatches() {
        return batches;
    }

    public int getWarmupRequests() {
        return warmupRequests;
    }

    public void setWarmupRequests(int warmupRequests) {
        if (warmupRequests < 1) {
            throw new IllegalArgumentException("Minimum warmup requests is 1");
        }
        this.warmupRequests = warmupRequests;
    }
}
