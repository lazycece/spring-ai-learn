package com.lazycece.springaichat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lazycece
 * @date 2026/4/22
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          SyncMcpToolCallbackProvider mcpToolCallbackProvider) {
        ChatMemory rawMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();
        // Wrap to strip reasoning_content before storing,
        // so it won't break DeepSeek V4 Pro multi-turn conversations.
        ChatMemory chatMemory = new ReasoningStrippingChatMemory(rawMemory);
        chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultToolCallbacks(mcpToolCallbackProvider)
                .build();
    }

    @PostMapping("/ai")
    public String generation(@RequestBody ChatRequest request) {
        return this.chatClient.prompt()
                .user(request.userInput())
                .advisors(a -> a.param("chat_memory_conversation_id", request.conversationId()))
                .call()
                .content();
    }
}

/**
 * {@link ChatMemory} decorator that strips {@code reasoningContent} metadata
 * from AssistantMessage before storing.  This keeps DeepSeek V4 Pro's
 * thinking mode enabled for the current turn while preventing the
 * "reasoning_content must be passed back" error in multi-turn conversations.
 */
class ReasoningStrippingChatMemory implements ChatMemory {

    private final ChatMemory delegate;

    ReasoningStrippingChatMemory(ChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> cleaned = new ArrayList<>(messages.size());
        for (Message m : messages) {
            if (m instanceof AssistantMessage am && am.getMetadata().containsKey("reasoningContent")) {
                var newMeta = new HashMap<>(am.getMetadata());
                newMeta.remove("reasoningContent");
                cleaned.add(AssistantMessage.builder()
                        .content(am.getText())
                        .properties(newMeta)
                        .toolCalls(am.getToolCalls())
                        .media(am.getMedia())
                        .build());
            } else {
                cleaned.add(m);
            }
        }
        delegate.add(conversationId, cleaned);
    }

    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }
}
