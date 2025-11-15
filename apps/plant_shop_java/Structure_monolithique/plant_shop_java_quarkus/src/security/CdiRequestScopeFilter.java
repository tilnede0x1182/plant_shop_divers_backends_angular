package security;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import jakarta.annotation.Priority;
import org.jboss.weld.context.bound.BoundRequestContext;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Singleton
public class CdiRequestScopeFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CDI_ACTIVE_KEY = CdiRequestScopeFilter.class.getName() + ".active";

    @Inject
    BoundRequestContext requestContext;

    @Override
    public void filter(ContainerRequestContext httpRequestContext) throws IOException {
        Map<String, Object> requestMap = new HashMap<>();
        this.requestContext.associate(requestMap);
        this.requestContext.activate();
        httpRequestContext.setProperty(CDI_ACTIVE_KEY, requestMap);
    }

    @Override
    public void filter(ContainerRequestContext httpRequestContext, ContainerResponseContext responseContext) throws IOException {
        Object storage = httpRequestContext.getProperty(CDI_ACTIVE_KEY);
        if (storage != null) {
            try {
                this.requestContext.invalidate();
                this.requestContext.deactivate();
            } finally {
                if (storage instanceof Map) {
                    this.requestContext.dissociate((Map<String, Object>) storage);
                }
            }
        }
        httpRequestContext.removeProperty(CDI_ACTIVE_KEY);
    }
}
