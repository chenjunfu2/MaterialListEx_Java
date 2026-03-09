package litematica.exporter;

import net.minecraft.nbt.*;
import java.util.*;

public class ItemProcess {
	
	public static ItemStack itemCompoundToItemStack(CompoundTag item) {
		String id = item.getString("id");
		CompoundTag tag = item.contains("tag") ? item.getCompound("tag") : new CompoundTag();
		byte count = item.contains("Count") ? item.getByte("Count") : 1;
		return new ItemStack(id, tag, count & 0xFF);
	}
	
	public static List<ItemStack> unpackContainer(List<ItemStack> items, int maxDepth) {
		List<ItemStack> result = new ArrayList<>();
		for (ItemStack item : items) {
			unpackContainer(item, result, maxDepth);
		}
		return result;
	}
	
	private static void unpackContainer(ItemStack item, List<ItemStack> result, int depth) {
		if (depth <= 0 || item.tag().isEmpty()) {
			result.add(item);
			return;
		}
		
		String name = item.name();
		boolean unpacked = false;
		
		// 潜影盒
		if (name.endsWith("shulker_box") && item.tag().contains("BlockEntityTag")) {
			CompoundTag blockEntityTag = item.tag().getCompound("BlockEntityTag");
			if (blockEntityTag.contains("Items", Tag.TAG_LIST)) {
				ListTag items = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
				for (int i = 0; i < items.size(); i++) {
					ItemStack content = itemCompoundToItemStack(items.getCompound(i));
					unpackContainer(content.scale(item.count()), result, depth - 1);
				}
				unpacked = true;
			}
		}
		// 收纳袋
		else if (name.equals("minecraft:bundle") && item.tag().contains("Items")) {
			ListTag items = item.tag().getList("Items", Tag.TAG_COMPOUND);
			for (int i = 0; i < items.size(); i++) {
				ItemStack content = itemCompoundToItemStack(items.getCompound(i));
				unpackContainer(content.scale(item.count()), result, depth - 1);
			}
			unpacked = true;
		}
		
		// 不论是否解包，原物品都保留（但解包后原物品的tag可能已变空）
		if (!unpacked) {
			result.add(item);
		} else {
			// 添加解包后的原物品（tag已空）
			result.add(new ItemStack(item.name(), new CompoundTag(), item.count()));
		}
	}
}