package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ProfilingState state = new ProfilingState();
    private final ZonedDateTime startTime;
    private String crawlerType = "Unknown";

    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        Objects.requireNonNull(klass);

        boolean hasProfiledMethod = false;
        for (var method : klass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Profiled.class)) {
                hasProfiledMethod = true;
                break;
            }
        }

        if (!hasProfiledMethod) {
            throw new IllegalArgumentException(
                    klass.getName() + " does not contain any methods marked with @Profiled");
        }

        // Capture crawler type for header
        crawlerType = klass.getSimpleName();

        Object proxyInstance = Proxy.newProxyInstance(
                klass.getClassLoader(),
                new Class[]{klass},
                new ProfilingMethodInterceptor(clock, delegate, state)
        );

        return klass.cast(proxyInstance);
    }

    @Override
    public void writeData(Path path) {
        try (Writer writer = Files.newBufferedWriter(
                path,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writeData(writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write profiling output to file: " + path, e);
        }
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("=== Profiling Session: " + crawlerType + " ===");
        writer.write(System.lineSeparator());
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
