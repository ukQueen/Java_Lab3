package com.project;

import java.util.*;

public class MultiValueMap<K, V> implements Map <K, Collection<V>>, Iterable<V> {
    private final List<Pair<K, Collection<V>>> map = new ArrayList<>();


    private static class Pair<K, V> implements Map.Entry<K,V>{
        final K key;
        V values;

        Pair(K key, V values){
            this.key = key;
            this.values = values;
        }

        @Override
        public K getKey(){
            return key;
        }

        @Override
        public V getValue() {
            return values;
        }

        @Override
        public V setValue(V value) {
            V oldValue = values;
            values = value;
            return oldValue;
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        for (Pair<K, Collection<V>> pair : map){
            if (pair.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Pair<K, Collection<V>> pair : map){
            if (pair.values.contains(value)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<V> get(Object key) {
        for (Pair<K, Collection<V>> pair : map){
            if (pair.key.equals(key)){
                return pair.values;
            }
        }
        return null;
    }

    @Override
    public Collection<V> put(K key, Collection<V> values) {
        for (Pair <K,Collection<V>> pair : map){
            if (pair.key.equals(key)){
                Collection<V> oldValues = pair.values;
                pair.values = values;
                return oldValues;
            }
        }
        map.add(new Pair<>(key, values));
        return null;
    }

    @Override
    public Collection<V> remove(Object key) {
        Iterator<Pair<K, Collection<V>>> iterator = map.iterator();
        while (iterator.hasNext()){
            Pair<K, Collection<V>> pair = iterator.next();
            if (pair.key.equals(key)){
                iterator.remove();
                return pair.values;
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends Collection<V>> m) {
        for (Map.Entry<? extends K, ? extends Collection<V>> pair : m.entrySet()){
            put(pair.getKey(), pair.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        List<K> keys = new ArrayList<>();
        for (Pair<K, Collection<V>> pair : map){
            keys.add(pair.key);
        }
        return new HashSet<>(keys);
    }

    @Override
    public Collection<Collection<V>> values() {
        List<Collection<V>> values = new ArrayList<>();
        for (Pair<K, Collection<V>> pair : map){
            values.add(pair.values);
        }
        return values;
    }

    @Override
    public Set<Map.Entry<K, Collection<V>>> entrySet() {
        return new HashSet<>(map);
    }

    @Override
    public Iterator<V> iterator() {
        return new Iterator<V>() {
            private int pairIndex = 0;
            private Iterator<V> currentIterator = Collections.emptyIterator();

            @Override
            public  boolean hasNext(){
                while(!currentIterator.hasNext() && pairIndex < map.size()) {
                    currentIterator = map.get(pairIndex++).values.iterator();
                }
                return currentIterator.hasNext();
            }

            @Override
            public V next(){
                if(!hasNext()){
                    throw new NoSuchElementException();
                }
                return currentIterator.next();
            }
        };
    }
}
