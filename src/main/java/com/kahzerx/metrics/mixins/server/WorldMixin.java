package com.kahzerx.metrics.mixins.server;

import com.kahzerx.metrics.helpers.ServerCollectorInterface;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(World.class)
public abstract class WorldMixin {
    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Shadow public abstract WorldChunk getWorldChunk(BlockPos pos);

    @Shadow @Nullable public abstract MinecraftServer getServer();

    @Shadow public abstract boolean shouldTickBlockPos(BlockPos pos);

    @Inject(method = "tickEntity", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", shift = At.Shift.BEFORE))
    private <T extends Entity> void onTick(Consumer<T> tickConsumer, T entity, CallbackInfo ci) {
        MinecraftServer server = entity.getServer();
        if (server == null) {
            return;
        }
        ((ServerCollectorInterface) server).getEntityProfiler().onEntityTick(entity.getName().getString(), entity.getWorld().getRegistryKey().getValue().getPath());
    }

    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/BlockEntityTickInvoker;getPos()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos onTickBE(BlockEntityTickInvoker instance) {
        BlockPos pos = instance.getPos();
        MinecraftServer server = getServer();
        if (server != null && shouldTickBlockPos(pos)) {
            ((ServerCollectorInterface) server).getBlockEntityProfiler().onBlockEntityTick(getBlockState(pos).getBlock().getName().getString(), getWorldChunk(pos).getWorld().getRegistryKey().getValue().getPath());
        }
        return instance.getPos();
    }
}
