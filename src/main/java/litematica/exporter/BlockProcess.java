package litematica.exporter;

import net.minecraft.nbt.*;
import java.util.*;

public class BlockProcess {
	
	record BlockStats(String name, CompoundTag properties, long count) {}
	
	private static final Set<String> UNITEMED_BLOCKS = Set.of(
		"minecraft:air", "minecraft:void_air", "minecraft:cave_air",
		"minecraft:nether_portal", "minecraft:end_portal", "minecraft:end_gateway",
		"minecraft:fire", "minecraft:soul_fire", "minecraft:moving_piston"
	                                                         );
	
	private static final Map<String, String> ALIAS_BLOCKS = Map.of(
		"minecraft:tripwire", "minecraft:string",
		"minecraft:redstone_wire", "minecraft:redstone",
		"minecraft:bubble_column", "minecraft:water_bucket",
		"minecraft:powder_snow", "minecraft:powder_snow_bucket"
	                                                              );
	
	public static List<BlockStats> getBlockStats(CompoundTag region) {
		// 获取区域位置
		CompoundTag pos = region.getCompound("Position");
		BlockPos regionPos = new BlockPos(pos.getInt("x"), pos.getInt("y"), pos.getInt("z"));
		
		CompoundTag size = region.getCompound("Size");
		BlockPos regionSize = new BlockPos(size.getInt("x"), size.getInt("y"), size.getInt("z"));
		
		// 计算实际大小
		BlockPos endRel = getRelativeEndFromSize(regionSize).add(regionPos);
		BlockPos min = new BlockPos(
			Math.min(regionPos.x(), endRel.x()),
			Math.min(regionPos.y(), endRel.y()),
			Math.min(regionPos.z(), endRel.z())
		);
		BlockPos max = new BlockPos(
			Math.max(regionPos.x(), endRel.x()),
			Math.max(regionPos.y(), endRel.y()),
			Math.max(regionPos.z(), endRel.z())
		);
		BlockPos realSize = max.sub(min).add(new BlockPos(1, 1, 1));
		
		// 获取调色板
		ListTag palette = region.getList("BlockStatePalette", Tag.TAG_COMPOUND);
		int bitsPerElement = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
		int mask = (1 << bitsPerElement) - 1;
		
		// 创建统计列表
		List<BlockStats> stats = new ArrayList<>();
		for (int i = 0; i < palette.size(); i++) {
			CompoundTag state = palette.getCompound(i);
			String name = state.getString("Name");
			CompoundTag properties = state.contains("Properties") ? state.getCompound("Properties") : null;
			stats.add(new BlockStats(name, properties, 0));
		}
		
		// 解析位图
		long[] blockStates = region.getLongArray("BlockStates");
		long totalBlocks = (long)realSize.x() * realSize.y() * realSize.z();
		
		if (blockStates.length * 64L / bitsPerElement < totalBlocks) {
			return stats;
		}
		
		for (long offset = 0; offset < totalBlocks * bitsPerElement; offset += bitsPerElement) {
			int startIdx = (int)(offset >> 6);
			int startBit = (int)(offset & 0x3F);
			int endIdx = (int)((offset + bitsPerElement - 1) >> 6);
			
			long paletteIdx;
			if (startIdx == endIdx) {
				paletteIdx = (blockStates[startIdx] >>> startBit) & mask;
			} else {
				int endBit = 64 - startBit;
				paletteIdx = ((blockStates[startIdx] >>> startBit) |
				              (blockStates[endIdx] << endBit)) & mask;
			}
			
			BlockStats old = stats.get((int)paletteIdx);
			stats.set((int)paletteIdx, new BlockStats(old.name(), old.properties(), old.count() + 1));
		}
		
		return stats;
	}
	
	public static List<NoTagItem> blockStatsToItemStack(BlockStats stats) {
		List<NoTagItem> items = new ArrayList<>();
		
		if (stats.name() == null) return items;
		
		// 无物品方块
		if (UNITEMED_BLOCKS.contains(stats.name())) return items;
		
		// 处理各种特殊方块
		if (processDoublePartBlocks(stats, items)) return items;
		if (processWallVariant(stats, items)) return items;
		if (processFlowerPot(stats, items)) return items;
		if (processCauldron(stats, items)) return items;
		if (processCandleCake(stats, items)) return items;
		if (processAliasBlocks(stats, items)) return items;
		if (processFluid(stats, items)) return items;
		if (processSlab(stats, items)) return items;
		if (processCluster(stats, items)) return items;
		if (processPolyAttach(stats, items)) return items;
		if (processMultiPartPlant(stats, items)) return items;
		if (processDoublePlant(stats, items)) return items;
		if (processCrop(stats, items)) return items;
		
		// 普通方块
		items.add(new NoTagItem(stats.name(), stats.count()));
		
		// 含水处理
		processWaterlogged(stats, items);
		
		return items;
	}
	
