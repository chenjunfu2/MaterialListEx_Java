package litematica.exporter;

import net.minecraft.nbt.*;
import java.util.*;

public class EntityProcess {
	
	record EntityStats(String name, List<EntityItemSlot> slots, ListTag passengers) {}
	
	record EntityItemSlot(int slotType, Tag items) {
		static final int CONTAINER_START = 5;
	}
	
	record EntitySlotResult(List<ItemStack> containers, List<ItemStack> inventories) {}
	
	private static final String[] SLOT_TAGS = {
		"ArmorItems", "HandItems", "Inventory", "SaddleItem", "DecorItem",
		"Items", "Item"
	};
	
	public static List<EntityStats> getEntityStats(CompoundTag region) {
		List<EntityStats> result = new ArrayList<>();
		
		if (!region.contains("Entities", Tag.TAG_LIST)) {
			return result;
		}
		
		ListTag entities = region.getList("Entities", Tag.TAG_COMPOUND);
		for (int i = 0; i < entities.size(); i++) {
			result.add(entityToStats(entities.getCompound(i)));
		}
		
		return result;
	}
	
	private static EntityStats entityToStats(CompoundTag entity) {
		String id = entity.getString("id");
		List<EntityItemSlot> slots = new ArrayList<>();
		
		// 处理特殊物品（鞍、拴绳）
		if (entity.contains("Saddle", Tag.TAG_BYTE) && entity.getByte("Saddle") != 0) {
			CompoundTag saddleItem = new CompoundTag();
			saddleItem.putString("id", "minecraft:saddle");
			saddleItem.putByte("Count", (byte)1);
			slots.add(new EntityItemSlot(3, saddleItem));
		}
		
		if (entity.contains("Leash", Tag.TAG_COMPOUND) && !entity.getCompound("Leash").isEmpty()) {
			CompoundTag leadItem = new CompoundTag();
			leadItem.putString("id", "minecraft:lead");
			leadItem.putByte("Count", (byte)1);
			slots.add(new EntityItemSlot(2, leadItem));
		}
		
		// 遍历所有可能的物品槽位
		for (int i = 0; i < SLOT_TAGS.length; i++) {
			String tagName = SLOT_TAGS[i];
			if (entity.contains(tagName)) {
				slots.add(new EntityItemSlot(i, entity.get(tagName)));
			}
		}
		
		ListTag passengers = entity.contains("Passengers") ?
		                     entity.getList("Passengers", Tag.TAG_COMPOUND) : null;
		
		return new EntityStats(id, slots, passengers);
	}
	
	public static List<EntityStats> unpackPassengers(List<EntityStats> entities, int maxDepth) {
		List<EntityStats> result = new ArrayList<>();
		for (EntityStats e : entities) {
			unpackPassengers(e, result, maxDepth);
		}
		return result;
	}
	
	private static void unpackPassengers(EntityStats entity, List<EntityStats> result, int depth) {
		if (depth <= 0) return;
		
		if (entity.passengers() != null) {
			for (int i = 0; i < entity.passengers().size(); i++) {
				CompoundTag passenger = entity.passengers().getCompound(i);
				unpackPassengers(entityToStats(passenger), result, depth - 1);
			}
		}
		
		result.add(entity);
	}
	
	public static EntitySlotResult entityToSlot(EntityStats entity) {
		if (entity.name().equals("minecraft:item_display")) {
			return new EntitySlotResult(new ArrayList<>(), new ArrayList<>());
		}
		
		List<ItemStack> containers = new ArrayList<>();
		List<ItemStack> inventories = new ArrayList<>();
		
		for (EntityItemSlot slot : entity.slots()) {
			List<ItemStack> target = slot.slotType() < EntityItemSlot.CONTAINER_START ?
			                         inventories : containers;
			
			if (slot.items() instanceof CompoundTag single) {
				if (!single.isEmpty()) {
					target.add(ItemProcess.itemCompoundToItemStack(single));
				}
			} else if (slot.items() instanceof ListTag list) {
				for (int i = 0; i < list.size(); i++) {
					CompoundTag item = list.getCompound(i);
					if (!item.isEmpty()) {
						target.add(ItemProcess.itemCompoundToItemStack(item));
					}
				}
			}
		}
		
		return new EntitySlotResult(containers, inventories);
	}
}