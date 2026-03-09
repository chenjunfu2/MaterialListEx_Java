package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public interface Tag {
    byte TAG_END = 0;
    byte TAG_BYTE = 1;
    byte TAG_SHORT = 2;
    byte TAG_INT = 3;
    byte TAG_LONG = 4;
    byte TAG_FLOAT = 5;
    byte TAG_DOUBLE = 6;
    byte TAG_BYTE_ARRAY = 7;
    byte TAG_STRING = 8;
    byte TAG_LIST = 9;
    byte TAG_COMPOUND = 10;
    byte TAG_INT_ARRAY = 11;
    byte TAG_LONG_ARRAY = 12;

    void write(DataOutput output) throws IOException;
    byte getId();
    Tag copy();
    
    default String getAsString() {
        return toString();
    }
}
