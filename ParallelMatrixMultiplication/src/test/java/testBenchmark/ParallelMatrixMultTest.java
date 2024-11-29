package testBenchmark;

import org.openjdk.jmh.annotations.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class ParallelMatrixMultTest {

    @Param({"10", "100", "1024"})
    private int n;

    private double[][] a;
    private double[][] b;
    private double[][] c;

    @Setup(Level.Trial)
    public void setup() {
        a = new double[n][n];
        b = new double[n][n];
        c = new double[n][n];

        Random random = new Random();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = random.nextDouble();
                b[i][j] = random.nextDouble();
                c[i][j] = 0;
            }
        }
    }

    @Benchmark
    public void testParallelMethod() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < n; i++) {
            final int row = i;
            executor.submit(() -> {
                for (int j = 0; j < n; j++) {
                    for (int k = 0; k < n; k++) {
                        c[row][j] += a[row][k] * b[k][j];
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        logSystemMetrics();
        logSpeedup();
    }

    private void logSystemMetrics() {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of cores used: " + cores);

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed();

        System.out.println("Used memory: " + usedMemory / (1024 * 1024) + " MB");
    }

    private void logSpeedup() {
        long sequentialTime = measureSequential();
        long parallelTime = measureParallel();

        double speedup = (double) sequentialTime / parallelTime;
        System.out.println("Sequential time: " + sequentialTime + " ms");
        System.out.println("Parallel time: " + parallelTime + " ms");
        System.out.println("Speedup per thread: " + speedup);
    }

    private long measureSequential() {
        double[][] tempC = new double[n][n];
        long start = System.currentTimeMillis();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    tempC[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        long end = System.currentTimeMillis();
        return end - start;
    }

    private long measureParallel() {
        double[][] tempC = new double[n][n];
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        long start = System.currentTimeMillis();

        for (int i = 0; i < n; i++) {
            final int row = i;
            executor.submit(() -> {
                for (int j = 0; j < n; j++) {
                    for (int k = 0; k < n; k++) {
                        tempC[row][j] += a[row][k] * b[k][j];
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long end = System.currentTimeMillis();
        return end - start;
    }
}
