package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.IndustryRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.IndustryDto;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Справочник отраслей и ролей участников площадки. */
@Service
public class IndustryService {

    private final IndustryRepository industries;
    private final CompanyRepository companies;
    private final DtoMapper mapper;

    public IndustryService(IndustryRepository industries, CompanyRepository companies, DtoMapper mapper) {
        this.industries = industries;
        this.companies = companies;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<IndustryDto> list() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : companies.countGroupedByIndustry()) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return industries.findAllByOrderBySortOrderAsc().stream()
                .map(industry -> mapper.industry(industry, counts.getOrDefault(industry.getCode(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Industry require(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("Не выбрана отрасль");
        }
        return industries.findByCode(code.trim())
                .orElseThrow(() -> ApiException.badRequest("Неизвестная отрасль: " + code));
    }

    @Transactional(readOnly = true)
    public List<String> cities() {
        return companies.findDistinctCities();
    }

    @Transactional(readOnly = true)
    public Map<String, String> titlesByCode() {
        Map<String, String> result = new LinkedHashMap<>();
        industries.findAllByOrderBySortOrderAsc()
                .forEach(industry -> result.put(industry.getCode(), industry.getTitle()));
        return result;
    }
}
