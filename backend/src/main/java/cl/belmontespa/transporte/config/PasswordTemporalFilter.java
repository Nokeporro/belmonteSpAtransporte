package cl.belmontespa.transporte.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

public class PasswordTemporalFilter extends OncePerRequestFilter {
    private final tools.jackson.databind.ObjectMapper json;
    public PasswordTemporalFilter(tools.jackson.databind.ObjectMapper json) { this.json = json; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt && Boolean.TRUE.equals(jwt.getToken().getClaim("temporal"))) {
            String path = UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            boolean perfil = ("GET".equals(request.getMethod()) || "PUT".equals(request.getMethod()))
                    && path.equalsIgnoreCase("/api/v1/usuarios/" + auth.getName());
            boolean logout = "POST".equals(request.getMethod()) && path.equals("/api/v1/auth/logout");
            if (!perfil && !logout) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(json.writeValueAsString(new cl.belmontespa.transporte.dto.response.ApiError(
                        java.time.Instant.now(), 403, "Forbidden", "Debes cambiar la contraseña", request.getRequestURI())));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
