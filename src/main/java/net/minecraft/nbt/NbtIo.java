package net.minecraft.nbt;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NbtIo {
	private static final OpenOption[] SYNC_OUTPUT_OPTIONS = new OpenOption[]{StandardOpenOption.SYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
	
	public static CompoundTag readCompressed(Path path) throws IOException {
		try (InputStream inputStream = Files.newInputStream(path)) {
			return readCompressed(inputStream);
		}
	}
	
	private static DataInputStream createDecompressorStream(InputStream inputStream) throws IOException {
		return new DataInputStream(new GZIPInputStream(inputStream));
	}
	
	private static DataOutputStream createCompressorStream(OutputStream outputStream) throws IOException {
		return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(outputStream)));
	}
	
	public static CompoundTag readCompressed(InputStream inputStream) throws IOException {
		try (DataInputStream dataInputStream = createDecompressorStream(inputStream)) {
			return read(dataInputStream);
		}
	}
	
	public static void parseCompressed(Path path, StreamTagVisitor streamTagVisitor) throws IOException {
		try (InputStream inputStream = Files.newInputStream(path)) {
			parseCompressed(inputStream, streamTagVisitor);
		}
	}
	
	public static void parseCompressed(InputStream inputStream, StreamTagVisitor streamTagVisitor) throws IOException {
		try (DataInputStream dataInputStream = createDecompressorStream(inputStream)) {
			parse(dataInputStream, streamTagVisitor);
		}
	}
	
	public static void writeCompressed(CompoundTag compoundTag, Path path) throws IOException {
		try (OutputStream outputStream = Files.newOutputStream(path, SYNC_OUTPUT_OPTIONS);
		     BufferedOutputStream outputStream2 = new BufferedOutputStream(outputStream)) {
			writeCompressed(compoundTag, outputStream2);
		}
	}
	
	public static void writeCompressed(CompoundTag compoundTag, OutputStream outputStream) throws IOException {
		try (DataOutputStream dataOutputStream = createCompressorStream(outputStream)) {
			write(compoundTag, dataOutputStream);
		}
	}
	
	public static void write(CompoundTag compoundTag, Path path) throws IOException {
		try (OutputStream outputStream = Files.newOutputStream(path, SYNC_OUTPUT_OPTIONS);
		     BufferedOutputStream outputStream2 = new BufferedOutputStream(outputStream);
		     DataOutputStream dataOutputStream = new DataOutputStream(outputStream2)) {
			write(compoundTag, dataOutputStream);
		}
	}
	
	public static CompoundTag read(Path path) throws IOException {
		if (!Files.exists(path)) {
			return null;
		}
		try (InputStream inputStream = Files.newInputStream(path);
		     DataInputStream dataInputStream = new DataInputStream(inputStream)) {
			return read(dataInputStream);
		}
	}
	
	public static CompoundTag read(DataInput dataInput) throws IOException {
		Tag tag = readUnnamedTag(dataInput);
		if (tag instanceof CompoundTag) {
			return (CompoundTag) tag;
		}
		throw new IOException("Root tag must be a named compound tag");
	}
	
	public static void write(CompoundTag compoundTag, DataOutput dataOutput) throws IOException {
		writeUnnamedTagWithFallback(compoundTag, dataOutput);
	}
	
	public static void parse(DataInput dataInput, StreamTagVisitor streamTagVisitor) throws IOException {
		byte typeId = dataInput.readByte();
		if (typeId == Tag.TAG_END) {
			if (streamTagVisitor.visitRootEntry(null) == StreamTagVisitor.ValueResult.CONTINUE) {
				streamTagVisitor.visitEnd();
			}
			return;
		}
		
		switch (streamTagVisitor.visitRootEntry(null)) {
		case HALT:
			break;
		case BREAK:
			StringTag.skipString(dataInput);
			skipTag(typeId, dataInput);
			break;
		case CONTINUE:
			StringTag.skipString(dataInput);
			parseTag(typeId, dataInput, streamTagVisitor);
			break;
		}
	}
	
	private static void skipTag(byte typeId, DataInput input) throws IOException {
		switch (typeId) {
		case Tag.TAG_BYTE:
			input.readByte();
			break;
		case Tag.TAG_SHORT:
			input.readShort();
			break;
		case Tag.TAG_INT:
			input.readInt();
			break;
		case Tag.TAG_LONG:
			input.readLong();
			break;
		case Tag.TAG_FLOAT:
			input.readFloat();
			break;
		case Tag.TAG_DOUBLE:
			input.readDouble();
			break;
		case Tag.TAG_BYTE_ARRAY:
			int byteLen = input.readInt();
			input.skipBytes(byteLen);
			break;
		case Tag.TAG_STRING:
			StringTag.skipString(input);
			break;
		case Tag.TAG_LIST:
			byte listType = input.readByte();
			int listSize = input.readInt();
			for (int i = 0; i < listSize; i++) {
				skipTag(listType, input);
			}
			break;
		case Tag.TAG_COMPOUND:
			while (true) {
				byte entryType = input.readByte();
				if (entryType == Tag.TAG_END) break;
				StringTag.skipString(input);
				skipTag(entryType, input);
			}
			break;
		case Tag.TAG_INT_ARRAY:
			int intLen = input.readInt();
			for (int i = 0; i < intLen; i++) {
				input.readInt();
			}
			break;
		case Tag.TAG_LONG_ARRAY:
			int longLen = input.readInt();
			for (int i = 0; i < longLen; i++) {
				input.readLong();
			}
			break;
		default:
			throw new IOException("Unknown tag type: " + typeId);
		}
	}
	
	private static void parseTag(byte typeId, DataInput input, StreamTagVisitor visitor) throws IOException {
		switch (typeId) {
		case Tag.TAG_BYTE:
			visitor.visit(input.readByte());
			break;
		case Tag.TAG_SHORT:
			visitor.visit(input.readShort());
			break;
		case Tag.TAG_INT:
			visitor.visit(input.readInt());
			break;
		case Tag.TAG_LONG:
			visitor.visit(input.readLong());
			break;
		case Tag.TAG_FLOAT:
			visitor.visit(input.readFloat());
			break;
		case Tag.TAG_DOUBLE:
			visitor.visit(input.readDouble());
			break;
		case Tag.TAG_BYTE_ARRAY:
			int byteLen = input.readInt();
			byte[] byteArray = new byte[byteLen];
			input.readFully(byteArray);
			visitor.visit(byteArray);
			break;
		case Tag.TAG_STRING:
			visitor.visit(input.readUTF());
			break;
		case Tag.TAG_LIST:
			byte listType = input.readByte();
			int listSize = input.readInt();
			if (visitor.visitList(null, listSize) == StreamTagVisitor.ValueResult.CONTINUE) {
				for (int i = 0; i < listSize; i++) {
					if (visitor.visitElement(null, i) == StreamTagVisitor.EntryResult.ENTER) {
						parseTag(listType, input, visitor);
					} else {
						skipTag(listType, input);
					}
				}
				visitor.visitContainerEnd();
			} else {
				for (int i = 0; i < listSize; i++) {
					skipTag(listType, input);
				}
			}
			break;
		case Tag.TAG_COMPOUND:
			while (true) {
				byte entryType = input.readByte();
				if (entryType == Tag.TAG_END) break;
				String name = input.readUTF();
				if (visitor.visitEntry(null, name) == StreamTagVisitor.EntryResult.ENTER) {
					parseTag(entryType, input, visitor);
				} else {
					skipTag(entryType, input);
				}
			}
			visitor.visitContainerEnd();
			break;
		case Tag.TAG_INT_ARRAY:
			int intLen = input.readInt();
			int[] intArray = new int[intLen];
			for (int i = 0; i < intLen; i++) {
				intArray[i] = input.readInt();
			}
			visitor.visit(intArray);
			break;
		case Tag.TAG_LONG_ARRAY:
			int longLen = input.readInt();
			long[] longArray = new long[longLen];
			for (int i = 0; i < longLen; i++) {
				longArray[i] = input.readLong();
			}
			visitor.visit(longArray);
			break;
		default:
			throw new IOException("Unknown tag type: " + typeId);
		}
	}
	
	public static Tag readAnyTag(DataInput dataInput) throws IOException {
		byte b = dataInput.readByte();
		if (b == Tag.TAG_END) {
			return EndTag.INSTANCE;
		}
		return readTagSafe(dataInput, b);
	}
	
	public static void writeAnyTag(Tag tag, DataOutput dataOutput) throws IOException {
		dataOutput.writeByte(tag.getId());
		if (tag.getId() == Tag.TAG_END) {
			return;
		}
		tag.write(dataOutput);
	}
	
	public static void writeUnnamedTag(Tag tag, DataOutput dataOutput) throws IOException {
		dataOutput.writeByte(tag.getId());
		if (tag.getId() == Tag.TAG_END) {
			return;
		}
		dataOutput.writeUTF("");
		tag.write(dataOutput);
	}
	
	public static void writeUnnamedTagWithFallback(Tag tag, DataOutput dataOutput) throws IOException {
		writeUnnamedTag(tag, new StringFallbackDataOutput(dataOutput));
	}
	
	public static Tag readUnnamedTag(DataInput dataInput) throws IOException {
		byte b = dataInput.readByte();
		if (b == Tag.TAG_END) {
			return EndTag.INSTANCE;
		}
		StringTag.skipString(dataInput);
		return readTagSafe(dataInput, b);
	}
	
	private static Tag readTagSafe(DataInput dataInput, byte typeId) throws IOException {
		switch (typeId) {
		case Tag.TAG_BYTE:
			return new ByteTag(dataInput.readByte());
		case Tag.TAG_SHORT:
			return new ShortTag(dataInput.readShort());
		case Tag.TAG_INT:
			return new IntTag(dataInput.readInt());
		case Tag.TAG_LONG:
			return new LongTag(dataInput.readLong());
		case Tag.TAG_FLOAT:
			return new FloatTag(dataInput.readFloat());
		case Tag.TAG_DOUBLE:
			return new DoubleTag(dataInput.readDouble());
		case Tag.TAG_BYTE_ARRAY:
			int byteLen = dataInput.readInt();
			byte[] byteArray = new byte[byteLen];
			dataInput.readFully(byteArray);
			return new ByteArrayTag(byteArray);
		case Tag.TAG_STRING:
			return new StringTag(dataInput.readUTF());
		case Tag.TAG_LIST:
			byte listType = dataInput.readByte();
			int listSize = dataInput.readInt();
			ListTag list = new ListTag();
			for (int i = 0; i < listSize; i++) {
				list.add(readTagSafe(dataInput, listType));
			}
			return list;
		case Tag.TAG_COMPOUND:
			CompoundTag compound = new CompoundTag();
			while (true) {
				byte entryType = dataInput.readByte();
				if (entryType == Tag.TAG_END) break;
				String name = dataInput.readUTF();
				compound.put(name, readTagSafe(dataInput, entryType));
			}
			return compound;
		case Tag.TAG_INT_ARRAY:
			int intLen = dataInput.readInt();
			int[] intArray = new int[intLen];
			for (int i = 0; i < intLen; i++) {
				intArray[i] = dataInput.readInt();
			}
			return new IntArrayTag(intArray);
		case Tag.TAG_LONG_ARRAY:
			int longLen = dataInput.readInt();
			long[] longArray = new long[longLen];
			for (int i = 0; i < longLen; i++) {
				longArray[i] = dataInput.readLong();
			}
			return new LongArrayTag(longArray);
		default:
			throw new IOException("Unknown tag type: " + typeId);
		}
	}
	
	public static class StringFallbackDataOutput implements DataOutput {
		private final DataOutput parent;
		
		public StringFallbackDataOutput(DataOutput parent) {
			this.parent = parent;
		}
		
		@Override
		public void write(int b) throws IOException {
			parent.write(b);
		}
		
		@Override
		public void write(byte[] b) throws IOException {
			parent.write(b);
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			parent.write(b, off, len);
		}
		
		@Override
		public void writeBoolean(boolean v) throws IOException {
			parent.writeBoolean(v);
		}
		
		@Override
		public void writeByte(int v) throws IOException {
			parent.writeByte(v);
		}
		
		@Override
		public void writeShort(int v) throws IOException {
			parent.writeShort(v);
		}
		
		@Override
		public void writeChar(int v) throws IOException {
			parent.writeChar(v);
		}
		
		@Override
		public void writeInt(int v) throws IOException {
			parent.writeInt(v);
		}
		
		@Override
		public void writeLong(long v) throws IOException {
			parent.writeLong(v);
		}
		
		@Override
		public void writeFloat(float v) throws IOException {
			parent.writeFloat(v);
		}
		
		@Override
		public void writeDouble(double v) throws IOException {
			parent.writeDouble(v);
		}
		
		@Override
		public void writeBytes(String s) throws IOException {
			parent.writeBytes(s);
		}
		
		@Override
		public void writeChars(String s) throws IOException {
			parent.writeChars(s);
		}
		
		@Override
		public void writeUTF(String s) throws IOException {
			try {
				parent.writeUTF(s);
			} catch (UTFDataFormatException e) {
				// Log and pause if in IDE (simplified - just write empty string)
				System.err.println("Failed to write NBT String: " + e.getMessage());
				parent.writeUTF("");
			}
		}
	}
}