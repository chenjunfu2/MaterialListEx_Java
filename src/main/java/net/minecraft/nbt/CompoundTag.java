package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CompoundTag implements Tag {
    private final Map<String, Tag> tags = new HashMap<>();

    @Override
    public void write(DataOutput output) throws IOException {
        for (String key : tags.keySet()) {
            Tag tag = tags.get(key);
            output.writeByte(tag.getId());
            if (tag.getId() != TAG_END) {
                output.writeUTF(key);
                tag.write(output);
            }
        }
        output.writeByte(TAG_END);
    }

    @Override
    public byte getId() {
        return TAG_COMPOUND;
    }

    @Override
    public CompoundTag copy() {
        CompoundTag copy = new CompoundTag();
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public Set<String> getAllKeys() {
        return tags.keySet();
    }
    
    public Set<String> keySet() {
        return tags.keySet();
    }

    public void put(String key, Tag tag) {
        tags.put(key, tag);
    }

    public void putByte(String key, byte value) {
        tags.put(key, new ByteTag(value));
    }

    public void putShort(String key, short value) {
        tags.put(key, new ShortTag(value));
    }

    public void putInt(String key, int value) {
        tags.put(key, new IntTag(value));
    }

    public void putLong(String key, long value) {
        tags.put(key, new LongTag(value));
    }
    
    public void putUUID(String key, UUID value) {
        putLong(key + "Most", value.getMostSignificantBits());
        putLong(key + "Least", value.getLeastSignificantBits());
    }

    public void putFloat(String key, float value) {
        tags.put(key, new FloatTag(value));
    }

    public void putDouble(String key, double value) {
        tags.put(key, new DoubleTag(value));
    }

    public void putString(String key, String value) {
        tags.put(key, new StringTag(value));
    }

    public void putByteArray(String key, byte[] value) {
        tags.put(key, new ByteArrayTag(value));
    }

    public void putIntArray(String key, int[] value) {
        tags.put(key, new IntArrayTag(value));
    }

    public void putLongArray(String key, long[] value) {
        tags.put(key, new LongArrayTag(value));
    }

    public void putBoolean(String key, boolean value) {
        putByte(key, (byte) (value ? 1 : 0));
    }

    public Tag get(String key) {
        return tags.get(key);
    }

    public boolean contains(String key) {
        return tags.containsKey(key);
    }

    public boolean contains(String key, int type) {
        Tag tag = tags.get(key);
        return tag != null && tag.getId() == type;
    }

    public byte getByte(String key) {
        Tag tag = tags.get(key);
        return tag instanceof NumericTag ? ((NumericTag) tag).getAsByte() : 0;
    }

    public short getShort(String key) {
        Tag tag = tags.get(key);
        return tag instanceof NumericTag ? ((NumericTag) tag).getAsShort() : 0;
    }

    public int getInt(String key) {
        Tag tag = tags.get(key);
        return tag instanceof NumericTag ? ((NumericTag) tag).getAsInt() : 0;
    }

    public long getLong(String key) {
        Tag tag = tags.get(key);
        return tag instanceof NumericTag ? ((NumericTag) tag).getAsLong() : 0;
    }

    public float getFloat(String key) {
        Tag tag = tags.get(key);
        return tag instanceof NumericTag ? ((NumericTag) tag).getAsFloat() : 0;
    }

    public double getDouble(String key) {
        Tag tag = tags.get(key);
        return tag instanceof NumericTag ? ((NumericTag) tag).getAsDouble() : 0;
    }

    public String getString(String key) {
        Tag tag = tags.get(key);
        return tag instanceof StringTag ? ((StringTag) tag).getValue() : "";
    }

    public byte[] getByteArray(String key) {
        Tag tag = tags.get(key);
        return tag instanceof ByteArrayTag ? ((ByteArrayTag) tag).getValue() : new byte[0];
    }

    public int[] getIntArray(String key) {
        Tag tag = tags.get(key);
        return tag instanceof IntArrayTag ? ((IntArrayTag) tag).getValue() : new int[0];
    }

    public long[] getLongArray(String key) {
        Tag tag = tags.get(key);
        return tag instanceof LongArrayTag ? ((LongArrayTag) tag).getValue() : new long[0];
    }

    public CompoundTag getCompound(String key) {
        Tag tag = tags.get(key);
        return tag instanceof CompoundTag ? (CompoundTag) tag : new CompoundTag();
    }
    
    public CompoundTag getCompoundOrEmpty(String key) {
        return getCompound(key);
    }

    public ListTag getList(String key, int type) {
        Tag tag = tags.get(key);
        if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            if (list.isEmpty() || list.getElementType() == type) {
                return list;
            }
        }
        return new ListTag();
    }
    
    public ListTag getListOrEmpty(String key) {
        Tag tag = tags.get(key);
        return tag instanceof ListTag ? (ListTag) tag : new ListTag();
    }

    public boolean getBoolean(String key) {
        return getByte(key) != 0;
    }
    
    public void remove(String key) {
        tags.remove(key);
    }
    
    public boolean isEmpty() {
        return tags.isEmpty();
    }
    
    public void merge(CompoundTag other) {
        for (String key : other.tags.keySet()) {
            Tag tag = other.tags.get(key);
            if (tag.getId() == TAG_COMPOUND) {
                if (this.contains(key, TAG_COMPOUND)) {
                    CompoundTag thisChild = this.getCompound(key);
                    CompoundTag otherChild = (CompoundTag) tag;
                    thisChild.merge(otherChild);
                } else {
                    this.put(key, tag.copy());
                }
            } else {
                this.put(key, tag.copy());
            }
        }
    }
    
    public int size() {
        return tags.size();
    }
    
    // Helper methods for "Or" defaults
    public int getIntOr(String key, int defaultValue) {
        return contains(key, TAG_INT) ? getInt(key) : defaultValue;
    }
    
    public String getStringOr(String key, String defaultValue) {
        return contains(key, TAG_STRING) ? getString(key) : defaultValue;
    }
    
    public long getLongOr(String key, long defaultValue) {
        return contains(key, TAG_LONG) ? getLong(key) : defaultValue;
    }
    
    public boolean getBooleanOr(String key, boolean defaultValue) {
        return contains(key) ? getBoolean(key) : defaultValue;
    }
    
    public byte getByteOr(String key, byte defaultValue) {
        return contains(key, TAG_BYTE) ? getByte(key) : defaultValue;
    }
    
    public float getFloatOr(String key, float defaultValue) {
        return contains(key, TAG_FLOAT) ? getFloat(key) : defaultValue;
    }
    
    public double getDoubleOr(String key, double defaultValue) {
        return contains(key, TAG_DOUBLE) ? getDouble(key) : defaultValue;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CompoundTag other)) {
            return false;
        }
	    return tags.equals(other.tags);
    }
}
