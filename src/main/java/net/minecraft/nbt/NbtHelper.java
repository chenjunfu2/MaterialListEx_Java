package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NBT序列化工具类 - 简化版
 * sortCompound固定为true
 */
public class NbtHelper {
	
	/**
	 * 将NBT对象序列化为字符串
	 * @param tag NBT标签
	 * @return 序列化后的字符串
	 */
	public static String serialize(Tag tag) {
		StringBuilder sb = new StringBuilder();
		serializeSwitch(tag, sb);
		return sb.toString();
	}
	
	/**
	 * 递归序列化NBT对象
	 */
	private static void serializeSwitch(Tag tag, StringBuilder sb) {
		byte type = tag.getId();
		
		switch (type) {
		case Tag.TAG_END:
			sb.append("[End]");
			break;
		
		case Tag.TAG_BYTE:
			toHexString(((ByteTag) tag).getAsByte(), sb);
			sb.append('B');
			break;
		
		case Tag.TAG_SHORT:
			toHexString(((ShortTag) tag).getAsShort(), sb);
			sb.append('S');
			break;
		
		case Tag.TAG_INT:
			toHexString(((IntTag) tag).getAsInt(), sb);
			sb.append('I');
			break;
		
		case Tag.TAG_LONG:
			toHexString(((LongTag) tag).getAsLong(), sb);
			sb.append('L');
			break;
		
		case Tag.TAG_FLOAT:
			toHexString(Float.floatToIntBits(((FloatTag) tag).getAsFloat()), sb);
			sb.append('F');
			break;
		
		case Tag.TAG_DOUBLE:
			toHexString(Double.doubleToLongBits(((DoubleTag) tag).getAsDouble()), sb);
			sb.append('D');
			break;
		
		case Tag.TAG_BYTE_ARRAY:
			byte[] byteArray = ((ByteArrayTag) tag).getValue();
			sb.append("[B;");
			for (int i = 0; i < byteArray.length; i++) {
				if (i > 0) sb.append(',');
				toHexString(byteArray[i], sb);
			}
			sb.append(']');
			break;
		
		case Tag.TAG_INT_ARRAY:
			int[] intArray = ((IntArrayTag) tag).getValue();
			sb.append("[I;");
			for (int i = 0; i < intArray.length; i++) {
				if (i > 0) sb.append(',');
				toHexString(intArray[i], sb);
			}
			sb.append(']');
			break;
		
		case Tag.TAG_LONG_ARRAY:
			long[] longArray = ((LongArrayTag) tag).getValue();
			sb.append("[L;");
			for (int i = 0; i < longArray.length; i++) {
				if (i > 0) sb.append(',');
				toHexString(longArray[i], sb);
			}
			sb.append(']');
			break;
		
		case Tag.TAG_STRING:
			sb.append('"');
			sb.append(((StringTag) tag).getAsString());
			sb.append('"');
			break;
		
		case Tag.TAG_LIST:
			ListTag list = (ListTag) tag;
			sb.append('[');
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) sb.append(',');
				serializeSwitch(list.get(i), sb);
			}
			sb.append(']');
			break;
		
		case Tag.TAG_COMPOUND:
			CompoundTag compound = (CompoundTag) tag;
			sb.append('{');
			
			// 获取所有键并排序
			List<String> keys = new ArrayList<>(compound.getAllKeys());
			Collections.sort(keys);
			
			for (int i = 0; i < keys.size(); i++) {
				String key = keys.get(i);
				if (i > 0) sb.append(',');
				sb.append('"');
				sb.append(key);
				sb.append("\":");
				serializeSwitch(compound.get(key), sb);
			}
			
			sb.append('}');
			break;
		
		default:
			sb.append("[Unknown NBT Tag Type [");
			toHexString(type & 0xFF, sb);
			sb.append("]]");
			break;
		}
	}
	
	private static void toHexString(byte value, StringBuilder sb) {
		sb.append(String.format("0x%02X", value & 0xFF));
	}
	
	private static void toHexString(short value, StringBuilder sb) {
		sb.append(String.format("0x%04X", value & 0xFFFF));
	}
	
	private static void toHexString(int value, StringBuilder sb) {
		sb.append(String.format("0x%08X", value));
	}
	
	private static void toHexString(long value, StringBuilder sb) {
		sb.append(String.format("0x%016X", value));
	}
}