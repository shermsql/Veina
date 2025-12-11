package org.shermsql.veina;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class Veina implements ModInitializer {
    private static final int MAX_ORE_CHAIN = 200;

    private static final Block[] ORES_LIST = new Block[] {
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.ANCIENT_DEBRIS
    };

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return true;

            if (!player.isSneaking()) return true;

            ItemStack tool = player.getMainHandStack();
            Item item = tool.getItem();
            if (tool.isEmpty() || !(item instanceof Item)) return true;

            if (!isOre(state)) return true;

            ServerWorld serverWorld = (ServerWorld) world;
            List<BlockPos> vein = findConnectedOres(serverWorld, pos);

            if (vein.size() <= 1) return true;

            tool.damage(vein.size(), player, EquipmentSlot.MAINHAND);

            for (BlockPos orePos : vein) {
                serverWorld.breakBlock(orePos, true, player);
            }

            float volume = Math.min(2.0F, 0.5F + vein.size() / 10.0F);
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, volume, 0.8F);

            return false;
        });
    }

    private boolean isOre(BlockState state) {
        for (Block block : ORES_LIST) {
            if (state.getBlock() == block) return true;
        }
        return false;
    }

    private List<BlockPos> findConnectedOres(ServerWorld world, BlockPos start) {
        List<BlockPos> ores = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start.toImmutable());

        while (!queue.isEmpty() && ores.size() < MAX_ORE_CHAIN) {
            BlockPos current = queue.poll();
            ores.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (visited.contains(neighbor)) continue;

                BlockState neighborState = world.getBlockState(neighbor);
                if (isOre(neighborState)) {
                    queue.add(neighbor);
                    visited.add(neighbor.toImmutable());
                }
            }
        }
        return ores;
    }
}
