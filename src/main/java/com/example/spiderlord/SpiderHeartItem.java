package com.example.spiderlord;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SpiderHeartItem extends Item {
    private static final int SWARM_COOLDOWN = 600; // 30 секунд

    public SpiderHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.spiderlord.spider_heart.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.spiderlord.spider_heart.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.spiderlord.spider_heart.tooltip3").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        boolean lord = ModEffects.isLord(player);
        if (!lord) {
            player.addEffect(ModEffects.lordInstance());
            level.playSound(null, player.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.7F);
            player.sendSystemMessage(Component.translatable("message.spiderlord.become"));
            player.getCooldowns().addCooldown(this, 40);
        } else if (player.isShiftKeyDown()) {
            player.removeEffect(ModEffects.SPIDER_LORD.get());
            player.sendSystemMessage(Component.translatable("message.spiderlord.revert"));
            player.getCooldowns().addCooldown(this, 40);
        } else {
            summonSwarm((ServerLevel) level, player);
            player.sendSystemMessage(Component.translatable("message.spiderlord.swarm"));
            player.getCooldowns().addCooldown(this, SWARM_COOLDOWN);
        }
        return InteractionResultHolder.consume(stack);
    }

    private void summonSwarm(ServerLevel level, Player player) {
        RandomSource random = level.getRandom();
        int spawned = 0;
        for (int attempt = 0; attempt < 20 && spawned < 5; attempt++) {
            BlockPos pos = player.blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                continue;
            }
            Spider spider;
            if (spawned == 4) {
                spider = EntityType.CAVE_SPIDER.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
            } else {
                spider = EntityType.SPIDER.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
            }
            if (spider != null) {
                spawned++;
                level.sendParticles(ParticleTypes.POOF, spider.getX(), spider.getY() + 0.3, spider.getZ(),
                        10, 0.3, 0.3, 0.3, 0.02);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.2F, 0.5F);
    }
}
