package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class ShortTag extends NumericTag {
    private final short value;

    public ShortTag(short value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeShort(value);
    }

    @Override
    public byte getId() {
        return TAG_SHORT;
    }

    @Override
    public ShortTag copy() {
        return new ShortTag(value);
    }

    @Override
    public long getAsLong() { return value; }
    @Override
    public int getAsInt() { return value; }
    @Override
    public short getAsShort() { return value; }
    @Override
    public byte getAsByte() { return (byte) (value & 0xFF); }
    @Override
    public double getAsDouble() { return value; }
    @Override
    public float getAsFloat() { return value; }
    @Override
    public Number getAsNumber() { return value; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ShortTag other)) {
            return false;
        }
	    return value == other.value;
    }
}
