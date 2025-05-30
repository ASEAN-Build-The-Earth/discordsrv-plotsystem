package asia.buildtheearth.asean.discord.plotsystem.core.database;

public class DatabaseEntry<K, V> {
    private final K key;
    private final V value;

    public DatabaseEntry(K k, V v) {
        this.key = k;
        this.value = v;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}