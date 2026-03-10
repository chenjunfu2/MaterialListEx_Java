package litematica.exporter;

import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class LitematicaExporter {
	
	private static Language language;
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: java LitematicaExporter <file.litematic>");
			return;
		}
		
		// 加载语言文件
		try {
			String langFile = "zh_cn.json";
			Path langPath = Paths.get(langFile);
			if (!Files.exists(langPath)) {
				// 尝试从程序所在目录查找
				String jarPath = LitematicaExporter.class.getProtectionDomain()
				                                         .getCodeSource().getLocation().getPath();
				Path jarDir = Paths.get(jarPath).getParent();
				if (jarDir != null) {
					langPath = jarDir.resolve(langFile);
				}
			}
			
			language = Language.load(langPath.toString());
			System.out.println("Language file loaded: " + langPath);
		} catch (IOException e) {
			System.err.println("Failed to load language file: " + e.getMessage());
			language = new Language();
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
		try
		{
			root = NbtIo.readCompressed(Files.newInputStream(Paths.get(path)));
		}
		catch (IOException e)
		{
			System.err.println("Read Exception: " + e);
			return;
		}
		
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
		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			Files.newOutputStream(Paths.get(outputPath)), StandardCharsets.UTF_8))) {
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
					              translate(Language.KeyType.ITEM, entry.getKey().name()),
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
					              translate(Language.KeyType.ITEM, entry.getKey().name),
					              entry.getKey().name,
					              serializeTag(entry.getKey().tag),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoTEC.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writer.printf("%s,%s,%s,%d = %s,%s%n",
						              translate(Language.KeyType.ITEM, itemEntry.getKey().name),
						              itemEntry.getKey().name,
						              serializeTag(itemEntry.getKey().tag),
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
					              translate(Language.KeyType.ENTITY, entry.getKey().name()),
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
					              translate(Language.KeyType.ITEM, entry.getKey().name),
					              entry.getKey().name,
					              serializeTag(entry.getKey().tag),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoEC.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writer.printf("%s,%s,%s,%d = %s,%s%n",
						              translate(Language.KeyType.ITEM, itemEntry.getKey().name),
						              itemEntry.getKey().name,
						              serializeTag(itemEntry.getKey().tag),
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
					              translate(Language.KeyType.ITEM, entry.getKey().name),
					              entry.getKey().name,
					              serializeTag(entry.getKey().tag),
					              entry.getValue().get(),
					              formatCount(entry.getValue().get()));
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoEI.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writer.printf("%s,%s,%s,%d = %s,%s%n",
						              translate(Language.KeyType.ITEM, itemEntry.getKey().name),
						              itemEntry.getKey().name,
						              serializeTag(itemEntry.getKey().tag),
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
					if (te.name() != null && !te.name().isEmpty()) {
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
				slots = new EntityProcess.EntitySlotResult(
					ItemProcess.unpackContainer(slots.containers(), 128),
					ItemProcess.unpackContainer(slots.inventories(), 128)
				);
				
				for (var item : slots.containers()) {
					ItemInfo info = new ItemInfo(item.name(), item.tag());
					stats.entityContainers.add(info, item.count());
					if (entity.name() != null && !entity.name().isEmpty()) {
						stats.parentInfoEC
							.computeIfAbsent(entity.name(), k -> new MapSortList<>())
							.add(info, item.count());
					}
				}
				
				for (var item : slots.inventories()) {
					ItemInfo info = new ItemInfo(item.name(), item.tag());
					stats.entityInventories.add(info, item.count());
					if (entity.name() != null && !entity.name().isEmpty()) {
						stats.parentInfoEI
							.computeIfAbsent(entity.name(), k -> new MapSortList<>())
							.add(info, item.count());
					}
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
	
	// 翻译函数
	private static String translate(Language.KeyType type, String key) {
		if (language != null) {
			return language.translate(type, key);
		}
		return key.replace(':', '.');
	}
	
	// 序列化NBT标签
	private static String serializeTag(CompoundTag tag) {
		if (tag == null || tag.isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		
		// 排序键名以获得一致输出
		List<String> keys = new ArrayList<>(tag.getAllKeys());
		Collections.sort(keys);
		
		for (String key : keys) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			
			sb.append("\"").append(key).append("\":");
			Tag value = tag.get(key);
			
			if (value instanceof CompoundTag) {
				sb.append(serializeTag((CompoundTag) value));
			} else if (value instanceof ListTag) {
				sb.append(serializeList((ListTag) value));
			} else if (value instanceof StringTag) {
				sb.append("\"").append(value.getAsString()).append("\"");
			} else if (value instanceof NumericTag) {
				sb.append(value);
			} else {
				sb.append(value.toString());
			}
		}
		
		sb.append("}");
		return sb.toString();
	}
	
	private static String serializeList(ListTag list) {
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		
		for (int i = 0; i < list.size(); i++) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			
			Tag value = list.get(i);
			if (value instanceof CompoundTag) {
				sb.append(serializeTag((CompoundTag) value));
			} else if (value instanceof ListTag) {
				sb.append(serializeList((ListTag) value));
			} else if (value instanceof StringTag) {
				sb.append("\"").append(value.getAsString()).append("\"");
			} else if (value instanceof NumericTag) {
				sb.append(value);
			} else {
				sb.append(value.toString());
			}
		}
		
		sb.append("]");
		return sb.toString();
	}
	
	// 数量格式化
	private static String formatCount(long count) {
		if (count == 0) return "0个";
		
		final long SET_SIZE = 64;
		final long BOX_SIZE = 27;
		
		long items = count % SET_SIZE;
		long sets = (count / SET_SIZE) % BOX_SIZE;
		long boxes = (count / SET_SIZE / BOX_SIZE) % BOX_SIZE;
		long chests = (count / SET_SIZE / BOX_SIZE / BOX_SIZE) % BOX_SIZE;
		long largeChests = (count / SET_SIZE / BOX_SIZE / BOX_SIZE / BOX_SIZE) % BOX_SIZE;
		
		StringBuilder sb = new StringBuilder();
		if (largeChests > 0) sb.append(largeChests).append("大箱盒");
		if (chests > 0) sb.append(chests).append("箱盒");
		if (boxes > 0) sb.append(boxes).append("盒");
		if (sets > 0) sb.append(sets).append("组");
		if (items > 0) sb.append(items).append("个");
		
		return sb.toString();
	}
}