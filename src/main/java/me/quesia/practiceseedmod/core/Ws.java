package me.quesia.practiceseedmod.core;

import java.net.URI;
import java.nio.ByteBuffer;
import me.quesia.practiceseedmod.PracticeSeedMod;
import net.minecraft.client.MinecraftClient;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class Ws extends WebSocketClient {
    public Ws(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public Ws(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        PracticeSeedMod.log("opened ws connection");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        PracticeSeedMod.log("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        String[] m = message.split(":", 0);
        switch (m[0]) {
            case "play":
                PracticeSeedMod.log("u want 2 play " + m[1]);
                long l = Long.parseLong(m[1]);
                PracticeSeedMod.QUEUE.add(new Seed(l, "todo notes"));

                if (!PracticeSeedMod.RUNNING) {
                    if (!MinecraftClient.getInstance().isInSingleplayer()) {
                        PracticeSeedMod.RUNNING = true;
                        PracticeSeedMod.playNextSeed();
                    }
                }
                break;

            // case "ping":
            //     PracticeSeedMod.log("ping from ws server");
            //     break;

            default:
                PracticeSeedMod.log("unrecognized message: " + message);
                break;
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {
        PracticeSeedMod.log("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        PracticeSeedMod.log("an error occurred:" + ex);
    }

}
