package litematica.exporter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Language {
	private final JsonObject data;
	private static final Map<String, Language> cache = new ConcurrentHashMap<>();
	
	public enum KeyType {
		NONE,
		BLOCK,
		ENTITY,
		ITEM
	}
	
	private static final String[] KEY_TYPE_PREFIX = {
		"",           // NONE
		"block.",     // BLOCK
		"entity.",    // ENTITY
		"item."       // ITEM
	};
	
	private Language(JsonObject data) {
		this.data = data != null ? data : new JsonObject();
	}
	
	public Language()
	{
		data = null;
	}
	
	public static Language load(String filePath) throws IOException {
		// 检查缓存
		if (cache.containsKey(filePath)) {
			return cache.get(filePath);
		}
		
		Path path = Paths.get(filePath);
		if (!Files.exists(path)) {
			System.err.println("Language file not found: " + filePath);
			return new Language(new JsonObject());
		}
		
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			Language lang = new Language(json);
			cache.put(filePath, lang);
			return lang;
		} catch (JsonSyntaxException | IllegalStateException e) {
			System.err.println("JSON parse error: " + e.getMessage());
			return new Language(new JsonObject());
		}
	}
	
	public String translate(KeyType type, String key) {
		if (type == null || type == KeyType.NONE) {
			type = KeyType.NONE;
		}
		
		// 转换命名空间
		String jsonKey = key.replace(':', '.');
		
		// 尝试查找
		String prefixedKey = KEY_TYPE_PREFIX[type.ordinal()] + jsonKey;
		if (data.has(prefixedKey)) {
			return data.get(prefixedKey).getAsString();
		}
		
		// 如果是ITEM，尝试用BLOCK类型再查一次
		if (type == KeyType.ITEM) {
			prefixedKey = KEY_TYPE_PREFIX[KeyType.BLOCK.ordinal()] + jsonKey;
			if (data.has(prefixedKey)) {
				return data.get(prefixedKey).getAsString();
			}
		}
		
		// 返回原始键名
		return key;
	}
	
	public void printPrefixes() {
		Set<String> prefixes = new TreeSet<>();
		
		for (String key : data.keySet()) {
			int dotPos = key.indexOf('.');
			if (dotPos > 0) {
				prefixes.add(key.substring(0, dotPos));
			} else {
				prefixes.add(key);
			}
		}
		
		for (String prefix : prefixes) {
			System.out.println(prefix);
		}
	}
}