package security;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Singleton
public class CdiRequestScopeFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject
    RequestContextController requestContextController;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestContextController.activate()) {
            ACTIVE.set(Boolean.TRUE);
        } else {
            ACTIVE.set(Boolean.FALSE);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (ACTIVE.get()) {
            requestContextController.deactivate();
        }
        ACTIVE.remove();
    }
}
