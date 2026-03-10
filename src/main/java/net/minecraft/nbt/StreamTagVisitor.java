package net.minecraft.nbt;

public interface StreamTagVisitor {
	ValueResult visitEnd();
	
	ValueResult visit(String var1);
	
	ValueResult visit(byte var1);
	
	ValueResult visit(short var1);
	
	ValueResult visit(int var1);
	
	ValueResult visit(long var1);
	
	ValueResult visit(float var1);
	
	ValueResult visit(double var1);
	
	ValueResult visit(byte[] var1);
	
	ValueResult visit(int[] var1);
	
	ValueResult visit(long[] var1);
	
	ValueResult visitList(TagType<?> var1, int var2);
	
	EntryResult visitEntry(TagType<?> var1);
	
	EntryResult visitEntry(TagType<?> var1, String var2);
	
	EntryResult visitElement(TagType<?> var1, int var2);
	
	ValueResult visitContainerEnd();
	
	ValueResult visitRootEntry(TagType<?> var1);
	
	enum EntryResult {
		ENTER,
		SKIP,
		BREAK,
		HALT;
		
	}
	
	enum ValueResult {
		CONTINUE,
		BREAK,
		HALT;
		
	}
}

