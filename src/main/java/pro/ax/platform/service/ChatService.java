package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.ChatMessage;
import pro.ax.platform.domain.ChatThread;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.NeedRequest;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.ChatMessageRepository;
import pro.ax.platform.repo.ChatThreadRepository;
import pro.ax.platform.repo.ProposalRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.ChatConversationDto;
import pro.ax.platform.web.dto.ChatMessageDto;
import pro.ax.platform.web.dto.ChatThreadDto;

import java.util.List;

/**
 * Переписка по заявке. Диалог всегда привязан к паре «заявка + компания-отклик»,
 * поэтому в нём ровно две стороны: автор заявки и представитель компании.
 */
@Service
public class ChatService {

    private final ChatThreadRepository threads;
    private final ChatMessageRepository messages;
    private final ProposalRepository proposals;
    private final NeedService needService;
    private final CompanyService companyService;
    private final DtoMapper mapper;

    public ChatService(ChatThreadRepository threads, ChatMessageRepository messages,
                       ProposalRepository proposals, NeedService needService,
                       CompanyService companyService, DtoMapper mapper) {
        this.threads = threads;
        this.messages = messages;
        this.proposals = proposals;
        this.needService = needService;
        this.companyService = companyService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ChatThreadDto> myThreads(User user) {
        return threads.findForUser(user.getId()).stream()
                .map(thread -> mapper.chatThread(thread, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countFor(User user) {
        return threads.countForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public ChatConversationDto conversation(User user, Long threadId) {
        ChatThread thread = requireThread(threadId);
        requireParticipant(thread, user);
        List<ChatMessageDto> items = messages.findByThreadIdOrderByCreatedAtAscIdAsc(threadId).stream()
                .map(message -> mapper.chatMessage(message, thread, user))
                .toList();
        return new ChatConversationDto(mapper.chatThread(thread, user), items);
    }

    @Transactional
    public ChatMessageDto send(User user, Long threadId, String body) {
        ChatThread thread = requireThread(threadId);
        requireParticipant(thread, user);
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw ApiException.badRequest("Сообщение не может быть пустым");
        }
        if (text.length() > 2000) {
            throw ApiException.badRequest("Сообщение длиннее 2000 символов");
        }
        ChatMessage message = messages.save(new ChatMessage(thread, user, text));
        thread.registerMessage(text);
        threads.save(thread);
        return mapper.chatMessage(message, thread, user);
    }

    /** Открыть (или создать) диалог по заявке и компании. */
    @Transactional
    public ChatThreadDto ensureThread(User user, Long needId, Long companyId) {
        NeedRequest need = needService.require(needId);
        Company company = companyService.requireCompany(companyId);
        boolean author = need.getAuthor().getId().equals(user.getId());
        boolean responder = company.getOwner() != null && company.getOwner().getId().equals(user.getId());
        if (!author && !responder && !user.isAdmin()) {
            throw ApiException.forbidden("Диалог доступен только автору заявки и откликнувшейся компании");
        }
        if (!proposals.existsByNeedIdAndCompanyId(needId, companyId)) {
            throw ApiException.badRequest("Компания не откликалась на эту заявку");
        }
        ChatThread thread = threads.findByNeedIdAndCompanyId(needId, companyId)
                .orElseGet(() -> threads.save(new ChatThread(need, company, user)));
        return mapper.chatThread(thread, user);
    }

    /** Диалог создаётся автоматически, когда компания откликается на заявку. */
    @Transactional
    public void ensureThreadForProposal(NeedRequest need, Company company, User initiator) {
        if (threads.findByNeedIdAndCompanyId(need.getId(), company.getId()).isEmpty()) {
            threads.save(new ChatThread(need, company, initiator));
        }
    }

    @Transactional(readOnly = true)
    public ChatThread requireThread(Long threadId) {
        return threads.findById(threadId)
                .orElseThrow(() -> ApiException.notFound("Диалог не найден"));
    }

    private static void requireParticipant(ChatThread thread, User user) {
        boolean author = thread.getNeed().getAuthor().getId().equals(user.getId());
        boolean responder = thread.getCompany().getOwner() != null
                && thread.getCompany().getOwner().getId().equals(user.getId());
        if (!author && !responder && !user.isAdmin()) {
            throw ApiException.forbidden("Этот диалог вам недоступен");
        }
    }
}
