package io.github.aelpecyem.la_cucaracha;

import io.github.aelpecyem.la_cucaracha.items.BottledRoachItem;
import io.github.aelpecyem.la_cucaracha.items.SplashBottledRoachItem;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityType.EntityFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaCucaracha implements ModInitializer {

	public static final String MOD_ID = "la_cucaracha";
	public static final Logger LOGGER = LoggerFactory.getLogger("La Cucaracha");

	public static final TagKey<Structure> ROACH_STRUCTURES = TagKey.of(RegistryKeys.STRUCTURE, id("roach_structure"));
	public static final TagKey<EntityType<?>> ROACH_CARRIERS = TagKey.of(RegistryKeys.ENTITY_TYPE, id("roach_carriers"));

	public static final Identifier PARTICLE_PACKET = id("roach_potion");

	public static final EntityType<RoachEntity> ROACH_ENTITY_TYPE = EntityType.Builder.create(RoachEntity::new, SpawnGroup.MONSTER)
		.setDimensions(0.25F, 0.25F).maxTrackingRange(8).build(MOD_ID + ":roach");
	public static final EntityType<SplashBottledRoachEntity> SPLASH_BOTTLED_ROACH_ENTITY_TYPE =
		EntityType.Builder.create((EntityFactory<SplashBottledRoachEntity>) SplashBottledRoachEntity::new, SpawnGroup.MISC)
			.setDimensions(0.25F, 0.25F).maxTrackingRange(4).trackingTickInterval(10).build(MOD_ID + ":splash_bottled_roach");
	public static final Item BOTTLED_ROACH_ITEM = new BottledRoachItem();
	public static final Item SPLASH_POTION_ROACH_ITEM = new SplashBottledRoachItem();
	public static final Item ROACH_SPAWN_EGG_ITEM = new SpawnEggItem(ROACH_ENTITY_TYPE, 0x3d2a0f, 0x42392c,
																	 new FabricItemSettings());
	public static final SoundEvent ROACH_SCURRY_SOUND_EVENT = SoundEvent.of(id("roach.scurry"));
	public static final SoundEvent ROACH_HURT_SOUND_EVENT = SoundEvent.of(id("roach.hurt"));
	public static final SoundEvent ROACH_DEATH_SOUND_EVENT = SoundEvent.of(id("roach.death"));

	public static final DefaultParticleType ROACH_PARTICLE_EFFECT = FabricParticleTypes.simple();
	public static final RegistryKey<DamageType> ROACH = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id("roach"));

	public static DamageSource create(RegistryKey<DamageType> t, World world) {
		return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(t));
	}

	public static void sendRoachPotionPacket(Entity entity) {
		PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
		data.writeDouble((entity.getX()));
		data.writeDouble(entity.getY());
		data.writeDouble(entity.getZ());
		data.writeBoolean(true);
		if (entity.getWorld() instanceof ServerWorld) {
			PlayerLookup.tracking(entity).forEach(p -> ServerPlayNetworking.send(p, PARTICLE_PACKET, data));
		}
	}

	public static void sendRoachPotionPacket(ServerWorld world, Vec3d pos) {
		PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
		data.writeDouble((pos.getX()));
		data.writeDouble(pos.getY());
		data.writeDouble(pos.getZ());
		data.writeBoolean(false);
		PlayerLookup.tracking(world, new BlockPos((int) pos.getX(), (int) pos.getX(), (int) pos.getZ())).forEach(p -> ServerPlayNetworking.send(p, PARTICLE_PACKET, data));
	}

	@Override
	public void onInitialize() {
		LaCucarachaConfig.init(MOD_ID, LaCucarachaConfig.class);
		Registry.register(Registries.ENTITY_TYPE, id("roach"), ROACH_ENTITY_TYPE);
		FabricDefaultAttributeRegistry.register(ROACH_ENTITY_TYPE, RoachEntity.createRoachAttributes());
		Registry.register(Registries.ENTITY_TYPE, id("splash_bottled_roach"), SPLASH_BOTTLED_ROACH_ENTITY_TYPE);
		Registry.register(Registries.ITEM, id("bottled_roach"), BOTTLED_ROACH_ITEM);
		Registry.register(Registries.ITEM, id("splash_bottled_roach"), SPLASH_POTION_ROACH_ITEM);
		Registry.register(Registries.ITEM, id("roach_spawn_egg"), ROACH_SPAWN_EGG_ITEM);
		Registry.register(Registries.SOUND_EVENT, id("roach.scurry"), ROACH_SCURRY_SOUND_EVENT);
		Registry.register(Registries.SOUND_EVENT, id("roach.hurt"), ROACH_HURT_SOUND_EVENT);
		Registry.register(Registries.SOUND_EVENT, id("roach.death"), ROACH_DEATH_SOUND_EVENT);
		Registry.register(Registries.PARTICLE_TYPE, id("roaches"), ROACH_PARTICLE_EFFECT);
		RoachSpawner roachSpawner = new RoachSpawner();
		ServerTickEvents.END_WORLD_TICK.register(world ->
			roachSpawner.spawn(world, world.getDifficulty() != Difficulty.PEACEFUL,
				world.getServer().shouldSpawnAnimals()));
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
			if (LaCucarachaConfig.roachSpawnEnabledCarriers) {
				Random random = killedEntity.getRandom();
				if (killedEntity.getType().isIn(ROACH_CARRIERS) && random.nextInt(8) == 0) {
					RoachEntity.spawnRoaches(world, entity instanceof LivingEntity l ? l : null,
							killedEntity.getPos().add(0, killedEntity.getHeight() / 2, 0),
							random, 1 + random.nextInt(3), false);
					sendRoachPotionPacket(world, killedEntity.getPos().add(0, killedEntity.getHeight() / 2, 0));
				}
			}
		});

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(BOTTLED_ROACH_ITEM));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(SPLASH_POTION_ROACH_ITEM));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(ROACH_SPAWN_EGG_ITEM));

	}

	public static Identifier id(String s) {
		return new Identifier(MOD_ID, s);
	}
}
