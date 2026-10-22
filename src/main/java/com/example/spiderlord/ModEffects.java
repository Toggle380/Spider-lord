package com.example.spiderlord;

import javax.annotation.Nullable;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SpiderLordMod.MODID);

    /** Состояние «Владыка пауков» хранится как бесконечный эффект: он сам синхронизируется с клиентами. */
    public static final RegistryObject<MobEffect> SPIDER_LORD =
            EFFECTS.register("spider_lord", SpiderLordEffect::new);

    public static MobEffectInstance lordInstance() {
        return new MobEffectInstance(SPIDER_LORD.get(), MobEffectInstance.INFINITE_DURATION, 0, false, false, true);
    }

    public static boolean isLord(@Nullable Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(SPIDER_LORD.get());
    }
}
