package cl.duoc.ferreteria.catalogo.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtFiltro jwtFiltro) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                       // API stateless con JWT en header, sin cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()          // sin esto, los errores reales se ven como 401
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Reglas de ms-catalogo (contrato, sección 7.4)
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**").permitAll()
                .requestMatchers("/api/v1/interno/**").permitAll()   // la protección es X-API-KEY, no el JWT
                .requestMatchers("/api/v1/productos/**", "/api/v1/categorias/**").hasRole("ADMINISTRADOR")
                .anyRequest().denyAll())                        // todo lo no listado se rechaza
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                        RespuestaErrorSeguridad.escribir(res, req, 401, "Debes iniciar sesión"))
                .accessDeniedHandler((req, res, ex) ->
                        RespuestaErrorSeguridad.escribir(res, req, 403, "No tienes permiso para esta acción")))
            .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * JwtFiltro es un @Component, y Spring Boot registra por su cuenta todo filtro que sea un bean,
     * también fuera de la cadena de seguridad. Esto lo desactiva para que se ejecute
     * únicamente donde lo pone addFilterBefore.
     */
    @Bean
    FilterRegistrationBean<JwtFiltro> registroJwtFiltro(JwtFiltro jwtFiltro) {
        FilterRegistrationBean<JwtFiltro> registro = new FilterRegistrationBean<>(jwtFiltro);
        registro.setEnabled(false);
        return registro;
    }

    /** Evita que Spring Boot cree un usuario en memoria e imprima "Using generated security password". */
    @Bean
    UserDetailsService sinUsuariosEnMemoria() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
