package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.Offer;
import pro.ax.platform.domain.OfferType;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.OfferRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.OfferDto;
import pro.ax.platform.web.dto.OfferForm;

import java.util.List;
import java.util.Locale;

/** Товары и услуги компаний. */
@Service
public class OfferService {

    private final OfferRepository offers;
    private final CompanyService companyService;
    private final DtoMapper mapper;

    public OfferService(OfferRepository offers, CompanyService companyService, DtoMapper mapper) {
        this.offers = offers;
        this.companyService = companyService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<OfferDto> forCompany(Long companyId, User viewer) {
        Company company = companyService.requireCompany(companyId);
        boolean mine = company.getOwner() != null && viewer != null && company.getOwner().getId().equals(viewer.getId());
        List<Offer> list = mine
                ? offers.findByCompanyIdOrderByCreatedAtDesc(companyId)
                : offers.findByCompanyIdAndActiveTrueOrderByCreatedAtDesc(companyId);
        return list.stream().map(offer -> mapper.offer(offer, mine)).toList();
    }

    @Transactional(readOnly = true)
    public List<OfferDto> latest() {
        return offers.findTop6ByActiveTrueOrderByCreatedAtDesc().stream()
                .map(offer -> mapper.offer(offer, false))
                .toList();
    }

    @Transactional
    public OfferDto create(User user, OfferForm form) {
        Company company = companyService.requireMyCompany(user);
        Offer offer = new Offer(company, form.title().trim());
        apply(offer, form);
        return mapper.offer(offers.save(offer), true);
    }

    @Transactional
    public OfferDto update(User user, Long offerId, OfferForm form) {
        Company company = companyService.requireMyCompany(user);
        Offer offer = offers.findByIdAndCompanyId(offerId, company.getId())
                .orElseThrow(() -> ApiException.notFound("Предложение не найдено"));
        offer.setTitle(form.title().trim());
        apply(offer, form);
        return mapper.offer(offers.save(offer), true);
    }

    @Transactional
    public void delete(User user, Long offerId) {
        Company company = companyService.requireMyCompany(user);
        Offer offer = offers.findByIdAndCompanyId(offerId, company.getId())
                .orElseThrow(() -> ApiException.notFound("Предложение не найдено"));
        offers.delete(offer);
    }

    @Transactional
    public OfferDto changeActive(User user, Long offerId, boolean active) {
        Company company = companyService.requireMyCompany(user);
        Offer offer = offers.findByIdAndCompanyId(offerId, company.getId())
                .orElseThrow(() -> ApiException.notFound("Предложение не найдено"));
        offer.setActive(active);
        return mapper.offer(offers.save(offer), true);
    }

    private static void apply(Offer offer, OfferForm form) {
        offer.setDescription(AccountService.trimToNull(form.description()));
        offer.setPriceFrom(form.priceFrom());
        offer.setUnit(AccountService.trimToNull(form.unit()));
        offer.setType(parseType(form.type()));
        if (form.active() != null) {
            offer.setActive(form.active());
        }
    }

    private static OfferType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return OfferType.PRODUCT;
        }
        try {
            return OfferType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return OfferType.PRODUCT;
        }
    }
}
