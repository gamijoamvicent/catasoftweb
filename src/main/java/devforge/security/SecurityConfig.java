package devforge.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/licorerias/seleccionar");
        return handler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/js/**").permitAll()
                .requestMatchers("/ventas/**").hasAnyRole("CAJERO", "ADMIN_LOCAL", "SUPER_ADMIN")
                .requestMatchers("/ventas/confirmar").hasAnyAuthority("ROLE_CAJERO", "ROLE_ADMIN_LOCAL", "ROLE_SUPER_ADMIN")
                .requestMatchers("/producto/actualizar", "/usuarios/**").hasAnyRole("ADMIN_LOCAL", "SUPER_ADMIN")
                .requestMatchers("/impresora/**").hasAnyRole("ADMIN_LOCAL", "SUPER_ADMIN")
                .requestMatchers("/licorerias/gestionar", "/licorerias/guardar", "/licorerias/*/cambiar-estado", "/licorerias/*/eliminar").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ventas/confirmar")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));

        // Configurar el manejador de errores
        http.exceptionHandling(handling -> handling
            .accessDeniedPage("/error?mensaje=Acceso denegado")
        );

        return http.build();
    }
}
