package com.example.spiderlord;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class SpiderLordEffect extends MobEffect {
    public SpiderLordEffect() {
        super(MobEffectCategory.NEUTRAL, 0x4B0F5E);
        // Паучья проворность: +15% к скорости
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "5f0a3b8e-6d1c-4b6e-9a53-2c1d7e8f9a10",
                0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** Молоко не снимает эффект. */
    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
