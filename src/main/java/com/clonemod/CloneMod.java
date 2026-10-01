package com.clonemod;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CloneMod.MODID)
public class CloneMod {
    public static final String MODID = "clonemod";

    public CloneMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(modBus);
    }
}
