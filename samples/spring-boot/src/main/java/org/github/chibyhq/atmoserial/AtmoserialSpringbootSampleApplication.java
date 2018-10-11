package org.github.chibyhq.atmoserial;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.ContainerInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class AtmoserialSpringbootSampleApplication  extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(AtmoserialSpringbootSampleApplication.class, args);
	}
	
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AtmoserialSpringbootSampleApplication.class);
    }

    @Configuration
    static class MvcConfiguration implements WebMvcConfigurer {
        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController("/").setViewName("forward:/index.html");
        }
        
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins(CorsConfiguration.ALL)
                    .allowedMethods("PUT", "DELETE","GET", "POST", "HEAD", "OPTIONS");
        }
    }

    @Bean
    public EmbeddedAtmosphereInitializer atmosphereInitializer() {
        return new EmbeddedAtmosphereInitializer();
    }

    @Bean
    public ServletRegistrationBean atmosphereServlet() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new AtmosphereServlet()
                , "/atmoserial/*");
        registration.addInitParameter("org.atmosphere.cpr.packages", "org.github.chibyhq.atmoserial");
        registration.addInitParameter(
                "org.atmosphere.interceptor.HeartbeatInterceptor" + ".clientHeartbeatFrequencyInSeconds", "10");
        registration.setLoadOnStartup(0);
        // Need to occur before the EmbeddedAtmosphereInitializer
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        //registration.addInitParameter("org.atmosphere.cpr.AtmosphereInterceptor", "org.atmosphere.interceptor.CorsInterceptor,org.atmosphere.interceptor.HeartbeatInterceptor");
        return registration;
    }

    private static class EmbeddedAtmosphereInitializer extends ContainerInitializer
            implements ServletContextInitializer {

        @Override
        public void onStartup(ServletContext servletContext) throws ServletException {
            onStartup(Collections.<Class<?>> emptySet(), servletContext);
            
        }
    }

}
