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

import java.util.List;

/**
 * @author <a href="http://bruno.factor45.org/">Bruno de Carvalho</a>
 */
public class BenchmarkResult {

    private final int threads;
    private final int batches;
    private final long targetRequests;
    private final long successfulRequests;
    private final long failedRequests;
    private final float averageRequestTime;
    private final float averageBatchTime;
    private final long totalBenchmarkTime;
    private final float requestsPerSecond;

    public BenchmarkResult(int threads, int batches, List<BatchResult> results) {
        this.threads = threads;
        this.batches = batches;

        long totalTargetRequests = 0;
        long totalSuccessfulRequests = 0;
        long totalFailedRequests = 0;
        long totalBenchmarkTime = 0;
        float totalRequestAverage = 0;

        for (BatchResult result : results) {
            totalTargetRequests += result.getBatchTargetRequests();
            totalSuccessfulRequests += result.getBatchSuccessfulRequests();
            totalFailedRequests += result.getBatchTargetRequests() - result.getBatchSuccessfulRequests();
            totalBenchmarkTime += result.getTotalBatchTime();
            totalRequestAverage += result.getAverageTimePerRequest();
        }

        this.targetRequests = totalTargetRequests;
        this.successfulRequests = totalSuccessfulRequests;
        this.failedRequests = totalFailedRequests;

        this.averageRequestTime = totalRequestAverage / (float) results.size();
        this.averageBatchTime = totalBenchmarkTime / (float) results.size();
        this.totalBenchmarkTime = totalBenchmarkTime;
        this.requestsPerSecond = 1000000000f / this.averageRequestTime;
    }

    public static String decimal(double d) {
        return decimal(d, 2);
    }

    public static String decimal(double d, int decimalUnits) {
        if (decimalUnits <= 0) {
            return Double.toString(d);
        }

        String s = Double.toString(d);
        int pos = s.indexOf('.');
        return s.substring(0, Math.min(pos + 1 + decimalUnits, s.length())); // xxxx.yy
    }

    public static String decimal(float f) {
        return decimal(f, 2);
    }

    public static String decimal(float f, int decimalUnits) {
        if (decimalUnits <= 0) {
            return Float.toString(f);
        }

        String s = Float.toString(f);
        int pos = s.indexOf('.');
        return s.substring(0, Math.min(pos + 1 + decimalUnits, s.length())); // xxxx.yy
    }

    public int getThreads() {
        return threads;
    }

    public int getBatches() {
        return batches;
    }

    public long getTargetRequests() {
        return targetRequests;
    }

    public long getSuccessfulRequests() {
        return successfulRequests;
    }

    public long getFailedRequests() {
        return failedRequests;
    }

    public float getAverageRequestTime() {
        return averageRequestTime;
    }

    public float getAverageBatchTime() {
        return averageBatchTime;
    }

    public long getTotalBenchmarkTime() {
        return totalBenchmarkTime;
    }

    public float getRequestsPerSecond() {
        return requestsPerSecond;
    }

    @Override
    public String toString() {
        return "BenchmarkResult: " +
               "successful=" + successfulRequests +
               ", failed=" + failedRequests +
               ", average=" + decimal(averageRequestTime / 1000000f) +
               "ms, requestsPerSecond=" + decimal(requestsPerSecond) +
               ", averageBatchTime=" + decimal(averageBatchTime / 1000000f) +
               "ms, totalBenchmarkTime=" + decimal(totalBenchmarkTime / 1000000f) +
               "ms, targetRequests=" + targetRequests;
               
    }
}
