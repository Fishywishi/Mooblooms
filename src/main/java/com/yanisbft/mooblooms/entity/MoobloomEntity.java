package com.yanisbft.mooblooms.entity;

import com.yanisbft.mooblooms.Mooblooms;
import com.yanisbft.mooblooms.api.Moobloom;
import com.yanisbft.mooblooms.init.MoobloomsEntities;
import net.minecraft.block.Block;
import net.minecraft.block.FlowerBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SuspiciousStewItem;
import net.minecraft.loot.LootTable;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class MoobloomEntity extends CowEntity implements AnimalWithBlockState {
	public Moobloom settings;
	
	public MoobloomEntity(EntityType<? extends MoobloomEntity> entityType, World world) {
		super(entityType, world);
		this.settings = Moobloom.MOOBLOOM_BY_TYPE.get(entityType);
	}
	
	@Override
	public RegistryKey<LootTable> getLootTableId() {
		return this.settings.getEntityType().getLootTableId();
	}
	
	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (stack.getItem() == Items.SHEARS && this.getBreedingAge() >= 0) {
			this.getWorld().addParticle(ParticleTypes.EXPLOSION, this.getX(), this.getY() + this.getHeight() / 2.0F, this.getZ(), 0.0D, 0.0D, 0.0D);
			if (!this.getWorld().isClient) {
				this.discard();
				CowEntity cow = EntityType.COW.create(this.getWorld());
				cow.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
				cow.setHealth(this.getHealth());
				cow.bodyYaw = this.bodyYaw;
				if (this.hasCustomName()) {
					cow.setCustomName(this.getCustomName());
				}
				this.getWorld().spawnEntity(cow);
				for (int i = 0; i < 5; i++) {
					this.getWorld().spawnEntity(new ItemEntity(this.getWorld(), this.getX(), this.getY() + this.getHeight(), this.getZ(), new ItemStack(this.settings.getBlockState().getBlock())));
				}
				stack.damage(1, player, getSlotForHand(hand));
				this.playSound(SoundEvents.ENTITY_MOOSHROOM_SHEAR, 1.0F, 1.0F);
			}
			return ActionResult.success(this.getWorld().isClient);
		} else if (stack.getItem() == Items.MUSHROOM_STEW && this.getBreedingAge() >= 0 && (this.settings.getBlockState().getBlock() instanceof FlowerBlock flowerBlock)) {
			stack.decrement(1);
			ItemStack suspiciousStew = new ItemStack(Items.SUSPICIOUS_STEW);
			suspiciousStew.set(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, flowerBlock.getStewEffects());
			player.setStackInHand(hand, suspiciousStew);
			this.playSound(SoundEvents.ENTITY_MOOSHROOM_SUSPICIOUS_MILK, 1.0F, 1.0F);
			return ActionResult.success(this.getWorld().isClient);
		} else {
			return super.interactMob(player, hand);
		}
	}
	
	@Override
	public MoobloomEntity createChild(ServerWorld world, PassiveEntity entity) {
		return this.settings.getEntityType().create(world);
	}
	
	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance statusEffectInstance) {
		if (this.settings.getIgnoredEffects().contains(statusEffectInstance.getEffectType())) {
			return false;
		}
		
		return super.canHaveStatusEffect(statusEffectInstance);
	}
	
	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		for (RegistryKey<DamageType> ignoredDamageType : this.settings.getIgnoredDamageTypes()) {
			if (source.isOf(ignoredDamageType)) {
				return true;
			}
		}

		return super.isInvulnerableTo(source);
	}
	
	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (!player.getAbilities().creativeMode && player.getPos().isInRange(this.getPos(), 1.5D)) {
			if (this.isWitherRose() && Mooblooms.config.witherRoseMoobloom.damagePlayers) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 200, 0));
			} else if (this.isCowctus() && Mooblooms.config.cowctus.damagePlayers) {
				player.damage(player.getDamageSources().cactus(), 1.0F);
			}
		}
		
		super.onPlayerCollision(player);
	}
	
	@Override
	public void tickMovement() {
		if (this.canSpawnBlocks(this.settings.getConfigCategory())) {
			if (!this.getWorld().isClient && !this.isBaby() && this.settings.canPlaceBlocks()) {
				Block blockUnderneath = this.getWorld().getBlockState(new BlockPos(this.getBlockX(), this.getBlockY() - 1, this.getBlockZ())).getBlock();
				if (this.settings.getValidBlocks().contains(blockUnderneath) && this.getWorld().isAir(this.getBlockPos())) {
					int i = this.random.nextInt(1000);
					if (i == 0) {
						this.placeBlocks(this, this.settings.getBlockState());
					}
				}
			}
		}
		
		if (this.getWorld().isClient && this.settings.getParticle() != null) {
			for (int i = 0; i < 3; i++) {
				this.getWorld().addParticle(this.settings.getParticle(), this.getX() + (this.random.nextDouble() - 0.5D) * this.getWidth(), this.getY() + this.random.nextDouble() * this.getHeight(), this.getZ() + (this.random.nextDouble() - 0.5D) * this.getWidth(), 0.0D, 0.0D, 0.0D);
			}
		}
		
		super.tickMovement();
	}

	@Override
	public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
		return true;
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return true;
	}

	public boolean isWitherRose() {
		return this.settings.equals(MoobloomsEntities.WITHER_ROSE_MOOBLOOM);
	}
	
	public boolean isSuncower() {
		return this.settings.equals(MoobloomsEntities.SUNCOWER);
	}
	
	public boolean isCowctus() {
		return this.settings.equals(MoobloomsEntities.COWCTUS);
	}
}
