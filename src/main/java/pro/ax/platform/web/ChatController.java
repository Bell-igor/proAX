package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.ChatService;
import pro.ax.platform.web.dto.ChatConversationDto;
import pro.ax.platform.web.dto.ChatMessageDto;
import pro.ax.platform.web.dto.ChatThreadDto;
import pro.ax.platform.web.dto.EnsureThreadForm;
import pro.ax.platform.web.dto.MessageForm;

import java.util.List;

/** Переписка по заявкам: диалоги автора заявки и откликнувшихся компаний. */
@RestController
@RequestMapping("/api/threads")
public class ChatController {

    private final ChatService chatService;
    private final AuthService authService;

    public ChatController(ChatService chatService, AuthService authService) {
        this.chatService = chatService;
        this.authService = authService;
    }

    @GetMapping
    public List<ChatThreadDto> threads(HttpSession session) {
        User user = authService.requireUser(session);
        return chatService.myThreads(user);
    }

    @GetMapping("/{id}")
    public ChatConversationDto conversation(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        return chatService.conversation(user, id);
    }

    @PostMapping("/{id}/messages")
    public ChatMessageDto send(@PathVariable Long id, @Valid @RequestBody MessageForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return chatService.send(user, id, form.body());
    }

    @PostMapping("/ensure")
    public ChatThreadDto ensure(@Valid @RequestBody EnsureThreadForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return chatService.ensureThread(user, form.needId(), form.companyId());
    }
}
