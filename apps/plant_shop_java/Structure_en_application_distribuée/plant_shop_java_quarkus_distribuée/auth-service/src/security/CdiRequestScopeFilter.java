package security;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import jakarta.annotation.Priority;
import java.io.IOException;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Singleton
public class CdiRequestScopeFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CDI_ACTIVE_KEY = CdiRequestScopeFilter.class.getName() + ".active";

    @Inject
    RequestContextController requestContextController;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean activated = requestContextController.activate();
        requestContext.setProperty(CDI_ACTIVE_KEY, activated);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Object flag = requestContext.getProperty(CDI_ACTIVE_KEY);
        if (flag instanceof Boolean && (Boolean) flag) {
            requestContextController.deactivate();
        }
        requestContext.removeProperty(CDI_ACTIVE_KEY);
    }
}
