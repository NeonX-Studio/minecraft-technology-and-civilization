package com.nightcity.tcivilization.sim;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tick-time profiler writing CSV reports to logs/tc-profile-<timestamp>.csv (03 §5.13, 40 RM-P6).
 */
public final class TcProfiler {
    private static final TcProfiler GLOBAL = new TcProfiler();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public record Stats(long samples, long totalNanos, long maxNanos) {
        public double avgMs() {
            return samples == 0 ? 0.0D : (totalNanos / (double) samples) / 1_000_000.0D;
        }

        public double peakMs() {
            return maxNanos / 1_000_000.0D;
        }
    }

    private final Map<String, Stats> moduleStats = new ConcurrentHashMap<>();
    private volatile int remainingTicks = 0;
    private volatile Path outputDirectory = Path.of("logs");

    public static TcProfiler global() {
        return GLOBAL;
    }

    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public synchronized void startSession(int seconds) {
        moduleStats.clear();
        remainingTicks = Math.clamp(seconds, 1, 600) * 20;
    }

    public boolean isRecording() {
        return remainingTicks > 0;
    }

    public void recordSample(String moduleId, long durationNanos) {
        if (remainingTicks <= 0) {
            return;
        }
        moduleStats.compute(moduleId, (k, prev) -> {
            if (prev == null) {
                return new Stats(1L, durationNanos, durationNanos);
            }
            return new Stats(prev.samples() + 1L, prev.totalNanos() + durationNanos, Math.max(prev.maxNanos(), durationNanos));
        });
    }

    public synchronized Optional<Path> endTickAndMaybeExport() {
        if (remainingTicks <= 0) {
            return Optional.empty();
        }
        remainingTicks--;
        if (remainingTicks > 0) {
            return Optional.empty();
        }
        return Optional.of(exportCsv());
    }

    public synchronized Path exportCsv() {
        try {
            Files.createDirectories(outputDirectory);
            Path csv = outputDirectory.resolve("tc-profile-" + LocalDateTime.now().format(TS_FMT) + ".csv");
            StringBuilder sb = new StringBuilder("module,samples,avg_ms,peak_ms\n");
            Map<String, Stats> snapshot = new LinkedHashMap<>(moduleStats);
            if (snapshot.isEmpty()) {
                sb.append("tcivilization,0,0.000,0.000\n");
            } else {
                for (Map.Entry<String, Stats> entry : snapshot.entrySet()) {
                    Stats s = entry.getValue();
                    sb.append(entry.getKey())
                            .append(',').append(s.samples())
                            .append(',').append(String.format(java.util.Locale.ROOT, "%.4f", s.avgMs()))
                            .append(',').append(String.format(java.util.Locale.ROOT, "%.4f", s.peakMs()))
                            .append('\n');
                }
            }
            Files.writeString(csv, sb.toString(), StandardCharsets.UTF_8);
            return csv;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed writing profile CSV", ex);
        }
    }
}
