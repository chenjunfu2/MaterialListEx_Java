package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class FloatTag extends NumericTag {
    private final float value;

    public FloatTag(float value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeFloat(value);
    }

    @Override
    public byte getId() {
        return TAG_FLOAT;
    }

    @Override
    public FloatTag copy() {
        return new FloatTag(value);
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
    public float getAsFloat() { return value; }
    @Override
    public Number getAsNumber() { return value; }
    
    public static FloatTag valueOf(float f) {
        return new FloatTag(f);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FloatTag other)) {
            return false;
        }
	    return Float.floatToIntBits(value) == Float.floatToIntBits(other.value);
    }
    
    @Override
    public int hashCode() {
        return Float.hashCode(value);
    }
}
