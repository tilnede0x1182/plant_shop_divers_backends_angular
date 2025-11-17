package util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ForwardedIdentityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            ForwardedIdentityHolder.set(extractIdentity(request));
            filterChain.doFilter(request, response);
        } finally {
            ForwardedIdentityHolder.clear();
        }
    }

    private ForwardedIdentity extractIdentity(HttpServletRequest request) {
        String idHeader = request.getHeader("X-User-Id");
        if (idHeader == null || idHeader.isBlank()) {
            return ForwardedIdentity.anonymous();
        }
        try {
            int id = Integer.parseInt(idHeader.trim());
            boolean admin = Boolean.parseBoolean(request.getHeader("X-User-Admin"));
            return new ForwardedIdentity(id, admin);
        } catch (NumberFormatException e) {
            return ForwardedIdentity.anonymous();
        }
    }
}
