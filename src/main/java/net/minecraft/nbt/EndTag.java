package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public class EndTag implements Tag {
	public static final EndTag INSTANCE = new EndTag();
	
	private EndTag() {
	}
	
	@Override
	public void write(DataOutput output) throws IOException {
		// END tag has no data to write
	}
	
	@Override
	public byte getId() {
		return TAG_END;
	}
	
	@Override
	public EndTag copy() {
		return this; // EndTag is immutable and singleton
	}
	
	@Override
	public String toString() {
		return "END";
	}
}