package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class DoubleTag extends NumericTag {
    private final double value;

    public DoubleTag(double value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeDouble(value);
    }

    @Override
    public byte getId() {
        return TAG_DOUBLE;
    }

    @Override
    public DoubleTag copy() {
        return new DoubleTag(value);
    }

    @Override
    public long getAsLong() { return (long) value; }
    @Override
    public int getAsInt() { return (int) value; }
    @Override
    public short getAsShort() { return (short) (value); }
    @Override
    public byte getAsByte() { return (byte) (value); }
    @Override
    public double getAsDouble() { return value; }
    @Override
    public float getAsFloat() { return (float) value; }
    @Override
    public Number getAsNumber() { return value; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DoubleTag other)) {
            return false;
        }
	    return Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
    }
}
