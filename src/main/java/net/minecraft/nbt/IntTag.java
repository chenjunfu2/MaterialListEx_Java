package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class IntTag extends NumericTag {
    private final int value;

    public IntTag(int value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(value);
    }

    @Override
    public byte getId() {
        return TAG_INT;
    }

    @Override
    public IntTag copy() {
        return new IntTag(value);
    }

    @Override
    public long getAsLong() { return value; }
    @Override
    public int getAsInt() { return value; }
    @Override
    public short getAsShort() { return (short) (value & 0xFFFF); }
    @Override
    public byte getAsByte() { return (byte) (value & 0xFF); }
    @Override
    public double getAsDouble() { return value; }
    @Override
    public float getAsFloat() { return value; }
    @Override
    public Number getAsNumber() { return value; }
}
