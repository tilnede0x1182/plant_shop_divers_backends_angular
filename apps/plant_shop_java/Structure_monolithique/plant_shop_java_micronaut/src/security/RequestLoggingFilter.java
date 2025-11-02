package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Log each HTTP request and its response status for easier debugging.
 */
@Singleton
@Filter("/**")
public final class RequestLoggingFilter implements HttpServerFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        long start = System.currentTimeMillis();
        LOG.info("→ {} {}", request.getMethodName(), request.getPath());
        return Mono.from(chain.proceed(request))
            .map(response -> {
                long duration = System.currentTimeMillis() - start;
                LOG.info("← {} {} ({} ms)", request.getMethodName(), response.getStatus(), duration);
                return response;
            });
    }
}
