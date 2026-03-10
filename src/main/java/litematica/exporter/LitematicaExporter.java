package litematica.exporter;

import net.minecraft.nbt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

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
				
				if (jarPath.startsWith("/") && System.getProperty("os.name").toLowerCase().contains("windows")) {
					jarPath = jarPath.substring(1); // 去掉开头的 /
				}
				
				langPath = Paths.get(jarPath).resolve(langFile);
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
					writeCSVLine(writer,
					             translate(Language.KeyType.ITEM, entry.getKey().name()),
					             entry.getKey().name(),
					             entry.getValue().get() + " = " + formatCount(entry.getValue().get())
					            );
				}
				writer.println();
				
				// 方块实体容器
				writer.println("类型(Type),[tile entity container]");
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count)");
				for (var entry : region.tileEntityContainers.sortedList) {
					writeCSVLine(writer,
					             translate(Language.KeyType.ITEM, entry.getKey().name),
					             entry.getKey().name,
					             NbtHelper.serialize(entry.getKey().tag),
					             entry.getValue().get() + " = " + formatCount(entry.getValue().get())
					            );
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoTEC.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writeCSVLine(writer,
						             translate(Language.KeyType.ITEM, itemEntry.getKey().name),
						             itemEntry.getKey().name,
						             NbtHelper.serialize(itemEntry.getKey().tag),
						             itemEntry.getValue().get() + " = " + formatCount(itemEntry.getValue().get()),
						             parentEntry.getKey()
						            );
					}
				}
				writer.println();
				
				// 实体
				writer.println("类型(Type),[entity info]");
				writer.println("名称(Name),键名(Key),数量(Count)");
				for (var entry : region.entities.sortedList) {
					writeCSVLine(writer,
					             translate(Language.KeyType.ENTITY, entry.getKey().name()),
					             entry.getKey().name(),
					             entry.getValue().get() + " = " + formatCount(entry.getValue().get())
					            );
				}
				writer.println();
				
				// 实体容器
				writer.println("类型(Type),[entity container]");
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count)");
				for (var entry : region.entityContainers.sortedList) {
					writeCSVLine(writer,
					             translate(Language.KeyType.ITEM, entry.getKey().name),
					             entry.getKey().name,
					             NbtHelper.serialize(entry.getKey().tag),
					             entry.getValue().get() + " = " + formatCount(entry.getValue().get())
					            );
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoEC.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writeCSVLine(writer,
						             translate(Language.KeyType.ITEM, itemEntry.getKey().name),
						             itemEntry.getKey().name,
						             NbtHelper.serialize(itemEntry.getKey().tag),
						             itemEntry.getValue().get() + " = " + formatCount(itemEntry.getValue().get()),
						             parentEntry.getKey()
						            );
					}
				}
				writer.println();
				
				// 实体物品栏
				writer.println("类型(Type),[entity inventory]");
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count)");
				for (var entry : region.entityInventories.sortedList) {
					writeCSVLine(writer,
					             translate(Language.KeyType.ITEM, entry.getKey().name),
					             entry.getKey().name,
					             NbtHelper.serialize(entry.getKey().tag),
					             entry.getValue().get() + " = " + formatCount(entry.getValue().get())
					            );
				}
				writer.println("名称(Name),键名(Key),标签(Tag),数量(Count),来源(source)");
				for (var parentEntry : region.parentInfoEI.entrySet()) {
					for (var itemEntry : parentEntry.getValue().sortedList) {
						writeCSVLine(writer,
						             translate(Language.KeyType.ITEM, itemEntry.getKey().name),
						             itemEntry.getKey().name,
						             NbtHelper.serialize(itemEntry.getKey().tag),
						             itemEntry.getValue().get() + " = " + formatCount(itemEntry.getValue().get()),
						             parentEntry.getKey()
						            );
					}
				}
				writer.println();
			}
		}
		
		System.out.println("Output: " + outputPath);
	}
	
	/**
	 * CSV字段转义：将字段中的双引号替换为两个双引号，并用双引号包围整个字段
	 */
	private static String escapeCSV(String field) {
		if (field == null) return "";
		
		// 如果字段包含逗号、换行符或双引号，则需要转义
		boolean needQuote = field.indexOf(',') >= 0 ||
		                    field.indexOf('\n') >= 0 ||
		                    field.indexOf('"') >= 0;
		
		if (!needQuote) {
			return field;
		}
		
		// 将双引号替换为两个双引号
		String escaped = field.replace("\"", "\"\"");
		// 用双引号包围整个字段
		return "\"" + escaped + "\"";
	}
	
	/**
	 * 写入一行CSV数据，自动处理字段转义
	 */
	private static void writeCSVLine(PrintWriter writer, String... fields) {
		if (fields.length == 0) {
			writer.println();
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fields.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(escapeCSV(fields[i]));
		}
		writer.println(sb.toString());
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
				
				boolean b = entity.name() != null && ! entity.name().isEmpty();
				for (var item : slots.containers()) {
					ItemInfo info = new ItemInfo(item.name(), item.tag());
					stats.entityContainers.add(info, item.count());
					if (b) {
						stats.parentInfoEC
							.computeIfAbsent(entity.name(), k -> new MapSortList<>())
							.add(info, item.count());
					}
				}
				
				for (var item : slots.inventories()) {
					ItemInfo info = new ItemInfo(item.name(), item.tag());
					stats.entityInventories.add(info, item.count());
					if (b) {
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