	// 门、床、活塞处理
	private static boolean processDoublePartBlocks(BlockStats stats, List<NoTagItem> items) {
		String name = stats.name();
		
		if (name.endsWith("_door") && stats.properties() != null) {
			if ("lower".equals(stats.properties().getString("half"))) {
				items.add(new NoTagItem(name, stats.count()));
			}
			return true;
		}
		
		if (name.endsWith("_bed") && stats.properties() != null) {
			if ("head".equals(stats.properties().getString("part"))) {
				items.add(new NoTagItem(name, stats.count()));
			}
			return true;
		}
		
		if (name.contains("piston") && !name.equals("minecraft:piston_head")) {
			items.add(new NoTagItem(name, stats.count()));
			return true;
		}
		
		return false;
	}
	
	// 墙上方块
	private static boolean processWallVariant(BlockStats stats, List<NoTagItem> items) {
		String name = stats.name();
		if (name.contains("wall_")) {
			String itemName = name.replace("wall_", "");
			items.add(new NoTagItem(itemName, stats.count()));
			return true;
		}
		return false;
	}
	
	// 花盆
	private static boolean processFlowerPot(BlockStats stats, List<NoTagItem> items) {
		String name = stats.name();
		if (name.contains("potted_")) {
			items.add(new NoTagItem("minecraft:flower_pot", stats.count()));
			String plantName = name.replace("potted_", "");
			if (plantName.endsWith("_bush")) {
				plantName = plantName.substring(0, plantName.length() - 5);
			}
			items.add(new NoTagItem(plantName, stats.count()));
			return true;
		}
		return false;
	}
	
	// 炼药锅
	private static boolean processCauldron(BlockStats stats, List<NoTagItem> items) {
		String name = stats.name();
		items.add(new NoTagItem("minecraft:cauldron", stats.count()));
		
		switch (name) {
		case "minecraft:water_cauldron":
			items.add(new NoTagItem("minecraft:water_bucket", stats.count()));
			if (stats.properties() != null) {
				String level = stats.properties().getString("level");
				if ("1".equals(level)) {
					items.add(new NoTagItem("minecraft:glass_bottle", stats.count() * 2));
				} else if ("2".equals(level)) {
					items.add(new NoTagItem("minecraft:glass_bottle", stats.count()));
				}
			}
			return true;
		
		case "minecraft:lava_cauldron":
			items.add(new NoTagItem("minecraft:lava_bucket", stats.count()));
			return true;
		
		case "minecraft:powder_snow_cauldron":
			if (stats.properties() != null && "3".equals(stats.properties().getString("level"))) {
				items.add(new NoTagItem("minecraft:powder_snow_bucket", stats.count()));
			}
			return true;
		
		default:
			return false;
		}
	}
	
	// 蜡烛蛋糕
	private static boolean processCandleCake(BlockStats stats, List<NoTagItem> items) {
		String name = stats.name();
		if (name.endsWith("_cake")) {
			items.add(new NoTagItem("minecraft:cake", stats.count()));
			String candleName = name.substring(0, name.length() - 5);
			items.add(new NoTagItem(candleName, stats.count()));
			return true;
		}
		return false;
	}
	
	// 别名方块
	private static boolean processAliasBlocks(BlockStats stats, List<NoTagItem> items) {
		String alias = ALIAS_BLOCKS.get(stats.name());
		if (alias != null) {
			items.add(new NoTagItem(alias, stats.count()));
			return true;
		}
		return false;
	}
	
	// 流体
	private static boolean processFluid(BlockStats stats, List<NoTagItem> items) {
		if ("minecraft:water".equals(stats.name()) && stats.properties() != null) {
			if ("0".equals(stats.properties().getString("level"))) {
				items.add(new NoTagItem("minecraft:water_bucket", stats.count()));
			}
			return true;
		}
		if ("minecraft:lava".equals(stats.name()) && stats.properties() != null) {
			if ("0".equals(stats.properties().getString("level"))) {
				items.add(new NoTagItem("minecraft:lava_bucket", stats.count()));
			}
			return true;
		}
		return false;
	}
	
	// 半砖
	private static boolean processSlab(BlockStats stats, List<NoTagItem> items) {
		String name = stats.name();
		if (name.endsWith("_slab") && stats.properties() != null) {
			if ("double".equals(stats.properties().getString("type"))) {
				items.add(new NoTagItem(name, stats.count() * 2));
			} else {
				items.add(new NoTagItem(name, stats.count()));
			}
			return true;
		}
		return false;
	}
	
	// 复合方块（雪、海龟蛋等）
	private static boolean processCluster(BlockStats stats, List<NoTagItem> items) {
		if (stats.properties() == null) return false;
		
		String name = stats.name();
		if (name.endsWith("candle")) {
			int count = Integer.parseInt(stats.properties().getString("candles"));
			items.add(new NoTagItem(name, stats.count() * count));
			return true;
		}
		
		Map<String, String> clusterMap = Map.of(
			"minecraft:snow", "layers",
			"minecraft:turtle_egg", "eggs",
			"minecraft:sea_pickle", "pickles",
			"minecraft:pink_petals", "flower_amount"
		                                       );
		
		String prop = clusterMap.get(name);
		if (prop != null && stats.properties().contains(prop)) {
			int count = Integer.parseInt(stats.properties().getString(prop));
			items.add(new NoTagItem(name, stats.count() * count));
			return true;
		}
		
		return false;
	}
	
