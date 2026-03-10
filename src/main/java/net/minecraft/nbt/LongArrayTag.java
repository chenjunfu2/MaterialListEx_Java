package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class LongArrayTag implements Tag {
    private final long[] value;

    public LongArrayTag(long[] value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(value.length);
        for (long l : value) {
            output.writeLong(l);
        }
    }

    @Override
    public byte getId() {
        return TAG_LONG_ARRAY;
    }

    @Override
    public LongArrayTag copy() {
        return new LongArrayTag(Arrays.copyOf(value, value.length));
    }

    public long[] getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LongArrayTag other)) {
            return false;
        }
	    return Arrays.equals(value, other.value);
    }
}
