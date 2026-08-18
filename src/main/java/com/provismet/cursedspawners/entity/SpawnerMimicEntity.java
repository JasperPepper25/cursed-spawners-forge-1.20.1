/*
 * MODIFIED from Cursed Spawners by Provismet (tag 1.0.0-mc1.21).
 * Unofficial native Forge 1.20.1 backport; see NOTICE and LICENSE.
 */
package com.provismet.cursedspawners.entity;

import com.provismet.cursedspawners.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Function;

public final class SpawnerMimicEntity extends Monster {
    private static final EntityDataAccessor<CompoundTag> DISPLAY_ENTITY =
            SynchedEntityData.defineId(SpawnerMimicEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private int spawnDelay = 20;
    private ListTag spawnPotentials = new ListTag();
    private CompoundTag spawnData = defaultSpawnData();
    private double mobRotation;
    private double prevMobRotation;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    private int maxNearbyEntities = 6;
    private int requiredPlayerRange = 16;
    private int spawnRange = 4;

    private int appearAnimationTicks = 40;
    private boolean clientSpawnAnimationStarted = false;
    public final AnimationState idleState = new AnimationState();
    public final AnimationState attackState = new AnimationState();
    public final AnimationState spawnState = new AnimationState();
    @Nullable private Entity clientDisplayEntity;
    private CompoundTag clientDisplayEntityTag = new CompoundTag();

    public SpawnerMimicEntity(EntityType<? extends SpawnerMimicEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.ARMOR, 25.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.275D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new AppearGoal());
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // Entity calls this during its superclass constructor, before this class's field initializers run.
        // Seed the tracked display with a standalone default tag; loadSpawnerData/readAdditionalSaveData
        // will replace it after construction.
        this.entityData.define(DISPLAY_ENTITY, defaultSpawnData().getCompound("entity").copy());
    }

    @Override
    public void tick() {
        this.prevMobRotation = this.mobRotation;
        this.mobRotation = (this.mobRotation + 1000.0D / ((double)this.spawnDelay + 200.0D)) % 360.0D;

        if (this.appearAnimationTicks > 0) --this.appearAnimationTicks;
        this.setupAnimationStates();

        if (!this.isNoAi() && this.level() instanceof ServerLevel serverLevel) {
            this.serverSpawnerTick(serverLevel);
        }

        if (this.level().isClientSide && this.getRenderedEntity() != null) {
            double particleX = this.getX() + this.random.nextDouble() - 0.5D;
            double particleY = this.getY() + this.random.nextDouble();
            double particleZ = this.getZ() + this.random.nextDouble() - 0.5D;
            this.level().addParticle(ParticleTypes.SMOKE, particleX, particleY, particleZ, 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.FLAME, particleX, particleY, particleZ, 0.0D, 0.0D, 0.0D);
        }

        super.tick();
    }

    private void setupAnimationStates() {
        if (!this.getNavigation().isInProgress()) this.idleState.startIfStopped(this.tickCount);
        else this.idleState.stop();

        if (!this.isAggressive()) this.attackState.stop();

        // Fabric's onSpawnPacket starts this state when the entity first reaches
        // the client. Forge's spawn packet path differs, so initialize it on the
        // first client tick, before the first normal render.
        if (this.level().isClientSide && !this.clientSpawnAnimationStarted) {
            this.spawnState.start(this.tickCount);
            this.clientSpawnAnimationStarted = true;
        }
        if (this.level().isClientSide && this.appearAnimationTicks <= 0) {
            this.spawnState.stop();
        }
    }

