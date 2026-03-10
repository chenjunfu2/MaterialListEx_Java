package net.minecraft.nbt;

import java.io.DataInput;
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
    
    public static void skipString(DataInput dataInput) throws IOException {
        dataInput.skipBytes(dataInput.readUnsignedShort());
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
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StringTag other)) {
            return false;
        }
	    return value.equals(other.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
