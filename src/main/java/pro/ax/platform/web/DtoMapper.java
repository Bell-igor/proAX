package pro.ax.platform.web;

import org.springframework.stereotype.Component;
import pro.ax.platform.domain.ChatMessage;
import pro.ax.platform.domain.ChatThread;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.CompanyDocument;
import pro.ax.platform.domain.ApplicationDocument;
import pro.ax.platform.domain.CompanySize;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.domain.NeedRequest;
import pro.ax.platform.domain.Offer;
import pro.ax.platform.domain.Proposal;
import pro.ax.platform.domain.Review;
import pro.ax.platform.domain.Subscription;
import pro.ax.platform.domain.User;
import pro.ax.platform.web.dto.ChatMessageDto;
import pro.ax.platform.web.dto.ChatThreadDto;
import pro.ax.platform.web.dto.CompanyDetailDto;
import pro.ax.platform.web.dto.CompanySummaryDto;
import pro.ax.platform.web.dto.DocumentDto;
import pro.ax.platform.web.dto.IndustryDto;
import pro.ax.platform.web.dto.NeedDto;
import pro.ax.platform.web.dto.OfferDto;
import pro.ax.platform.web.dto.ProposalDto;
import pro.ax.platform.web.dto.ReviewDto;
import pro.ax.platform.web.dto.SubscriptionDto;
import pro.ax.platform.web.dto.UserDto;

import java.util.List;

/** Преобразование сущностей в DTO. Все методы вызываются внутри транзакций сервисов. */
@Component
public class DtoMapper {

    public IndustryDto industry(Industry industry, long companiesCount) {
        return new IndustryDto(industry.getCode(), industry.getTitle(), industry.getDescription(),
                industry.getIcon(), companiesCount);
    }

