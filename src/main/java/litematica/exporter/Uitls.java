package litematica.exporter;

//import com.mojang.brigadier.StringReader;
import net.minecraft.nbt.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// 基础坐标类
record BlockPos(int x, int y, int z) {
	public BlockPos add(BlockPos other) {
		return new BlockPos(x + other.x, y + other.y, z + other.z);
	}
	
	public BlockPos sub(BlockPos other) {
		return new BlockPos(x - other.x, y - other.y, z - other.z);
	}
}

// 物品信息（带NBT标签）
class ItemInfo {
	final String name;
	final CompoundTag tag;
	final long hash;
	
	public ItemInfo(String name, CompoundTag tag) {
		this.name = name;
		this.tag = tag != null ? tag : new CompoundTag();
		this.hash = computeHash();
	}
	
	private long computeHash() {
		final long SEED = 0xDE35B92A7F41806CL;
		long h = SEED;
		for (byte b : name.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
			h = h * 0x9E3779B97F4A7C15L + (b & 0xFF);
		}
		if (!tag.isEmpty()) {
			h = h * 0x9E3779B97F4A7C15L + tag.hashCode();
		}
		return h;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ItemInfo that)) return false;
		return hash == that.hash && name.equals(that.name) && tag.equals(that.tag);
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(hash);
	}
}

// 无标签物品信息
record NoTagItemInfo(String name, long hash) {
	public NoTagItemInfo(String name) {
		this(name, computeHash(name));
	}
	
	private static long computeHash(String name) {
		final long SEED = 0x83B01A83062C4F5DL;
		long h = SEED;
		for (byte b : name.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
			h = h * 0x9E3779B97F4A7C15L + (b & 0xFF);
		}
		return h;
	}
}

// 物品堆
record ItemStack(String name, CompoundTag tag, long count) {
	public ItemStack scale(long multiplier) {
		return new ItemStack(name, tag, count * multiplier);
	}
}

// 带统计的Map包装类
class MapSortList<K> {
	final Map<K, AtomicLong> map = new HashMap<>();
	List<Map.Entry<K, AtomicLong>> sortedList = new ArrayList<>();
	
	public void add(K key, long value) {
		map.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(value);
	}
	
	public void sort() {
		sortedList = map.entrySet().stream()
		                .sorted((a, b) -> {
			                long cmp = b.getValue().get() - a.getValue().get();
			                if (cmp != 0) return Long.signum(cmp);
			                return a.getKey().toString().compareTo(b.getKey().toString());
		                })
		                .collect(Collectors.toList());
	}
	
	public void merge(MapSortList<K> other) {
		other.map.forEach((k, v) -> add(k, v.get()));
	}
}

// 二级Map结构
class MapMSL<K> extends HashMap<K, MapSortList<ItemInfo>> {
	public void sortAll() {
		values().forEach(MapSortList::sort);
	}
	
	public void merge(MapMSL<K> other) {
		other.forEach((k, v) ->
			              computeIfAbsent(k, kk -> new MapSortList<>()).merge(v));
	}
}

// 区域统计
class RegionStats {
	final String regionName;
	final MapSortList<NoTagItemInfo> blockItems = new MapSortList<>();      // 方块转物品
	final MapSortList<ItemInfo> tileEntityContainers = new MapSortList<>(); // 方块实体容器
	final MapMSL<String> parentInfoTEC = new MapMSL<>();                    // 带来源的方块实体
	final MapSortList<NoTagItemInfo> entities = new MapSortList<>();        // 实体
	final MapSortList<ItemInfo> entityContainers = new MapSortList<>();     // 实体容器
	final MapMSL<String> parentInfoEC = new MapMSL<>();                     // 带来源的实体容器
	final MapSortList<ItemInfo> entityInventories = new MapSortList<>();    // 实体物品栏
	final MapMSL<String> parentInfoEI = new MapMSL<>();                     // 带来源的实体物品栏
	
	public RegionStats(String name) {
		this.regionName = name;
	}
	
	public void sortAll() {
		blockItems.sort();
		tileEntityContainers.sort();
		parentInfoTEC.sortAll();
		entities.sort();
		entityContainers.sort();
		parentInfoEC.sortAll();
		entityInventories.sort();
		parentInfoEI.sortAll();
	}
	
	public void merge(RegionStats other) {
		blockItems.merge(other.blockItems);
		tileEntityContainers.merge(other.tileEntityContainers);
		parentInfoTEC.merge(other.parentInfoTEC);
		entities.merge(other.entities);
		entityContainers.merge(other.entityContainers);
		parentInfoEC.merge(other.parentInfoEC);
		entityInventories.merge(other.entityInventories);
		parentInfoEI.merge(other.parentInfoEI);
	}
}