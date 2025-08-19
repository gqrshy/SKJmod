package com.gqrshy.skjmod.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;

/**
 * Custom callback for chat message events
 * Runs when a chat message is received (before being displayed)
 */
public interface ChatMessageCallback {
    Event<ChatMessageCallback> EVENT = EventFactory.createArrayBacked(ChatMessageCallback.class,
            (listeners) -> (message) -> {
                for (ChatMessageCallback listener : listeners) {
                    message = listener.onMessage(message);
                    if (message == null) {
                        return null;
                    }
                }
                return message;
            });

    Text onMessage(Text message);
}