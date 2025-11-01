package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Intercepts method calls and logs execution time for methods marked with {@link Profiled}.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object target;
    private final ProfilingState profilingState;

    ProfilingMethodInterceptor(Clock clock, Object target, ProfilingState profilingState) {
        this.clock = Objects.requireNonNull(clock);
        this.target = Objects.requireNonNull(target);
        this.profilingState = Objects.requireNonNull(profilingState);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean isProfiled = method.getAnnotation(Profiled.class) != null;
        Instant startTime = isProfiled ? clock.instant() : null;

        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Access to method denied", e);
        } finally {
            if (isProfiled) {
                Instant endTime = clock.instant();
                Duration elapsed = Duration.between(startTime, endTime);
                profilingState.record(target.getClass(), method, elapsed);
            }
        }

        return result;
    }
}
