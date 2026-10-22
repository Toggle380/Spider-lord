package com.example.spiderlord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/** Серверная логика способностей Владыки пауков. */
@Mod.EventBusSubscriber(modid = SpiderLordMod.MODID)
public class SpiderLordEvents {
    private static final double COMMAND_RADIUS = 32.0D;
    private static final long ORDER_TICKS = 200L;
    private static final long MILK_COOLDOWN = 600L;
    private static final String MILK_TAG = "spiderlord_last_milk";

    private record Order(UUID target, long expires) {}

    private static final Map<UUID, BlockPos> LAST_WEB = new HashMap<>();
    private static final Map<UUID, Order> ORDERS = new HashMap<>();
    private static final Set<UUID> RESPAWN_AS_LORD = new HashSet<>();

    // ------------------------------------------------------------------ тик: паутина и командование

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer player) || !ModEffects.isLord(player)) return;

        ServerLevel level = (ServerLevel) player.level();
        spinWeb(player, level);
        if (player.tickCount % 10 == 0) {
            commandSpiders(player, level);
        }
    }

    /** След из паутины: в каждом новом блоке, через который прошёл владыка. */
    private static void spinWeb(ServerPlayer player, ServerLevel level) {
        BlockPos pos = player.blockPosition();
        if (pos.equals(LAST_WEB.get(player.getUUID()))) return;
        LAST_WEB.put(player.getUUID(), pos);

        if (player.isSpectator() || player.isInWater()) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            level.setBlock(pos, ModBlocks.SPIDER_SILK.get().defaultBlockState(), 3);
            player.causeFoodExhaustion(0.02F);
        }
    }

    private static void commandSpiders(ServerPlayer lord, ServerLevel level) {
        AABB box = lord.getBoundingBox().inflate(COMMAND_RADIUS);
        List<Spider> spiders = level.getEntitiesOfClass(Spider.class, box);
        if (spiders.isEmpty()) return;

        LivingEntity enemy = currentEnemy(lord, level);
        for (Spider spider : spiders) {
            if (spider.getTarget() == lord) {
                spider.setTarget(null);
            }
            if (enemy != null) {
                if (spider.getTarget() != enemy) {
                    spider.setTarget(enemy);
                }
            } else if (spider.getTarget() == null && spider.distanceToSqr(lord) > 64.0D) {
                spider.getNavigation().moveTo(lord, 1.2D); // идут за владыкой
            }
        }
    }

    private static LivingEntity currentEnemy(ServerPlayer lord, ServerLevel level) {
        Order order = ORDERS.get(lord.getUUID());
        if (order == null) return null;
        if (level.getGameTime() > order.expires()) {
            ORDERS.remove(lord.getUUID());
            return null;
        }
        Entity target = level.getEntity(order.target());
        return target instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static void issueOrder(LivingEntity lord, LivingEntity target) {
        if (target == lord || target instanceof Spider || ModEffects.isLord(target)) return;
        ORDERS.put(lord.getUUID(), new Order(target.getUUID(), lord.level().getGameTime() + ORDER_TICKS));
    }

    // ------------------------------------------------------------------ бой

    /** Владыка бьёт цель: пауки нападают на неё. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !ModEffects.isLord(player)) return;
        if (event.getTarget() instanceof LivingEntity target) {
            issueOrder(player, target);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        // Владыку ударили: пауки мстят.
        if (ModEffects.isLord(victim) && event.getSource().getEntity() instanceof LivingEntity attacker) {
            issueOrder(victim, attacker);
        }

        // Ядовитый укус владыки.
        if (event.getSource().getEntity() instanceof Player biter
                && ModEffects.isLord(biter)
                && event.getSource().getDirectEntity() == biter
                && !(victim instanceof Spider)
                && !ModEffects.isLord(victim)) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        }
    }

    /** Пауки не нападают на владыку. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Spider && ModEffects.isLord(event.getNewTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Spider && ModEffects.isLord(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Иммунитет к яду. */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (ModEffects.isLord(event.getEntity())
                && event.getEffectInstance().getEffect() == MobEffects.POISON) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Паучья цепкость: падение причиняет на 70% меньше урона. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (ModEffects.isLord(event.getEntity())) {
            event.setDamageMultiplier(0.3F);
        }
    }

    // ------------------------------------------------------------------ жизненный цикл

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && ModEffects.isLord(event.getOriginal())) {
            RESPAWN_AS_LORD.add(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (RESPAWN_AS_LORD.remove(event.getEntity().getUUID())) {
            event.getEntity().addEffect(ModEffects.lordInstance());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_WEB.remove(id);
        ORDERS.remove(id);
        RESPAWN_AS_LORD.remove(id);
    }

    // ------------------------------------------------------------------ доение

    @SubscribeEvent
    public static void onMilk(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Player lord) || !ModEffects.isLord(lord)) return;

        Player milker = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!held.is(Items.BUCKET)) return;

        Level level = event.getLevel();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;

        long now = level.getGameTime();
        CompoundTag data = lord.getPersistentData();
        long last = data.contains(MILK_TAG) ? data.getLong(MILK_TAG) : now - MILK_COOLDOWN;
        if (now - last < MILK_COOLDOWN) {
            milker.displayClientMessage(Component.translatable("message.spiderlord.milk_wait"), true);
            return;
        }
        data.putLong(MILK_TAG, now);

        level.playSound(null, lord.getX(), lord.getY(), lord.getZ(),
                SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0F, 1.0F);
        milker.setItemInHand(event.getHand(),
                ItemUtils.createFilledResult(held, milker, new ItemStack(Items.MILK_BUCKET)));

        milker.sendSystemMessage(Component.translatable("message.spiderlord.milked_milker", lord.getDisplayName()));
        lord.sendSystemMessage(Component.translatable("message.spiderlord.milked_lord", milker.getDisplayName()));
    }
}
