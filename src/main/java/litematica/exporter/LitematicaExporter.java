package litematica.exporter;

import net.minecraft.nbt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class LitematicaExporter {
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: java LitematicaExporter <file.litematic>");
			return;
		}
		
		String inputFile = args[0];
		try {
			processFile(inputFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void processFile(String path) throws IOException {
		System.out.println("Processing: " + path);
		
		// 读取NBT文件
		CompoundTag root;
		try (InputStream is = Files.newInputStream(Paths.get(path));
		     GZIPInputStream gzis = new GZIPInputStream(is)) {
			root = NbtIo.readCompressed(gzis, NbtAccounter.unlimitedHeap());
		}
		
		// 获取Regions
		if (root.size() != 1) {
			System.err.println("Invalid root size");
			return;
		}
		
		String rootName = root.getString("");
		System.out.println("Root: " + rootName);
		
		CompoundTag regions = root.getCompound("Regions");
		List<RegionStats> regionStatsList = processRegions(regions);
		
		// 合并所有区域
		if (regionStatsList.size() > 1) {
			RegionStats merged = mergeRegions(regionStatsList);
			merged.regionName = "[All Region]";
			regionStatsList.add(merged);
		}
		
		// 输出CSV
		String outputPath = path.replace(".litematic", ".csv");
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputPath)))) {
			// 写入BOM
			writer.write('\uFEFF');
			
			for (RegionStats region : regionStatsList) {
				writer.printf("区域(Region),[%s]%n", region.regionName);
				writer.println();
				
				// 方块转物品
				writer.println("类型(Type),[block item]");
				writer.println("名称(Name),键名(Key),数量(Count)");
				for (var entry : region.blockItems.sortedList) {
					writer.printf("%s,%s,%d = %s%n",
					              translate("item", entry.getKey().name()),
					              entry.getKey().name(),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println();
				
				// 方块实体容器
				writer.println("类型(Type),[tile entity container]");
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count)");
				for (var entry : region.tileEntityContainers.sortedList) {
					writer.printf("%s,%s,%s,%d = %s%n",
					              translate("item", entry.getKey().name),
					              entry.getKey().name,
					              NbtHelper.toPrettyPrintableString(entry.getKey().tag, 0, true),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoTEC.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writer.printf("%s,%s,%s,%d = %s,%s%n",
						              translate("item", itemEntry.getKey().name),
						              itemEntry.getKey().name,
						              NbtHelper.toPrettyPrintableString(itemEntry.getKey().tag, 0, true),
						              itemEntry.getValue().get(),
						              formatCount(itemEntry.getValue().get()),
						              parentEntry.getKey());
					}
				}
				writer.println();
				
				// 实体
				writer.println("类型(Type),[entity info]");
				writer.println("名称(Name),键名(Key),数量(Count)");
				for (var entry : region.entities.sortedList) {
					writer.printf("%s,%s,%d = %s%n",
					              translate("entity", entry.getKey().name()),
					              entry.getKey().name(),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println();
				
				// 实体容器
				writer.println("类型(Type),[entity container]");
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count)");
				for (var entry : region.entityContainers.sortedList) {
					writer.printf("%s,%s,%s,%d = %s%n",
					              translate("item", entry.getKey().name),
					              entry.getKey().name,
					              NbtHelper.toPrettyPrintableString(entry.getKey().tag, 0, true),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoEC.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writer.printf("%s,%s,%s,%d = %s,%s%n",
						              translate("item", itemEntry.getKey().name),
						              itemEntry.getKey().name,
						              NbtHelper.toPrettyPrintableString(itemEntry.getKey().tag, 0, true),
						              itemEntry.getValue().get(),
						              formatCount(itemEntry.getValue().get()),
						              parentEntry.getKey());
					}
				}
				writer.println();
				
				// 实体物品栏
				writer.println("类型(Type),[entity inventory]");
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count)");
				for (var entry : region.entityInventories.sortedList) {
					writer.printf("%s,%s,%s,%d = %s%n",
					              translate("item", entry.getKey().name),
					              entry.getKey().name,
					              NbtHelper.toPrettyPrintableString(entry.getKey().tag, 0, true),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoEI.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writer.printf("%s,%s,%s,%d = %s,%s%n",
						              translate("item", itemEntry.getKey().name),
						              itemEntry.getKey().name,
						              NbtHelper.toPrettyPrintableString(itemEntry.getKey().tag, 0, true),
						              itemEntry.getValue().get(),
						              formatCount(itemEntry.getValue().get()),
						              parentEntry.getKey());
					}
				}
				writer.println();
			}
		}
		
		System.out.println("Output: " + outputPath);
	}
	
	private static List<RegionStats> processRegions(CompoundTag regions) {
		List<RegionStats> result = new ArrayList<>();
		
		for (String regionName : regions.getAllKeys()) {
			System.out.println("Processing region: " + regionName);
			RegionStats stats = new RegionStats(regionName);
			CompoundTag region = regions.getCompound(regionName);
			
			// 方块处理
			var blockStats = BlockProcess.getBlockStats(region);
			for (var block : blockStats) {
				var items = BlockProcess.blockStatsToItemStack(block);
				for (var item : items) {
					stats.blockItems.add(new NoTagItemInfo(item.name()), item.count());
				}
			}
			
			// 方块实体容器
			var teStats = TileEntityProcess.getTileEntityContainerStats(region);
			for (var te : teStats) {
				var items = TileEntityProcess.tileEntityToItemStack(te);
				items = ItemProcess.unpackContainer(items, 128);
				for (var item : items) {
					ItemInfo info = new ItemInfo(item.name(), item.tag());
					stats.tileEntityContainers.add(info, item.count());
					if (te.name() != null) {
						stats.parentInfoTEC
							.computeIfAbsent(te.name(), k -> new MapSortList<>())
							.add(info, item.count());
					}
				}
			}
			
			// 实体处理
			var entityStats = EntityProcess.getEntityStats(region);
			entityStats = EntityProcess.unpackPassengers(entityStats, 128);
			for (var entity : entityStats) {
				stats.entities.add(new NoTagItemInfo(entity.name()), 1);
				
				var slots = EntityProcess.entityToSlot(entity);
				slots = new EntitySlotResult(
					ItemProcess.unpackContainer(slots.containers(), 128),
					ItemProcess.unpackContainer(slots.inventories(), 128)
				);
				
				for (var item : slots.containers()) {
					ItemInfo info = new ItemInfo(item.name(), item.tag());
					stats.entityContainers.add(info, item.count());
					stats.parentInfoEC
						.computeIfAbsent(entity.name(), k -> new MapSortList<>())
						.add(info, item.count());
				}
				
				for (var item : slots.inventories()) {
					ItemInfo info = new ItemInfo(item.name(), item.tag());
					stats.entityInventories.add(info, item.count());
					stats.parentInfoEI
						.computeIfAbsent(entity.name(), k -> new MapSortList<>())
						.add(info, item.count());
				}
			}
			
			stats.sortAll();
			result.add(stats);
		}
		
		return result;
	}
	
	private static RegionStats mergeRegions(List<RegionStats> regions) {
		RegionStats merged = new RegionStats("");
		for (RegionStats r : regions) {
			merged.merge(r);
		}
		merged.sortAll();
		return merged;
	}
	
	// 简化的翻译函数
	private static String translate(String type, String key) {
		// 这里可以集成JSON语言文件
		return key.replace(':', '.');
	}
	
	// 数量格式化
	private static String formatCount(long count) {
		if (count < 64) return count + "个";
		
		long items = count % 64;
		long sets = (count / 64) % 27;
		long boxes = (count / 64 / 27) % 27;
		long chests = (count / 64 / 27 / 27) % 27;
		long largeChests = (count / 64 / 27 / 27 / 27) % 27;
		
		StringBuilder sb = new StringBuilder();
		if (largeChests > 0) sb.append(largeChests).append("大箱盒");
		if (chests > 0) sb.append(chests).append("箱盒");
		if (boxes > 0) sb.append(boxes).append("盒");
		if (sets > 0) sb.append(sets).append("组");
		if (items > 0) sb.append(items).append("个");
		
		return sb.toString();
	}
}