	// 多面附着方块
	private static boolean processPolyAttach(BlockStats stats, List<NoTagItem> items) {
		Set<String> polyBlocks = Set.of(
			"minecraft:vine", "minecraft:glow_lichen", "minecraft:sculk_vein"
		                               );
		
		if (polyBlocks.contains(stats.name()) && stats.properties() != null) {
			String[] faces = {"down", "east", "north", "south", "up", "west"};
			long faceCount = Arrays.stream(faces)
			                       .filter(f -> "true".equals(stats.properties().getString(f)))
			                       .count();
			items.add(new NoTagItem(stats.name(), stats.count() * faceCount));
			return true;
		}
		return false;
	}
	
	// 多格植株
	private static boolean processMultiPartPlant(BlockStats stats, List<NoTagItem> items) {
		Map<String, String> plantMap = Map.of(
			"minecraft:cave_vines_plant", "minecraft:glow_berries",
			"minecraft:cave_vines", "minecraft:glow_berries",
			"minecraft:bamboo_sapling", "minecraft:bamboo",
			"minecraft:big_dripleaf_stem", "minecraft:big_dripleaf",
			"minecraft:kelp", "minecraft:kelp",
			"minecraft:kelp_plant", "minecraft:kelp",
			"minecraft:tall_seagrass", "minecraft:seagrass",
			"minecraft:weeping_vines_plant", "minecraft:weeping_vines",
			"minecraft:twisting_vines_plant", "minecraft:twisting_vines"
		                                     );
		
		String mapped = plantMap.get(stats.name());
		if (mapped != null) {
			items.add(new NoTagItem(mapped, stats.count()));
			if (mapped.equals("minecraft:kelp") || mapped.equals("minecraft:seagrass")) {
				items.add(new NoTagItem("minecraft:water_bucket", stats.count()));
			}
			return true;
		}
		return false;
	}
	
	// 双格植株
	private static boolean processDoublePlant(BlockStats stats, List<NoTagItem> items) {
		Set<String> doublePlants = Set.of(
			"minecraft:small_dripleaf", "minecraft:tall_grass", "minecraft:large_fern",
			"minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush",
			"minecraft:peony", "minecraft:pitcher_plant"
		                                 );
		
		if (doublePlants.contains(stats.name()) && stats.properties() != null) {
			if ("lower".equals(stats.properties().getString("half"))) {
				items.add(new NoTagItem(stats.name(), stats.count()));
			}
			return true;
		}
		return false;
	}
	
	// 作物
	private static boolean processCrop(BlockStats stats, List<NoTagItem> items) {
		Map<String, String> cropMap = Map.of(
			"minecraft:pumpkin_stem", "minecraft:pumpkin_seeds",
			"minecraft:attached_pumpkin_stem", "minecraft:pumpkin_seeds",
			"minecraft:melon_stem", "minecraft:melon_seeds",
			"minecraft:attached_melon_stem", "minecraft:melon_seeds",
			"minecraft:beetroots", "minecraft:beetroot_seeds",
			"minecraft:wheat", "minecraft:wheat_seeds",
			"minecraft:carrots", "minecraft:carrot",
			"minecraft:potatoes", "minecraft:potato",
			"minecraft:torchflower_crop", "minecraft:torchflower_seeds",
			"minecraft:pitcher_crop", "minecraft:pitcher_pod"
		                                    );
		
		String mapped = cropMap.get(stats.name());
		if (mapped != null) {
			if (mapped.equals("minecraft:pitcher_pod") && stats.properties() != null) {
				if ("lower".equals(stats.properties().getString("half"))) {
					items.add(new NoTagItem(mapped, stats.count()));
				}
			} else {
				items.add(new NoTagItem(mapped, stats.count()));
			}
			return true;
		}
		return false;
	}
	
	// 含水处理
	private static void processWaterlogged(BlockStats stats, List<NoTagItem> items) {
		if (stats.properties() != null && "true".equals(stats.properties().getString("waterlogged"))) {
			items.add(new NoTagItem("minecraft:water_bucket", stats.count()));
		}
	}
	
	private static BlockPos getRelativeEndFromSize(BlockPos size) {
		int x = size.x() >= 0 ? size.x() - 1 : size.x() + 1;
		int y = size.y() >= 0 ? size.y() - 1 : size.y() + 1;
		int z = size.z() >= 0 ? size.z() - 1 : size.z() + 1;
		return new BlockPos(x, y, z);
	}
	
	record NoTagItem(String name, long count) {}
}