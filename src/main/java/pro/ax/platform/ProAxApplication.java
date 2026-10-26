package pro.ax.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ProAX — B2B-экосистема: единая площадка для поставщиков ресурсов, производства,
 * логистики, складов, юристов и сервисных компаний.
 */
@SpringBootApplication
@EnableScheduling
public class ProAxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProAxApplication.class, args);
    }
}
