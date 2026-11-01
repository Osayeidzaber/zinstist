package dev.jilaton.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.jilaton.client.module.impl.TimeChangerModule;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Time Changer: server time updates are replaced by the forced time. Ported from Anubis Client (MixinTimeChangerClientLevel, GPL-3.0, Horcus and 4ldenz). */
@Mixin(ClientLevel.class)
public abstract class TimeChangerMixin {
    @WrapOperation(method = "setTimeFromServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;setDayTime(J)V"))
    private void jilaton$timeChangerServerDayTime(ClientLevel.ClientLevelData levelData, long dayTime, Operation<Void> original) {
        TimeChangerModule timeChanger = TimeChangerModule.active;
        if (timeChanger == null) {
            original.call(levelData, dayTime);
            return;
        }
        ClientLevel level = (ClientLevel) (Object) this;
        long shown = levelData.getDayTime();
        long forced = timeChanger.interceptServerDayTime(level, dayTime);
        original.call(levelData, forced);
        if (forced != shown) level.environmentAttributes().invalidateTickCache();
    }
}
