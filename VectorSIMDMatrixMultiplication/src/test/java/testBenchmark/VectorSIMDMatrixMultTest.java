package testBenchmark;

import org.openjdk.jmh.annotations.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class VectorSIMDMatrixMultTest {

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
    public void testSIMDMethod() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int k = 0; k < n; k += 4) {
                    sum += simdDotProduct(a[i], b, k, j);
                }
                c[i][j] = sum;
            }
        }

        logSystemMetrics();
    }

    private double simdDotProduct(double[] row, double[][] matrix, int start, int column) {
        double sum = 0;

        for (int i = 0; i < 4 && (start + i) < row.length; i++) {
            sum += row[start + i] * matrix[start + i][column];
        }
        return sum;
    }

    private void logSystemMetrics() {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of cores used: " + cores);

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed();

        System.out.println("Used memory: " + usedMemory / (1024 * 1024) + " MB");
    }
}
