/*
 * Decompiled with CFR 0.2.2 (FabricMC 7c48b8c4).
 */
package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface TagType<T extends Tag> {
	public T load(DataInput var1) throws IOException;
	
	public StreamTagVisitor.ValueResult parse(DataInput var1, StreamTagVisitor var2) throws IOException;
	
	default public void parseRoot(DataInput dataInput, StreamTagVisitor streamTagVisitor) throws IOException {
		switch (streamTagVisitor.visitRootEntry(this)) {
		case CONTINUE: {
			this.parse(dataInput, streamTagVisitor);
			break;
		}
		case HALT: {
			break;
		}
		case BREAK: {
			this.skip(dataInput);
		}
		}
	}
	
	public void skip(DataInput var1, int var2) throws IOException;
	
	public void skip(DataInput var1) throws IOException;
	
	public String getName();
	
	public String getPrettyName();
	
	public static interface VariableSize<T extends Tag>
		extends TagType<T> {
		@Override
		default public void skip(DataInput dataInput, int i) throws IOException {
			for (int j = 0; j < i; ++j) {
				this.skip(dataInput);
			}
		}
	}
	
	public static interface StaticSize<T extends Tag>
		extends TagType<T> {
		@Override
		default public void skip(DataInput dataInput) throws IOException {
			dataInput.skipBytes(this.size());
		}
		
		@Override
		default public void skip(DataInput dataInput, int i) throws IOException {
			dataInput.skipBytes(this.size() * i);
		}
		
		public int size();
	}
}

