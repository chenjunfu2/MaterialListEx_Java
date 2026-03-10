package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class ByteTag extends NumericTag {
    private final byte value;

    public ByteTag(byte value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeByte(value);
    }

    @Override
    public byte getId() {
        return TAG_BYTE;
    }

    @Override
    public ByteTag copy() {
        return new ByteTag(value);
    }

    @Override
    public long getAsLong() { return value; }
    @Override
    public int getAsInt() { return value; }
    @Override
    public short getAsShort() { return value; }
    @Override
    public byte getAsByte() { return value; }
    @Override
    public double getAsDouble() { return value; }
    @Override
    public float getAsFloat() { return value; }
    @Override
    public Number getAsNumber() { return value; }
    
    public static ByteTag valueOf(boolean b) {
        return new ByteTag((byte)(b ? 1 : 0));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ByteTag other)) {
            return false;
        }
	    return value == other.value;
    }
}
