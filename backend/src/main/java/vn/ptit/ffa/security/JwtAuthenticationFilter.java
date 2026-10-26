package vn.ptit.ffa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.ptit.ffa.error.ApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** DUAN-40: chuyen Authorization: Bearer <jwt> thanh AuthPrincipal trong SecurityContext. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthPrincipal principal = jwtService.verify(header.substring(PREFIX.length()).trim());
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities(principal));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ApiException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute("authError", e);
            }
        }
        chain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> authorities(AuthPrincipal principal) {
        List<SimpleGrantedAuthority> list = new ArrayList<>();
        list.add(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
        if (principal.systemAdmin()) {
            list.add(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"));
        }
        return list;
    }
}
