package lol.leontg.iddrpc;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.exceptions.UnsupportedOsType;
import dev.firstdark.rpc.handlers.DiscordEventHandler;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.User;

import lol.leontg.iddrpc.utils.DiscordGameDetector;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author LeonTG
 */
public class Main implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ImprovedDefaultDiscordRPC");

    private DiscordRpc rpc;

    @Override
    public void onInitializeClient() {
        try {
            DiscordGameDetector.makeDiscordNotDetectDefaultMinecraftRPC();
        } catch (Throwable thr) {
            LOGGER.error("[DiscordRPC] Failed to make Discord not display the default Minecraft Rich Presence", thr);
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                if (this.rpc == null) {
                    this.enableRPC();
                }
            } else {
                if (this.rpc != null) {
                    this.disableRPC();
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(_ -> this.disableRPC());
    }

    private void enableRPC() {
        if (this.rpc != null) {
            return;
        }

        LOGGER.info("[DiscordRPC] Enabling Discord RPC...");
        this.rpc = new DiscordRpc();

        DiscordEventHandler handler = new DiscordEventHandler() {
            @Override
            public void ready(User user) {
                LOGGER.info("[DiscordRPC] Discord RPC successfully enabled!");
                LOGGER.info("[DiscordRPC] Detected User: {}", user.getUsername());
            }

            @Override
            public void disconnected(ErrorCode errorCode, @Nullable String message) {
                LOGGER.info("[DiscordRPC] Disconnected: {} ({})", message, errorCode);
            }

            @Override
            public void errored(ErrorCode errorCode, @Nullable String message) {
                LOGGER.info("[DiscordRPC] An error occurred: {} ({})", message, errorCode);
            }

            @Override
            public void joinGame(String joinSecret) {}

            @Override
            public void spectateGame(String spectateSecret) {}

            @Override
            public void joinRequest(DiscordJoinRequest joinRequest) {}
        };

        try {
            this.rpc.init("1557009977040965713", handler, false);
        } catch (UnsupportedOsType ex) {
            LOGGER.error("[DiscordRPC] Failed to enable Discord RPC.", ex);
            return;
        }

        this.rpc.updatePresence(null);
        LOGGER.info("[DiscordRPC] Initialization complete...");
    }

    private void disableRPC() {
        if (this.rpc == null) {
            return;
        }

        LOGGER.info("[DiscordRPC] Disabling Discord RPC...");

        this.rpc.shutdown();
        this.rpc = null;

        LOGGER.info("[DiscordRPC] Discord RPC successfully disabled.");
    }
}
