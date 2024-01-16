package me.quesia.practiceseedmod.mixin.core;

import me.quesia.practiceseedmod.PracticeSeedMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    public abstract void method_29970(Screen screen);

    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;startServer(Ljava/util/function/Function;)Lnet/minecraft/server/MinecraftServer;"))
    private void worldWait(CallbackInfo ci) {
        if (!PracticeSeedMod.SAVING.get()) {
            PracticeSeedMod.log("No save lock active.");
            return;
        }

        this.method_29970(new SaveLevelScreen(new LiteralText("Still saving the last world...")));

        synchronized (PracticeSeedMod.SAVE_LOCK) {
            PracticeSeedMod.log("Done waiting for save lock.");
        }
    }
}
