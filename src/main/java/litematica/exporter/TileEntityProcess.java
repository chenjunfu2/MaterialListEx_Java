package litematica.exporter;

import net.minecraft.nbt.*;
import java.util.*;

public class TileEntityProcess {
	
	record TileEntityContainerStats(String name, Tag items) {}
	
	private static final Map<String, String> CONTAINER_TAG_MAP = Map.of(
		"minecraft:jukebox", "RecordItem",
		"minecraft:lectern", "Book",
		"minecraft:brushable_block", "item"
	                                                                   );
	
	public static List<TileEntityContainerStats> getTileEntityContainerStats(CompoundTag region) {
		List<TileEntityContainerStats> result = new ArrayList<>();
		
		if (!region.contains("TileEntities", Tag.TAG_LIST)) {
			return result;
		}
		
		ListTag tileEntities = region.getList("TileEntities", Tag.TAG_COMPOUND);
		for (int i = 0; i < tileEntities.size(); i++) {
			CompoundTag te = tileEntities.getCompound(i);
			String id = te.getString("id");
			
			// 先找普通容器
			if (te.contains("Items", Tag.TAG_LIST)) {
				result.add(new TileEntityContainerStats(id, te.getList("Items", Tag.TAG_COMPOUND)));
				continue;
			}
			
			// 特殊容器
			String tagName = CONTAINER_TAG_MAP.get(id);
			if (tagName != null && te.contains(tagName)) {
				result.add(new TileEntityContainerStats(id, te.get(tagName)));
			}
		}
		
		return result;
	}
	
	public static List<ItemStack> tileEntityToItemStack(TileEntityContainerStats stats) {
		List<ItemStack> items = new ArrayList<>();
		
		if (stats.items() instanceof CompoundTag single) {
			if (!single.isEmpty()) {
				items.add(ItemProcess.itemCompoundToItemStack(single));
			}
		} else if (stats.items() instanceof ListTag list) {
			for (int i = 0; i < list.size(); i++) {
				CompoundTag item = list.getCompound(i);
				if (!item.isEmpty()) {
					items.add(ItemProcess.itemCompoundToItemStack(item));
				}
			}
		}
		
		return items;
	}
}