    private void serverSpawnerTick(ServerLevel level) {
        if (this.spawnDelay < 0) this.resetSpawnDelay(level);
        if (this.spawnDelay > 0) {
            --this.spawnDelay;
            return;
        }

        boolean spawnedAny = false;
        for (int attempt = 0; attempt < this.spawnCount; ++attempt) {
            CompoundTag entityTag = entityTagFromSpawnData(this.spawnData);
            Optional<EntityType<?>> optionalType = EntityType.by(entityTag);
            if (optionalType.isEmpty()) {
                this.resetSpawnDelay(level);
                return;
            }

            EntityType<?> entityType = optionalType.get();
            ListTag position = entityTag.getList("Pos", Tag.TAG_DOUBLE);
            int positionSize = position.size();
            double x = positionSize >= 1 ? position.getDouble(0)
                    : this.getX() + (level.random.nextDouble() - level.random.nextDouble()) * this.spawnRange + 0.5D;
            double y = positionSize >= 2 ? position.getDouble(1)
                    : this.getY() + level.random.nextInt(3) - 1;
            double z = positionSize >= 3 ? position.getDouble(2)
                    : this.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * this.spawnRange + 0.5D;

            // Vanilla's spawner path checks the type's prospective spawn box
            // before spawn-rule RNG is consumed. Preserve that ordering.
            if (!level.noCollision(entityType.getAABB(x, y, z))) continue;
            BlockPos targetPos = BlockPos.containing(x, y, z);

            boolean hasCustomSpawnRules = this.spawnData.contains("custom_spawn_rules", Tag.TAG_COMPOUND);
            if (hasCustomSpawnRules) {
                if (!this.customSpawnRulesAllow(level, entityType, targetPos)) continue;
            } else if (!SpawnPlacements.checkSpawnRules(entityType, level, MobSpawnType.SPAWNER, targetPos, level.random)) {
                continue;
            }

            final double spawnX = x;
            final double spawnY = y;
            final double spawnZ = z;
            Entity entity = EntityType.loadEntityRecursive(entityTag, level, loadedEntity -> {
                loadedEntity.moveTo(spawnX, spawnY, spawnZ, loadedEntity.getYRot(), loadedEntity.getXRot());
                return loadedEntity;
            });
            if (entity == null) {
                this.resetSpawnDelay(level);
                return;
            }
            AABB nearbyBox = new AABB(this.getX(), this.getY(), this.getZ(),
                    this.getX() + 1.0D, this.getY() + 1.0D, this.getZ() + 1.0D).inflate(this.spawnRange);
            int nearby = level.getEntities(entity, nearbyBox,
                    candidate -> !candidate.isSpectator() && candidate.getClass() == entity.getClass()).size();
            if (nearby >= this.maxNearbyEntities) {
                this.resetSpawnDelay(level);
                return;
            }

            // The original randomizes yaw only after collision/nearby checks.
            entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
            if (entity instanceof Mob mob) {
                // Forge patches vanilla BaseSpawner to fire PositionCheck even for spawner
                // mobs with custom light rules. Mimics have no BaseSpawner instance, but
                // Forge explicitly permits a null spawner reference for SPAWNER events.
                MobSpawnEvent.PositionCheck positionEvent =
                        new MobSpawnEvent.PositionCheck(mob, level, MobSpawnType.SPAWNER, null);
                MinecraftForge.EVENT_BUS.post(positionEvent);
                boolean positionAllowed = switch (positionEvent.getResult()) {
                    case ALLOW -> true;
                    case DENY -> false;
                    default -> (hasCustomSpawnRules || mob.checkSpawnRules(level, MobSpawnType.SPAWNER))
                            && mob.checkSpawnObstruction(level);
                };
                if (!positionAllowed) continue;

                // Forge's BaseSpawner patch fires FinalizeSpawn for every mob loaded by a
                // spawner, even when vanilla would skip finalizeSpawn because entity NBT
                // contains more than just an id. Preserve that event behavior here.
                MobSpawnEvent.FinalizeSpawn finalizeEvent = ForgeEventFactory.onFinalizeSpawnSpawner(
                        mob, level, level.getCurrentDifficultyAt(targetPos), null, entityTag, null);
                if (finalizeEvent != null && entityTag.size() == 1 && entityTag.contains("id", Tag.TAG_STRING)) {
                    mob.finalizeSpawn(level, finalizeEvent.getDifficulty(), finalizeEvent.getSpawnType(),
                            finalizeEvent.getSpawnData(), finalizeEvent.getSpawnTag());
                }
            }

            if (!level.tryAddFreshEntityWithPassengers(entity)) {
                this.resetSpawnDelay(level);
                return;
            }
            level.levelEvent(2004, this.blockPosition(), 0);
            level.gameEvent(entity, GameEvent.ENTITY_PLACE, targetPos);
            if (entity instanceof Mob mob) mob.spawnAnim();
            spawnedAny = true;
        }
        if (spawnedAny) this.resetSpawnDelay(level);
    }

