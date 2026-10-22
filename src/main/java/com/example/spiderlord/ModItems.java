package com.example.spiderlord;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SpiderLordMod.MODID);

    public static final RegistryObject<Item> SPIDER_HEART = ITEMS.register("spider_heart",
            () -> new SpiderHeartItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
}
