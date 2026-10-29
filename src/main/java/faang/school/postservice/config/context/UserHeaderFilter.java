package faang.school.postservice.config.context;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserHeaderFilter implements Filter {
    private final UserContext userContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String userId = httpRequest.getHeader("x-user-id");

        try {
            if (userId != null) {
                long parsedUserId;
                try {
                    parsedUserId = Long.parseLong(userId);
                } catch (NumberFormatException exception) {
                    ((HttpServletResponse) response).sendError(
                            HttpServletResponse.SC_BAD_REQUEST, "x-user-id must be a positive integer");
                    return;
                }
                if (parsedUserId <= 0) {
                    ((HttpServletResponse) response).sendError(
                            HttpServletResponse.SC_BAD_REQUEST, "x-user-id must be a positive integer");
                    return;
                }
                userContext.setUserId(parsedUserId);
            }
            chain.doFilter(request, response);
        } finally {
            userContext.clear();
        }
    }
}