    private boolean customSpawnRulesAllow(ServerLevel level, EntityType<?> type, BlockPos pos) {
        if (!type.getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL) return false;
        CompoundTag rules = this.spawnData.getCompound("custom_spawn_rules");
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        return lightRangeContains(rules, "block_light_limit", blockLight)
                && lightRangeContains(rules, "sky_light_limit", skyLight);
    }

    /** Supports the vanilla inclusive-range NBT shape and fixed numeric values. */
    private static boolean lightRangeContains(CompoundTag rules, String key, int value) {
        if (!rules.contains(key)) return true;
        if (rules.contains(key, Tag.TAG_ANY_NUMERIC)) return value == rules.getInt(key);
        if (!rules.contains(key, Tag.TAG_COMPOUND)) return true;
        CompoundTag range = rules.getCompound(key);
        int min = range.contains("min_inclusive", Tag.TAG_ANY_NUMERIC) ? range.getInt("min_inclusive") : 0;
        int max = range.contains("max_inclusive", Tag.TAG_ANY_NUMERIC) ? range.getInt("max_inclusive") : 15;
        return value >= min && value <= max;
    }

    private void resetSpawnDelay(ServerLevel level) {
        if (this.maxSpawnDelay <= this.minSpawnDelay) this.spawnDelay = this.minSpawnDelay;
        else this.spawnDelay = this.minSpawnDelay + level.random.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        this.chooseSpawnData(level);
    }

    private void chooseSpawnData(ServerLevel level) {
        if (this.spawnPotentials.isEmpty()) return;
        int totalWeight = 0;
        for (int i = 0; i < this.spawnPotentials.size(); ++i) {
            CompoundTag entry = this.spawnPotentials.getCompound(i);
            totalWeight += Math.max(1, entry.getInt("weight"));
        }
        int roll = level.random.nextInt(Math.max(1, totalWeight));
        for (int i = 0; i < this.spawnPotentials.size(); ++i) {
            CompoundTag entry = this.spawnPotentials.getCompound(i);
            roll -= Math.max(1, entry.getInt("weight"));
            if (roll < 0) {
                CompoundTag data = entry.contains("data", Tag.TAG_COMPOUND) ? entry.getCompound("data") : entry;
                this.setSpawnData(data.copy());
                return;
            }
        }
    }

    /** Loads exactly the vanilla spawner keys carried by a broken SpawnerBlockEntity. */
    public void loadSpawnerData(CompoundTag tag) {
        if (tag.contains("Delay", Tag.TAG_ANY_NUMERIC)) this.spawnDelay = tag.getShort("Delay");
        if (tag.contains("MinSpawnDelay", Tag.TAG_ANY_NUMERIC)) this.minSpawnDelay = tag.getShort("MinSpawnDelay");
        if (tag.contains("MaxSpawnDelay", Tag.TAG_ANY_NUMERIC)) this.maxSpawnDelay = tag.getShort("MaxSpawnDelay");
        if (tag.contains("SpawnCount", Tag.TAG_ANY_NUMERIC)) this.spawnCount = tag.getShort("SpawnCount");
        if (tag.contains("MaxNearbyEntities", Tag.TAG_ANY_NUMERIC)) this.maxNearbyEntities = tag.getShort("MaxNearbyEntities");
        if (tag.contains("RequiredPlayerRange", Tag.TAG_ANY_NUMERIC)) this.requiredPlayerRange = tag.getShort("RequiredPlayerRange");
        if (tag.contains("SpawnRange", Tag.TAG_ANY_NUMERIC)) this.spawnRange = tag.getShort("SpawnRange");
        if (tag.contains("SpawnData", Tag.TAG_COMPOUND)) this.setSpawnData(tag.getCompound("SpawnData").copy());
        if (tag.contains("SpawnPotentials", Tag.TAG_LIST)) this.spawnPotentials = tag.getList("SpawnPotentials", Tag.TAG_COMPOUND).copy();
        ensurePotentials();
    }

    private void setSpawnData(CompoundTag data) {
        this.spawnData = data.isEmpty() ? defaultSpawnData() : data;
        this.entityData.set(DISPLAY_ENTITY, entityTagFromSpawnData(this.spawnData));
        this.clientDisplayEntity = null;
        this.clientDisplayEntityTag = new CompoundTag();
    }

