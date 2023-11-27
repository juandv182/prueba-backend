package fastglp.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer{
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Permite configurar CORS para todas las rutas
                .allowedOrigins("http://localhost:3000","https://inf226-981-4d.inf.pucp.edu.pe")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                //aceptar todos los headers
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600L);  // Cache (tiempo en segundos) para la respuesta de pre-verificación
    }
}
