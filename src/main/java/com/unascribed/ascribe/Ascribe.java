package com.unascribed.ascribe;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableFloat;

import com.github.gfx.util.WeakIdentityHashMap;
import com.google.common.collect.Lists;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.GameRules.ValueType;
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
			e.world.getGameRules().addGameRule("doDownfall", "true", ValueType.BOOLEAN_VALUE);
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
				if (!e.world.getGameRules().getBoolean("doDownfall")) {
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
