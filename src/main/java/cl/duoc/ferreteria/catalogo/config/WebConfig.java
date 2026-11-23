package cl.duoc.ferreteria.catalogo.config;

import cl.duoc.ferreteria.catalogo.security.ClaveInternaValidador;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Aplica ClaveInternaValidador solo a la API interna. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ClaveInternaValidador claveInternaValidador;

    public WebConfig(ClaveInternaValidador claveInternaValidador) {
        this.claveInternaValidador = claveInternaValidador;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(claveInternaValidador).addPathPatterns("/api/v1/interno/**");
    }
}
