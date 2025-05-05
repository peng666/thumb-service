package com.peng.thumb.manager.cache;

import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.ObjUtil;
import lombok.Data;
import org.checkerframework.checker.units.qual.N;
import org.springframework.data.redis.core.convert.Bucket;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

public class HeavyKeeper implements TopK {

    private static final int LOOKUP_TABLE_SIZE = 256;
    private final int k;
    private final int width;
    private final int depth;
    private final double[] lookupTable;
    private final Bucket[][] buckets;
    private final PriorityQueue<Node> minHeap;
    private final BlockingQueue<Item> expelledQueue;
    private final Random random;
    private long total;
    private final int minCount;

    public HeavyKeeper(int k, int width, int depth, double decay, int minCount) {
        this.k = k;
        this.width = width;
        this.depth = depth;
        this.minCount = minCount;

        this.lookupTable = new double[LOOKUP_TABLE_SIZE];
        for (int i = 0; i < LOOKUP_TABLE_SIZE; i++) {
            this.lookupTable[i] = Math.pow(decay, i);
        }

        this.buckets = new Bucket[depth][width];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                this.buckets[i][j] = new Bucket();
            }
        }

        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
        this.expelledQueue = new LinkedBlockingDeque<>();
        this.random = new Random();
        this.total = 0;
    }

    @Override
    public AddResult add(String key, int increment) { // 添加一个元素，返回是否是topK元素，以及是否过期
        if (ObjUtil.isEmpty(key)) {
            return new AddResult(null, false, null);
        }
        byte[] keyBytes = key.getBytes();
        long itemFingerprint = hash(keyBytes);
        int maxCount = 0;

        for (int i = 0; i < depth; i++) {
            int bucketNumber = Math.abs(hash(keyBytes)) % width;
            Bucket bucket = buckets[i][bucketNumber];

            synchronized (bucket) {
                if (bucket.count == 0) {
                    bucket.fingerprint = itemFingerprint;
                    bucket.count = increment;
                    maxCount = Math.max(maxCount, increment);
                } else if (bucket.fingerprint == itemFingerprint) {
                    bucket.count += increment;
                    maxCount = Math.max(maxCount, bucket.count);
                } else {
                    for (int j = 0; j < increment; j++) {
                        double decay = bucket.count < LOOKUP_TABLE_SIZE ?
                                lookupTable[bucket.count] : lookupTable[LOOKUP_TABLE_SIZE - 1];
                        if (random.nextDouble() < decay) {
                            bucket.count--;
                            if (bucket.count == 0) {
                                bucket.fingerprint = itemFingerprint;
                                bucket.count = increment - j;
                                maxCount = Math.max(maxCount, bucket.count);
                                break;
                            }
                        }
                    }
                }
            }
        }
        total += increment;
        if (maxCount < minCount) {
            return new AddResult(null, false, null);
        }
        synchronized (minHeap) {
            boolean isHot = false;
            String expelledKey = null;

            Optional<Node> existing = minHeap.stream().filter(n -> n.key.equals(key)).findFirst();

            if (existing.isPresent()) {
                minHeap.remove(existing.get());
                minHeap.add(new Node(key, maxCount));
                isHot = true;
            } else {
                if (minHeap.size() < k || maxCount >= Objects.requireNonNull(minHeap.peek()).count) {
                    Node newNode = new Node(key, maxCount);
                    if (minHeap.size() >= k) {
                        expelledKey = Objects.requireNonNull(minHeap.poll()).key;
                        expelledQueue.offer(new Item(expelledKey, maxCount));
                    }
                    minHeap.add(newNode);
                    isHot = true;
                }
            }
            return new AddResult(expelledKey, isHot, key);
        }
    }

    @Override
    public List<Item> list() { // 返回当前的topK列表，按照count降序排序
        synchronized (minHeap) {
            List<Item> result = new ArrayList<>(minHeap.size());
            for (Node node : minHeap) {
                result.add(new Item(node.key, node.count));
            }
            result.sort(Comparator.comparingInt(Item::count).reversed());
            return result;
        }
    }

    @Override
    public BlockingQueue<Item> expelled() { // 返回过期的topK列表
        return expelledQueue;
    }

    @Override
    public void fading() {  // 衰减操作，将所有的count减半
        for (Bucket[] row : buckets) {
            for (Bucket bucket : row) {
                synchronized (bucket) {
                    bucket.count = bucket.count >> 1;
                }
            }
        }
        synchronized (minHeap) {
            PriorityQueue<Node> newHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
            for (Node node : minHeap) {
                newHeap.add(new Node(node.key, node.count >> 1));
            }
            minHeap.clear();
            minHeap.addAll(newHeap);
        }
        total = total >> 1;
    }

    @Override
    public long total() {
        return total;
    }

    private static class Bucket {
        long fingerprint;
        int count;
    }

    private static class Node {
        final String key;
        final int count;

        Node(String key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    private static int hash(byte[] data) {
        return HashUtil.murmur32(data);
    }
}

@Data
class AddResult {
    private final String expelledKey;
    private final boolean isHotKey;
    private final String currentKey;

    AddResult(String expelledKey, boolean isHotKey, String currentKey) {
        this.expelledKey = expelledKey;
        this.isHotKey = isHotKey;
        this.currentKey = currentKey;
    }
}
