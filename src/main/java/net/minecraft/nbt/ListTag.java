package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListTag implements Tag {
    private final List<Tag> list = new ArrayList<>();
    
    public ListTag() {
    }
    
    private ListTag(List<Tag> list) {
        this.list.addAll(list);
    }
    
    /**
     * 识别列表中元素的实际类型
     * 如果所有元素类型相同，返回该类型ID
     * 如果类型不一致，返回TAG_COMPOUND(10)作为包装类型
     */
    private byte identifyElementType() {
        byte type = Tag.TAG_END;
        for (Tag tag : list) {
            byte currentId = tag.getId();
            if (type == Tag.TAG_END) {
                type = currentId;
            } else if (type != currentId) {
                return Tag.TAG_COMPOUND; // 类型不一致时返回复合类型
            }
        }
        return type;
    }
    
    /**
     * 如果需要，将元素包装为CompoundTag
     * 用于处理类型不一致的情况
     */
    private Tag wrapIfNeeded(byte elementType, Tag tag) {
        if (elementType != Tag.TAG_COMPOUND) {
            return tag;
        }
        // 如果元素已经是CompoundTag，直接返回
        if (tag instanceof CompoundTag) {
            return tag;
        }
        // 否则包装成CompoundTag
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("", tag);
        return wrapper;
    }
    
    @Override
    public void write(DataOutput output) throws IOException {
        byte elementType = identifyElementType();
        output.writeByte(elementType);
        output.writeInt(list.size());
        
        for (Tag tag : list) {
            wrapIfNeeded(elementType, tag).write(output);
        }
    }
    
    @Override
    public byte getId() {
        return Tag.TAG_LIST;
    }
    
    @Override
    public ListTag copy() {
        ListTag copy = new ListTag();
        for (Tag tag : list) {
            copy.add(tag.copy());
        }
        return copy;
    }
    
    // ====== 列表操作方法 ======
    
    public void add(Tag tag) {
        list.add(tag);
    }
    
    public void add(int index, Tag tag) {
        list.add(index, tag);
    }
    
    public Tag set(int index, Tag tag) {
        return list.set(index, tag);
    }
    
    public Tag get(int index) {
        return list.get(index);
    }
    
    public Tag remove(int index) {
        return list.remove(index);
    }
    
    public int size() {
        return list.size();
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public void clear() {
        list.clear();
    }
    
    /**
     * 获取列表中元素的类型
     * 如果列表为空，返回TAG_END
     */
    public byte getElementType() {
        return list.isEmpty() ? Tag.TAG_END : identifyElementType();
    }
    
    // ====== 类型安全的获取方法 ======
    
    public CompoundTag getCompound(int index) {
        Tag tag = get(index);
        if (tag instanceof CompoundTag) {
            return (CompoundTag) tag;
        }
        return new CompoundTag();
    }
    
    public String getString(int index) {
        Tag tag = get(index);
        if (tag instanceof StringTag) {
            return ((StringTag) tag).getValue();
        }
        return "";
    }
    
    public int getInt(int index) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsInt();
        }
        return 0;
    }
    
    public long getLong(int index) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsLong();
        }
        return 0L;
    }
    
    public float getFloat(int index) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsFloat();
        }
        return 0.0f;
    }
    
    public double getDouble(int index) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsDouble();
        }
        return 0.0;
    }
    
    public short getShort(int index) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsShort();
        }
        return 0;
    }
    
    public byte getByte(int index) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsByte();
        }
        return 0;
    }
    
    public boolean getBoolean(int index) {
        return getByte(index) != 0;
    }
    
    public int[] getIntArray(int index) {
        Tag tag = get(index);
        if (tag instanceof IntArrayTag) {
            return ((IntArrayTag) tag).getValue();
        }
        return new int[0];
    }
    
    public long[] getLongArray(int index) {
        Tag tag = get(index);
        if (tag instanceof LongArrayTag) {
            return ((LongArrayTag) tag).getValue();
        }
        return new long[0];
    }
    
    public byte[] getByteArray(int index) {
        Tag tag = get(index);
        if (tag instanceof ByteArrayTag) {
            return ((ByteArrayTag) tag).getValue();
        }
        return new byte[0];
    }
    
    // ====== 带默认值的获取方法 ======
    
    public int getIntOr(int index, int defaultValue) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsInt();
        }
        return defaultValue;
    }
    
    public String getStringOr(int index, String defaultValue) {
        Tag tag = get(index);
        if (tag instanceof StringTag) {
            return ((StringTag) tag).getValue();
        }
        return defaultValue;
    }
    
    public long getLongOr(int index, long defaultValue) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsLong();
        }
        return defaultValue;
    }
    
    public boolean getBooleanOr(int index, boolean defaultValue) {
        if (index >= 0 && index < list.size()) {
            return getBoolean(index);
        }
        return defaultValue;
    }
    
    public byte getByteOr(int index, byte defaultValue) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsByte();
        }
        return defaultValue;
    }
    
    public float getFloatOr(int index, float defaultValue) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsFloat();
        }
        return defaultValue;
    }
    
    public double getDoubleOr(int index, double defaultValue) {
        Tag tag = get(index);
        if (tag instanceof NumericTag) {
            return ((NumericTag) tag).getAsDouble();
        }
        return defaultValue;
    }
    
    // ====== 批量操作方法 ======
    
    public void addAll(List<CompoundTag> tags) {
        for (CompoundTag tag : tags) {
            add(tag);
        }
    }
    
    // ====== 标准方法重写 ======
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ListTag other)) {
            return false;
        }
	    return Objects.equals(list, other.list);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(list.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return list.hashCode();
    }
}