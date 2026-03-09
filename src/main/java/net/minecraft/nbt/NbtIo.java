package net.minecraft.nbt;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NbtIo {
	
	public static CompoundTag readCompressed(Path path) throws IOException {
		try (InputStream inputStream = Files.newInputStream(path)) {
			return readCompressed(inputStream);
		}
	}
	
	public static CompoundTag readCompressed(InputStream inputStream) throws IOException {
		try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(inputStream)))) {
			return read(dataInputStream);
		}
	}
	
	public static void writeCompressed(CompoundTag compoundTag, Path path) throws IOException {
		try (OutputStream outputStream = Files.newOutputStream(path);
		     BufferedOutputStream bufferedOutput = new BufferedOutputStream(outputStream)) {
			writeCompressed(compoundTag, bufferedOutput);
		}
	}
	
	public static void writeCompressed(CompoundTag compoundTag, OutputStream outputStream) throws IOException {
		try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(outputStream)))) {
			write(compoundTag, dataOutputStream);
		}
	}
	
	public static void write(CompoundTag compoundTag, Path path) throws IOException {
		try (OutputStream outputStream = Files.newOutputStream(path);
		     BufferedOutputStream bufferedOutput = new BufferedOutputStream(outputStream);
		     DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutput)) {
			write(compoundTag, dataOutputStream);
		}
	}
	
	public static CompoundTag read(Path path) throws IOException {
		if (!Files.exists(path)) {
			return null;
		}
		try (InputStream inputStream = Files.newInputStream(path)) {
			try (DataInputStream dataInputStream = new DataInputStream(inputStream)) {
				return read(dataInputStream);
			}
		}
	}
	
	public static CompoundTag read(DataInput input) throws IOException {
		Tag tag = readUnnamedTag(input);
		if (tag instanceof CompoundTag) {
			return (CompoundTag) tag;
		}
		throw new IOException("Root tag must be a named compound tag");
	}
	
	public static void write(CompoundTag compoundTag, DataOutput output) throws IOException {
		writeUnnamedTag(compoundTag, output);
	}
	
	public static Tag readAnyTag(DataInput input) throws IOException {
		byte id = input.readByte();
		if (id == 0) {
			return EndTag.INSTANCE;
		}
		return readTag(input, id);
	}
	
	public static void writeAnyTag(Tag tag, DataOutput output) throws IOException {
		output.writeByte(tag.getId());
		if (tag.getId() == 0) {
			return;
		}
		tag.write(output);
	}
	
	public static void writeUnnamedTag(Tag tag, DataOutput output) throws IOException {
		output.writeByte(tag.getId());
		if (tag.getId() == 0) {
			return;
		}
		output.writeUTF("");
		tag.write(output);
	}
	
	public static Tag readUnnamedTag(DataInput input) throws IOException {
		byte id = input.readByte();
		if (id == 0) {
			return EndTag.INSTANCE;
		}
		input.readUTF(); // Skip the empty name
		return readTag(input, id);
	}
	
	private static Tag readTag(DataInput input, byte id) throws IOException {
		switch (id) {
		case Tag.TAG_BYTE:
			return new ByteTag(input.readByte());
		case Tag.TAG_SHORT:
			return new ShortTag(input.readShort());
		case Tag.TAG_INT:
			return new IntTag(input.readInt());
		case Tag.TAG_LONG:
			return new LongTag(input.readLong());
		case Tag.TAG_FLOAT:
			return new FloatTag(input.readFloat());
		case Tag.TAG_DOUBLE:
			return new DoubleTag(input.readDouble());
		case Tag.TAG_BYTE_ARRAY:
			int byteArrayLength = input.readInt();
			byte[] byteArray = new byte[byteArrayLength];
			input.readFully(byteArray);
			return new ByteArrayTag(byteArray);
		case Tag.TAG_STRING:
			return new StringTag(input.readUTF());
		case Tag.TAG_LIST:
			byte listType = input.readByte();
			int listLength = input.readInt();
			ListTag list = new ListTag();
			for (int i = 0; i < listLength; i++) {
				list.add(readTag(input, listType));
			}
			return list;
		case Tag.TAG_COMPOUND:
			CompoundTag compound = new CompoundTag();
			Tag tag;
			while ((tag = readUnnamedTag(input)).getId() != 0) {
				compound.put(((StringTag) tag).getValue(), tag);
			}
			return compound;
		case Tag.TAG_INT_ARRAY:
			int intArrayLength = input.readInt();
			int[] intArray = new int[intArrayLength];
			for (int i = 0; i < intArrayLength; i++) {
				intArray[i] = input.readInt();
			}
			return new IntArrayTag(intArray);
		case Tag.TAG_LONG_ARRAY:
			int longArrayLength = input.readInt();
			long[] longArray = new long[longArrayLength];
			for (int i = 0; i < longArrayLength; i++) {
				longArray[i] = input.readLong();
			}
			return new LongArrayTag(longArray);
		default:
			throw new IOException("Invalid tag ID: " + id);
		}
	}
}