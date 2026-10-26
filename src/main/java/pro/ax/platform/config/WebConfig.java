package pro.ax.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pro.ax.platform.security.ApiAccessInterceptor;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiAccessInterceptor apiAccessInterceptor;
    private final Path uploadsDir;

    public WebConfig(ApiAccessInterceptor apiAccessInterceptor,
                     @Value("${proax.uploads-dir:uploads}") String uploadsDir) {
        this.apiAccessInterceptor = apiAccessInterceptor;
        this.uploadsDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAccessInterceptor).addPathPatterns("/api/**");
    }

    /** Загруженные логотипы и документы раздаются из каталога uploads. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadsDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Внутренние разделы SPA отдаются тем же index.html
        registry.addViewController("/catalog").setViewName("forward:/index.html");
        registry.addViewController("/cabinet").setViewName("forward:/index.html");
        registry.addViewController("/admin").setViewName("forward:/index.html");
        registry.addViewController("/chat").setViewName("forward:/index.html");
    }
}
