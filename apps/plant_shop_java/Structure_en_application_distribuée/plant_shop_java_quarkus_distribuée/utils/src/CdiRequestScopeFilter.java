package util;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.HashMap;
import org.jboss.weld.context.bound.BoundRequestContext;

/**
 * Active/désactive explicitement le contexte @RequestScoped pour chaque requête HTTP.
 * Repris du monolithe afin d'éviter les erreurs "No active contexts" sur Resteasy/Undertow.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Singleton
public final class CdiRequestScopeFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CDI_ACTIVE_KEY = CdiRequestScopeFilter.class.getName() + ".active";

    private static final class RequestScopeStorage extends HashMap<String, Object> {
        private static final long serialVersionUID = 1L;
    }

    @Inject
    BoundRequestContext requestContext;

    @Override
    public void filter(ContainerRequestContext httpRequestContext) throws IOException {
        RequestScopeStorage storage = new RequestScopeStorage();
        requestContext.associate(storage);
        requestContext.activate();
        httpRequestContext.setProperty(CDI_ACTIVE_KEY, storage);
    }

    @Override
    public void filter(ContainerRequestContext httpRequestContext, ContainerResponseContext responseContext) throws IOException {
        Object storage = httpRequestContext.getProperty(CDI_ACTIVE_KEY);
        if (storage instanceof RequestScopeStorage storageMap) {
            try {
                requestContext.invalidate();
                requestContext.deactivate();
            } finally {
                requestContext.dissociate(storageMap);
            }
        }
        httpRequestContext.removeProperty(CDI_ACTIVE_KEY);
    }
}