    public UserDto user(User user, Company company) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getCity(),
                user.getPhone(),
                company == null ? null : company.getId(),
                company == null ? null : company.getSlug(),
                company == null ? null : company.getName(),
                user.isAdmin()
        );
    }

    public CompanySummaryDto summary(Company company, boolean favorite) {
        Industry industry = company.getIndustry();
        return new CompanySummaryDto(
                company.getId(),
                company.getSlug(),
                company.getName(),
                industry == null ? null : industry.getCode(),
                industry == null ? null : industry.getTitle(),
                industry == null ? null : industry.getIcon(),
                company.getCity(),
                shorten(company.getDescription(), 180),
                company.isVerified(),
                round1(company.getRating()),
                company.getReviewsCount(),
                company.getAccentColor(),
                sizeTitle(company.getSize()),
                company.getFoundedYear(),
                favorite,
                company.getLogoUrl(),
                company.getPlan().name(),
                company.getPlan().getTitle(),
                company.isPromoted()
        );
    }

    public CompanyDetailDto detail(Company company, boolean favorite, boolean mine, int openNeeds,
                                   List<OfferDto> offers, List<ReviewDto> reviews, int documentsCount) {
        Industry industry = company.getIndustry();
        User owner = company.getOwner();
        return new CompanyDetailDto(
                company.getId(),
                company.getSlug(),
                company.getName(),
                industry == null ? null : industry.getCode(),
                industry == null ? null : industry.getTitle(),
                industry == null ? null : industry.getIcon(),
                company.getCity(),
                company.getDescription(),
                company.getWebsite(),
                company.getPhone(),
                company.getEmail(),
                sizeTitle(company.getSize()),
                company.getFoundedYear(),
                company.isVerified(),
                round1(company.getRating()),
                company.getReviewsCount(),
                company.getAccentColor(),
                owner == null ? null : owner.getDisplayName(),
                company.getCreatedAt(),
                favorite,
                mine,
                openNeeds,
                offers,
                reviews,
                company.getLogoUrl(),
                company.getPlan().name(),
                company.getPlan().getTitle(),
                company.isPromoted(),
                documentsCount
        );
    }

    public OfferDto offer(Offer offer, boolean mine) {
        Company company = offer.getCompany();
        return new OfferDto(
                offer.getId(),
                company.getId(),
                company.getSlug(),
                company.getName(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getType().name(),
                offer.getType().getTitle(),
                offer.getPriceFrom(),
                offer.getUnit(),
                offer.getCurrency(),
                offer.isActive(),
                offer.getCreatedAt(),
                mine
        );
    }

    public NeedDto need(NeedRequest need, boolean mine, boolean canRespond, boolean alreadyResponded) {
        Industry industry = need.getIndustry();
        Company authorCompany = need.getAuthorCompany();
        return new NeedDto(
                need.getId(),
                need.getTitle(),
                need.getDescription(),
                industry == null ? null : industry.getCode(),
                industry == null ? null : industry.getTitle(),
                industry == null ? null : industry.getIcon(),
                need.getCity(),
                need.getBudget(),
                need.getDeadline(),
                need.getStatus().name(),
                need.getStatus().getTitle(),
                need.getAuthor().getDisplayName(),
                authorCompany == null ? null : authorCompany.getName(),
                authorCompany == null ? null : authorCompany.getSlug(),
                need.getProposalsCount(),
                need.getCreatedAt(),
                mine,
                canRespond,
                alreadyResponded
        );
    }

    public ProposalDto proposal(Proposal proposal, boolean mine) {
        Company company = proposal.getCompany();
        NeedRequest need = proposal.getNeed();
        return new ProposalDto(
                proposal.getId(),
                need.getId(),
                need.getTitle(),
                company.getId(),
                company.getSlug(),
                company.getName(),
                proposal.getMessage(),
                proposal.getPrice(),
                proposal.getStatus().name(),
                proposal.getStatus().getTitle(),
                proposal.getCreatedAt(),
                mine
        );
    }

    public ReviewDto review(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getAuthor().getDisplayName(),
                review.getRating(),
                review.getText(),
                review.getCreatedAt()
        );
    }

    public ChatThreadDto chatThread(ChatThread thread, User viewer) {
        NeedRequest need = thread.getNeed();
        Company company = thread.getCompany();
        boolean author = viewer != null && need.getAuthor().getId().equals(viewer.getId());
        return new ChatThreadDto(
                thread.getId(),
                need.getId(),
                need.getTitle(),
                company.getId(),
                company.getName(),
                company.getSlug(),
                need.getAuthor().getDisplayName(),
                author ? "AUTHOR" : "RESPONDER",
                thread.getLastMessagePreview(),
                thread.getLastMessageAt(),
                thread.getMessagesCount()
        );
    }

    public ChatMessageDto chatMessage(ChatMessage message, ChatThread thread, User viewer) {
        Long senderId = message.getSender().getId();
        boolean fromAuthor = thread.getNeed().getAuthor().getId().equals(senderId);
        Company senderCompany = fromAuthor ? thread.getNeed().getAuthorCompany() : thread.getCompany();
        return new ChatMessageDto(
                message.getId(),
                thread.getId(),
                senderId,
                message.getSender().getDisplayName(),
                senderCompany == null ? null : senderCompany.getName(),
                message.getBody(),
                message.getCreatedAt(),
                viewer != null && senderId.equals(viewer.getId())
        );
    }

    public DocumentDto document(CompanyDocument document) {
        return new DocumentDto(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getContentType(),
                document.getSize(),
                document.getStoredPath(),
                document.getCreatedAt()
        );
    }

    public DocumentDto applicationDocument(ApplicationDocument document) {
        return new DocumentDto(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getContentType(),
                document.getSize(),
                document.getStoredPath(),
                document.getCreatedAt()
        );
    }

    public SubscriptionDto subscription(Subscription subscription) {
        return new SubscriptionDto(
                subscription.getPlan().getTitle(),
                subscription.getAmount(),
                subscription.getStartedAt(),
                subscription.getExpiresAt()
        );
    }

    private static String sizeTitle(CompanySize size) {
        return size == null ? null : size.getTitle();
    }

    private static double round1(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return null;
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max - 1).trim() + "…";
    }
}
