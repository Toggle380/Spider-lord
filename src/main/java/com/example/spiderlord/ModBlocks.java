package com.example.spiderlord;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SpiderLordMod.MODID);

    public static final RegistryObject<Block> SPIDER_SILK =
            BLOCKS.register("spider_silk", SpiderSilkBlock::new);
}
