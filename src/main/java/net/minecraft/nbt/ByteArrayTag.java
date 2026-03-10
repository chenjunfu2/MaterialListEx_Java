package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class ByteArrayTag implements Tag {
    private final byte[] value;

    public ByteArrayTag(byte[] value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(value.length);
        output.write(value);
    }

    @Override
    public byte getId() {
        return TAG_BYTE_ARRAY;
    }

    @Override
    public ByteArrayTag copy() {
        return new ByteArrayTag(Arrays.copyOf(value, value.length));
    }

    public byte[] getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ByteArrayTag other)) {
            return false;
        }
	    return Arrays.equals(value, other.value);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
