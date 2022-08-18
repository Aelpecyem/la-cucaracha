package io.github.aelpecyem.la_cucaracha;

import net.minecraft.data.server.tag.StructureTags;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.world.GameRules;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.List;

public class RoachSpawner implements Spawner {
	public static final int SPAWN_RADIUS = 64;
	public static final int ITEM_SPAWN_RADIUS = 16;
	private int ticksUntilNextSpawn;


	public int spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals) {
		if (spawnAnimals && world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
			--this.ticksUntilNextSpawn;
			if (this.ticksUntilNextSpawn <= 0) {
				this.ticksUntilNextSpawn = 100;
				RandomGenerator random = world.random;
				world.getPlayers().forEach(serverPlayerEntity -> {
					boolean fromFood = random.nextBoolean();
					int x = (8 + random.nextInt(fromFood ? 12 : 32)) * (random.nextBoolean() ? -1 : 1);
					int y = (random.nextInt(4)) * (random.nextBoolean() ? -1 : 1);
					int z = (8 + random.nextInt(fromFood ? 12 : 32)) * (random.nextBoolean() ? -1 : 1);
					BlockPos blockPos = serverPlayerEntity.getBlockPos().add(x, y, z);

					if (RoachEntity.canMobSpawn(LaCucaracha.ROACH_ENTITY_TYPE, world, SpawnReason.NATURAL, blockPos, world.getRandom())) {
						if (fromFood) {
							spawnFromFood(world, blockPos);
						} else {
							spawnFromStructure(world, blockPos);
						}
					}
				});
			}
		}

		return 0;
	}

	private void spawnFromFood(ServerWorld world, BlockPos blockPos) {
		int i = world.getEntitiesByType(TypeFilter.instanceOf(ItemEntity.class), new Box(blockPos.getX() - ITEM_SPAWN_RADIUS,
																						 blockPos.getY() - ITEM_SPAWN_RADIUS,
																						 blockPos.getZ() - ITEM_SPAWN_RADIUS,
																						 blockPos.getX() + ITEM_SPAWN_RADIUS,
																						 blockPos.getY() + ITEM_SPAWN_RADIUS,
																						 blockPos.getZ() + ITEM_SPAWN_RADIUS),
								item -> item.getStack().isFood() && item.isOnGround()).size();
		if (i > 0) {
			if (SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, world, blockPos, LaCucaracha.ROACH_ENTITY_TYPE)) {
				this.spawn(blockPos, world);
			}
		}
	}

	private void spawnFromStructure(ServerWorld world, BlockPos blockPos) {
		BlockPos structurePos = world.findFirstPos(LaCucaracha.ROACH_STRUCTURES, blockPos, 5, false);
		boolean isVillage = structurePos == world.findFirstPos(StructureTags.VILLAGE, blockPos, 5, false);
		if (structurePos != null && blockPos.getManhattanDistance(structurePos) <= 300) {
			if (isVillage) {
				List<VillagerEntity> villagersNearby = world.getEntitiesByType(EntityType.VILLAGER,
																			   new Box(blockPos.getX() - SPAWN_RADIUS,
																					   blockPos.getY() - SPAWN_RADIUS,
																					   blockPos.getZ() - SPAWN_RADIUS,
																					   blockPos.getX() + SPAWN_RADIUS,
																					   blockPos.getY() + SPAWN_RADIUS,
																					   blockPos.getZ() + SPAWN_RADIUS),
																			   villagerEntity -> true);

				if (villagersNearby.isEmpty() && world.isRegionLoaded(
					blockPos.getX() - 10, blockPos.getY() - 10, blockPos.getZ() - 10,
					blockPos.getX() + 10, blockPos.getY() + 10, blockPos.getZ() + 10)) {
					if (SpawnHelper
						.canSpawn(SpawnRestriction.Location.ON_GROUND, world, blockPos, LaCucaracha.ROACH_ENTITY_TYPE)) {
						this.spawnInHouse(world, blockPos);
					}
				}
			} else if (world.isRegionLoaded(
				blockPos.getX() - 10, blockPos.getY() - 10, blockPos.getZ() - 10,
				blockPos.getX() + 10, blockPos.getY() + 10, blockPos.getZ() + 10)) {
				if (SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, world, blockPos, LaCucaracha.ROACH_ENTITY_TYPE)) {
					this.spawn(blockPos, world);
				}
			}
		}
	}

	private int spawnInHouse(ServerWorld world, BlockPos pos) {
		if (world.getPointOfInterestStorage().count((registryEntry) -> registryEntry.isRegistryKey(PointOfInterestTypes.HOME), pos,
													48, PointOfInterestStorage.OccupationStatus.HAS_SPACE) > 4) {
			List<RoachEntity> list = world.getNonSpectatingEntities(RoachEntity.class, (new Box(pos)).expand(48.0D, 8.0D, 48.0D));
			if (list.size() < 12) {
				return this.spawn(pos, world);
			}
		}

		return 0;
	}

	private int spawn(BlockPos pos, ServerWorld world) {
		int amount = 1 + world.random.nextInt(2);
		int i = 0;
		while (i < amount) {
			RoachEntity roach = LaCucaracha.ROACH_ENTITY_TYPE.create(world);
			if (roach == null) {
				break;
			} else {
				roach.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);
				roach.refreshPositionAndAngles(pos, 0.0F, 0.0F);
				world.spawnEntityAndPassengers(roach);
				i++;
			}
		}
		return i;
	}
}