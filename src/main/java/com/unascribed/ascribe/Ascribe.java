package com.unascribed.ascribe;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableFloat;
import com.github.gfx.util.WeakIdentityHashMap;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.WorldEvent;

@Mod(name="Ascribe",modid="ascribe",version="@VERSION@",acceptedMinecraftVersions="@MCVERSION@")
public class Ascribe {
	@Instance
	public static Ascribe inst;

	public static boolean isObfEnv;
	
	public static final float EPSILON = 0.05f;
	
	private Map<EntityPlayer, MutableFloat> lastAttackedAtYaw = new WeakIdentityHashMap<>();
	private Configuration config;
	private SimpleNetworkWrapper network;
	
	private List<Runnable> tasks = Lists.newArrayList();;
	
	@EventHandler
	public void onPreInit(FMLPreInitializationEvent e) {
		config = new Configuration(e.getSuggestedConfigurationFile());
		for (AscribeOption ao : AscribeOption.values()) {
			config.getBoolean(ao.getKey(), ao.getCategory(), true, ao.getComment());
		}
		config.save();
		
		network = NetworkRegistry.INSTANCE.newSimpleChannel("Ascribe");
		registerMessage(AttackedAtYawMessage.class, 0, Side.CLIENT);
		
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	private <T extends IMessage & IMessageHandler<T, IMessage>> void registerMessage(Class<T> clazz, int discriminator, Side side) {
		network.registerMessage(clazz, clazz, discriminator, side);
	}
	
	public void doLater(Runnable r) {
		tasks.add(r);
	}

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent e) {
		if (e.phase == Phase.END && !e.player.worldObj.isRemote && e.player instanceof EntityPlayerMP) {
			if (getOption(AscribeOption.SYNC_ATTACKED_AT_YAW)) {
				if (!lastAttackedAtYaw.containsKey(e.player)) {
					lastAttackedAtYaw.put(e.player, new MutableFloat(e.player.attackedAtYaw));
				}
				if (Math.abs(lastAttackedAtYaw.get(e.player).floatValue()-e.player.attackedAtYaw) > EPSILON) {
					AttackedAtYawMessage aaym = new AttackedAtYawMessage();
					aaym.yaw = e.player.attackedAtYaw;
					network.sendTo(aaym, (EntityPlayerMP)e.player);
					lastAttackedAtYaw.get(e.player).setValue(e.player.attackedAtYaw);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load e) {
		if (getOption(AscribeOption.GAMERULE_DO_DOWNFALL)) {
			e.world.getGameRules().addGameRule("doDownfall", "true");
		}
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent e) {
		if (e.phase == Phase.START) {
			for (Runnable r : tasks) {
				r.run();
			}
			tasks.clear();
		}
	}
	
	@SubscribeEvent
	public void onWorldTick(WorldTickEvent e) {
		if (e.phase == Phase.START) {
			if (getOption(AscribeOption.GAMERULE_DO_DOWNFALL)) {
				if (!e.world.getGameRules().getGameRuleBooleanValue("doDownfall")) {
					e.world.getWorldInfo().setRainTime(0);
					e.world.getWorldInfo().setThunderTime(0);
					e.world.getWorldInfo().setRaining(false);
					e.world.getWorldInfo().setThundering(false);
				}
			}
		}
	}
	
	public static boolean getOption(AscribeOption ao) {
		return inst.config.getBoolean(ao.getKey(), ao.getCategory(), true, ao.getComment());
	}

}
