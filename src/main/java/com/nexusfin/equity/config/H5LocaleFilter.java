package com.nexusfin.equity.config;

import com.nexusfin.equity.enums.H5Locale;
import com.nexusfin.equity.util.H5LocaleContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class H5LocaleFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        H5Locale locale = H5Locale.resolve(request.getHeader("Accept-Language"));
        try {
            H5LocaleContext.bind(locale);
            response.setHeader("Content-Language", locale.languageTag());
            filterChain.doFilter(request, response);
        } finally {
            H5LocaleContext.clear();
        }
    }
}
