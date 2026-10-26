package pro.ax.platform.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.service.IndustryService;
import pro.ax.platform.service.StatsService;
import pro.ax.platform.web.dto.IndustryDto;
import pro.ax.platform.web.dto.StatsDto;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final IndustryService industryService;
    private final StatsService statsService;

    public CatalogController(IndustryService industryService, StatsService statsService) {
        this.industryService = industryService;
        this.statsService = statsService;
    }

    @GetMapping("/industries")
    public List<IndustryDto> industries() {
        return industryService.list();
    }

    @GetMapping("/cities")
    public List<String> cities() {
        return industryService.cities();
    }

    @GetMapping("/stats")
    public StatsDto stats() {
        return statsService.stats();
    }
}
