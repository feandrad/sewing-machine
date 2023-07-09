/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.theelm.sewingmachine.protection.interfaces;

import net.minecraft.text.Text;
import net.theelm.sewingmachine.base.config.SewBaseConfig;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.protection.enums.ClaimPermissions;
import net.theelm.sewingmachine.protection.enums.ClaimSettings;
import net.theelm.sewingmachine.exceptions.TranslationKeyException;
import net.theelm.sewingmachine.protection.objects.ClaimCache;
import net.theelm.sewingmachine.protection.utilities.ClaimChunkUtils.ClaimSlice;
import net.theelm.sewingmachine.protection.claims.ClaimantPlayer;
import net.theelm.sewingmachine.protection.claims.ClaimantTown;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface IClaimedChunk {
    
    ClaimantTown updateTownOwner(@Nullable UUID owner, boolean fresh);
    default ClaimantTown updateTownOwner(@Nullable UUID owner) {
        return this.updateTownOwner(owner, true);
    }
    ClaimantPlayer updatePlayerOwner(@Nullable UUID owner, boolean fresh);
    default ClaimantPlayer updatePlayerOwner(@Nullable UUID owner) {
        return this.updatePlayerOwner(owner, true);
    }
    boolean canPlayerClaim(@NotNull ClaimantPlayer player, boolean stopIfClaimed) throws TranslationKeyException;
    
    @Nullable ClaimCache getClaimCache();
    @Nullable UUID getOwnerId();
    @Nullable UUID getOwnerId(@Nullable BlockPos pos);
    @Nullable ClaimantPlayer getOwner();
    @Nullable UUID getTownId();
    @Nullable ClaimantTown getTown();
    
    default MutableText getOwnerName() {
        return this.getOwnerName((UUID) null);
    }
    default MutableText getOwnerName(@Nullable UUID zonePlayer) {
        UUID owner = this.getOwnerId();
        if ( owner == null )
            return Text.literal(SewConfig.get(SewBaseConfig.NAME_WILDERNESS))
                .formatted(Formatting.GREEN);
        
        // Get the owner of the chunk
        ClaimantPlayer chunkPlayer = this.getClaimCache().getPlayerClaim(owner);
        
        // Get the owners name (Colored using the relation to the zonePlayer)
        return chunkPlayer.getName(zonePlayer);
    }
    default MutableText getOwnerName(@Nullable PlayerEntity zonePlayer) {
        return this.getOwnerName(zonePlayer, zonePlayer == null ? null : zonePlayer.getBlockPos());
    }
    default MutableText getOwnerName(@Nullable PlayerEntity zonePlayer, @Nullable BlockPos pos) {
        return this.getOwnerName(zonePlayer == null ? null : zonePlayer.getUuid(), pos);
    }
    default MutableText getOwnerName(@Nullable UUID zonePlayer, @Nullable BlockPos pos) {
        UUID owner = this.getOwnerId(pos);
        if ( owner == null )
            return Text.literal(SewConfig.get(SewBaseConfig.NAME_WILDERNESS))
                .formatted(Formatting.GREEN);
        
        // Get the owner of the chunk
        ClaimantPlayer chunkPlayer = this.getClaimCache().getPlayerClaim(owner);
        
        // Get the owners name (Colored using the relation to the zonePlayer)
        return chunkPlayer.getName(zonePlayer);
    }
    
    boolean canPlayerDo(@NotNull BlockPos blockPos, @Nullable UUID player, @Nullable ClaimPermissions perm);
    boolean isSetting(@NotNull BlockPos pos, @NotNull ClaimSettings setting);
    
    /*
     * Claim Slices
     */
    @NotNull NbtList serializeSlices();
    void deserializeSlices(@NotNull NbtList serialized);
    
    default void updateSliceOwner(@Nullable UUID owner, int slicePos) {
        this.updateSliceOwner(owner, slicePos, 0, 256);
    }
    default void updateSliceOwner(@Nullable UUID owner, int slicePos, int yFrom, int yTo) {
        this.updateSliceOwner(owner, slicePos, yFrom, yTo, true);
    }
    void updateSliceOwner(UUID owner, int slicePos, int yFrom, int yTo, boolean fresh);
    UUID[] getSliceOwner(int slicePos, int yFrom, int yTo);
    @NotNull ClaimSlice[] getSlices();
    void setSlices(@NotNull ClaimSlice[] slices);
    
    /*
     * Statics
     */
    static boolean isOwnedAround(@NotNull final World world, @NotNull final BlockPos blockPos, int leniency) {
        return IClaimedChunk.getOwnedAround( world, blockPos, leniency).length > 0;
    }
    static @NotNull IClaimedChunk[] getOwnedAround(@NotNull final World world, @NotNull final BlockPos blockPos, final int radius) {
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;
        
        List<IClaimedChunk> claimedChunks = new ArrayList<>();
        
        // For the X axis
        for ( int x = chunkX - radius; x <= chunkX + radius; x++ ) {
            // For the Z axis
            for ( int z = chunkZ - radius; z <= chunkZ + radius; z++ ) {
                // Create the chunk position
                WorldChunk worldChunk = world.getWorldChunk(new BlockPos( x << 4, 0, z << 4 ));
                
                // If the chunk is claimed
                if (((IClaimedChunk) worldChunk).getOwnerId() != null)
                    claimedChunks.add( (IClaimedChunk)worldChunk );
            }
        }
        
        return claimedChunks.toArray(new IClaimedChunk[0]);
    }
    
}
