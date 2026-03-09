package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class LongTag extends NumericTag {
    private final long value;

    public LongTag(long value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeLong(value);
    }

    @Override
    public byte getId() {
        return TAG_LONG;
    }

    @Override
    public LongTag copy() {
        return new LongTag(value);
    }

    @Override
    public long getAsLong() { return value; }
    @Override
    public int getAsInt() { return (int) value; }
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