    private void setSpawnEntityType(EntityType<?> type) {
        CompoundTag data = this.spawnData.copy();
        CompoundTag entity = entityTagFromSpawnData(data);
        ResourceLocationHelper.putEntityId(entity, type);
        if (data.contains("entity", Tag.TAG_COMPOUND)) data.put("entity", entity);
        else data = entity;
        this.setSpawnData(data);
        this.spawnDelay = 20;
        this.ensurePotentials();
    }

    private static CompoundTag defaultSpawnData() {
        CompoundTag data = new CompoundTag();
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:pig");
        data.put("entity", entity);
        return data;
    }

    private static CompoundTag entityTagFromSpawnData(CompoundTag data) {
        if (data.contains("entity", Tag.TAG_COMPOUND)) return data.getCompound("entity").copy();
        if (data.contains("id", Tag.TAG_STRING)) return data.copy();
        return defaultSpawnData().getCompound("entity").copy();
    }

    private void ensurePotentials() {
        if (!this.spawnPotentials.isEmpty()) return;
        CompoundTag weighted = new CompoundTag();
        weighted.putInt("weight", 1);
        weighted.put("data", this.spawnData.copy());
        this.spawnPotentials.add(weighted);
    }

    @Nullable
    public Entity getRenderedEntity() {
        if (!this.level().isClientSide) return null;
        CompoundTag displayed = this.entityData.get(DISPLAY_ENTITY);
        if (this.clientDisplayEntity == null || !displayed.equals(this.clientDisplayEntityTag)) {
            this.clientDisplayEntityTag = displayed.copy();
            this.clientDisplayEntity = EntityType.loadEntityRecursive(displayed, this.level(), Function.identity());
        }
        return this.clientDisplayEntity;
    }

    public float getMobRotation(float partialTick) {
        return (float)Mth.lerp(partialTick, this.prevMobRotation, this.mobRotation);
    }


    @Override
    public boolean doHurtTarget(Entity target) {
        // Preserve the original status-4 packet for the client attack animation.
        // 1.20.1 lacks the later generic Mob.playAttackSound() hook, so the
        // custom sound is broadcast explicitly on the server.
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte)4);
            this.playSound(ModRegistry.ENTITY_MIMIC_ATTACK.get(), 1.0F, 1.0F);
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 4) {
            this.attackState.start(this.tickCount);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof SpawnEggItem spawnEgg) {
            if (!this.level().isClientSide) {
                this.setSpawnEntityType(spawnEgg.getType(held.getTag()));
                this.level().gameEvent(player, GameEvent.ENTITY_INTERACT, this.blockPosition());
            }
            // The original consumes the egg even for creative players.
            held.shrink(1);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
        if (!state.is(Blocks.COBWEB)) super.makeStuckInBlock(state, multiplier);
    }

    @Override protected SoundEvent getAmbientSound() { return ModRegistry.ENTITY_MIMIC_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModRegistry.ENTITY_MIMIC_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModRegistry.ENTITY_MIMIC_DEATH.get(); }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { this.playSound(ModRegistry.ENTITY_MIMIC_STEP.get(), 1.0F, 1.0F); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putShort("Delay", (short)this.spawnDelay);
        tag.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
        tag.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        tag.putShort("SpawnCount", (short)this.spawnCount);
        tag.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        tag.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        tag.putShort("SpawnRange", (short)this.spawnRange);
        tag.put("SpawnData", this.spawnData.copy());
        tag.put("SpawnPotentials", this.spawnPotentials.copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.loadSpawnerData(tag);
    }

    private final class AppearGoal extends Goal {
        private AppearGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
        }
        @Override public boolean canUse() { return SpawnerMimicEntity.this.appearAnimationTicks > 0; }
        @Override public boolean canContinueToUse() { return SpawnerMimicEntity.this.appearAnimationTicks > 0; }
        @Override public void start() { SpawnerMimicEntity.this.getNavigation().stop(); }
        @Override public void tick() { SpawnerMimicEntity.this.getNavigation().stop(); }
    }

    /** Isolates the registry lookup so the rest of the NBT code remains loader-neutral. */
    private static final class ResourceLocationHelper {
        private static void putEntityId(CompoundTag tag, EntityType<?> type) {
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id != null) tag.putString("id", id.toString());
        }
    }
}
