package ics3u.ayhacked.items;

import ics3u.ayhacked.AYHackED;
import ics3u.ayhacked.registration.ModCapabilities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public class WaterPollutionGauge extends Item {
    public WaterPollutionGauge() {
        super(new Properties().group(AYHackED.GROUP).maxStackSize(1));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand handIn) {
        RayTraceResult ray = rayTrace(world, player, RayTraceContext.FluidMode.SOURCE_ONLY);
        ItemStack stack = player.getHeldItem(handIn);
        if (ray.getType() == RayTraceResult.Type.MISS) {
            return ActionResult.resultFail(stack);
        } else {
            BlockPos blockpos = ((BlockRayTraceResult) ray).getPos();
            if (!world.isBlockModifiable(player, blockpos)) {
                player.sendStatusMessage(new TranslationTextComponent("ayhacked.message.gauge_failed"), true);
                return ActionResult.resultFail(stack);
            }

            Biome.Category category = world.getBiome(blockpos).getCategory();

            if (category != Biome.Category.OCEAN && category != Biome.Category.BEACH) {
                player.sendStatusMessage(new TranslationTextComponent("ayhacked.message.gauge_failed"), true);
                return ActionResult.resultFail(stack);
            }
            BlockState state = world.getBlockState(blockpos);
            if (state.getFluidState().isTagged(FluidTags.WATER)) {
                world.playSound(player, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                CompoundNBT nbt = stack.getOrCreateChildTag("water_pollution_gauge");
                nbt.putInt("scan_progress", 100);
                nbt.put("scan_pos", NBTUtil.writeBlockPos(blockpos));
                return ActionResult.resultSuccess(stack);
            }
        }
        player.sendStatusMessage(new TranslationTextComponent("ayhacked.message.gauge_failed"), true);
        return ActionResult.resultFail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);

        if (entityIn instanceof PlayerEntity) {
            PlayerEntity player = ((PlayerEntity) entityIn);
            CompoundNBT nbt = stack.getOrCreateChildTag("water_pollution_gauge");
            int progress = nbt.getInt("scan_progress");
            CompoundNBT posNBT = (CompoundNBT) nbt.get("scan_pos");
            if (posNBT == null || progress == 0) return;
            progress--;
            nbt.putInt("scan_progress", progress);
            if (progress <= 0) {
                Chunk chunk = (Chunk) worldIn.getChunk(NBTUtil.readBlockPos(posNBT));
                chunk.getCapability(ModCapabilities.WATER_POLLUTION_CAPABILITY).ifPresent(cap -> {
                    player.sendStatusMessage(new TranslationTextComponent("ayhacked.message.pollution_amount", (cap.getPollutionAmount()/10000f)*100f), true);
                });
                return;
            }
            player.sendStatusMessage(new TranslationTextComponent("ayhacked.message.gauge_scanning", progress), true);
        }
    }
}
