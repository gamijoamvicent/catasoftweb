package devforge.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession
public class WebConfig extends AbstractHttpSessionApplicationInitializer implements WebMvcConfigurer {

    @Autowired
    private LicoreriaInterceptor licoreriaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licoreriaInterceptor)
               .addPathPatterns("/**")
               .excludePathPatterns(
                   "/login",
                   "/css/**",
                   "/js/**",
                   "/error",
                   "/",
                   "/licorerias/seleccionar"
               );
    }
} 