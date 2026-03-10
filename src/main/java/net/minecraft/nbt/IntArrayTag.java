package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class IntArrayTag implements Tag {
    private final int[] value;

    public IntArrayTag(int[] value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(value.length);
        for (int i : value) {
            output.writeInt(i);
        }
    }

    @Override
    public byte getId() {
        return TAG_INT_ARRAY;
    }

    @Override
    public IntArrayTag copy() {
        return new IntArrayTag(Arrays.copyOf(value, value.length));
    }

    public int[] getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IntArrayTag other)) {
            return false;
        }
	    return Arrays.equals(value, other.value);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
