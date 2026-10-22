package com.example.spiderlord.client;

import com.example.spiderlord.ModEffects;
import com.example.spiderlord.SpiderLordMod;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Лазание по стенам: движение игрока считается на клиенте, поэтому логика здесь. */
@Mod.EventBusSubscriber(modid = SpiderLordMod.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof LocalPlayer player)) return;
        if (!ModEffects.isLord(player)) return;
        if (player.isSpectator() || player.getAbilities().flying || player.isFallFlying()
                || player.isInWater() || player.isInLava() || player.isPassenger()) return;
        if (!player.horizontalCollision) return;

        Vec3 motion = player.getDeltaMovement();
        double y;
        if (player.input.shiftKeyDown) {
            y = 0.0D;                          // Shift: прилипнуть к стене
        } else if (player.input.up || player.input.jumping) {
            y = 0.2D;                          // вперёд/прыжок: ползти вверх
        } else {
            y = Math.max(motion.y, -0.04D);    // иначе медленно сползать
        }
        player.setDeltaMovement(motion.x, y, motion.z);
        player.fallDistance = 0.0F;
    }
}
