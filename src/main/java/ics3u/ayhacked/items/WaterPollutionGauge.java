package ics3u.ayhacked.items;

import ics3u.ayhacked.AYHackED;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class WaterPollutionGauge extends Item {
    public WaterPollutionGauge() {
        super(new Properties().group(AYHackED.GROUP));
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        if (player != null) {
            RayTraceResult ray = rayTrace(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);
            if (ray.getType() == RayTraceResult.Type.MISS) {
                return ActionResultType.FAIL;
            } else {
                BlockPos blockpos = ((BlockRayTraceResult) ray).getPos();
                if (!world.isBlockModifiable(player, blockpos)) {
                    return ActionResultType.FAIL;
                }
                BlockState state = world.getBlockState(blockpos);
                if (state.getFluidState().isTagged(FluidTags.WATER)) {
                    ItemStack stack = context.getItem();
                    world.playSound(player, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    CompoundNBT nbt = stack.getOrCreateChildTag("water_pollution_gauge");
                    nbt.putInt("scan_progress", 100);
                    nbt.put("scan_pos", NBTUtil.writeBlockPos(blockpos));
                    return ActionResultType.SUCCESS;
                }
            }
            player.sendStatusMessage(new TranslationTextComponent("ayhacked.message.gauge_failed"), true);
        }
        return ActionResultType.FAIL;
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);

        if (entityIn instanceof PlayerEntity) {
            CompoundNBT nbt = stack.getOrCreateChildTag("water_pollution_gauge");
            int progress = nbt.getInt("scan_progress");
            CompoundNBT posNBT = (CompoundNBT) nbt.get("scan_pos");
            if (posNBT == null || progress == 0) return;
            ChunkPos pos = new ChunkPos(NBTUtil.readBlockPos(posNBT));
            nbt.putInt("scan_progress", progress-1);
            ((PlayerEntity) entityIn).sendStatusMessage(new TranslationTextComponent("ayhacked.message.gauge_scanning", progress), true);
        }
    }
}