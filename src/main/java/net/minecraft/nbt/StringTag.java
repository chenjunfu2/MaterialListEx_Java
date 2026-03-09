package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class StringTag implements Tag {
    private final String value;

    public StringTag(String value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeUTF(value);
    }

    @Override
    public byte getId() {
        return TAG_STRING;
    }

    @Override
    public StringTag copy() {
        return new StringTag(value);
    }

    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    public static StringTag valueOf(String s) {
        return new StringTag(s);
    }
}
