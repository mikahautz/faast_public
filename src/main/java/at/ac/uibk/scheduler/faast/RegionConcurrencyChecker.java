package at.ac.uibk.scheduler.faast;

import at.ac.uibk.metadata.api.model.DetailedProvider;
import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.scheduler.api.MetadataCache;
import at.ac.uibk.scheduler.api.SchedulingException;

import java.util.*;
import java.util.stream.Collectors;

public class RegionConcurrencyChecker {

    private final Map<Region, SortedMap<Double, Integer>> regionConcurrencyMap;


    public RegionConcurrencyChecker() {
        this.regionConcurrencyMap = new HashMap<>();
    }

    public Optional<Double> getEarliestRuntimeInRegionBetween(final Region region, final double start,
                                                              final double end,
                                                              final double duration) {

        assert start + duration <= end;

        final int maximumConcurrency =
                MetadataCache.get().getDetailedProviderFor(region)
                        .map(DetailedProvider::getMaxConcurrency)
                        .orElse(0);

        final SortedMap<Double, Integer> concurrencyMap = this.regionConcurrencyMap.computeIfAbsent(region,
                RegionConcurrencyChecker::initializeInnerMap);

        final SortedSet<Double> startTimesWithConcurrencyNotReached =
                concurrencyMap.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() >= start)
                        .filter(entry -> entry.getValue() < maximumConcurrency)
                        .map(Map.Entry::getKey)
                        .sorted()
                        .collect(Collectors.toCollection(TreeSet::new));

        final SortedSet<Double> startTimesWithConcurrencyReached =
                concurrencyMap.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() >= maximumConcurrency)
                        .map(Map.Entry::getKey)
                        .sorted()
                        .collect(Collectors.toCollection(TreeSet::new));

        if (startTimesWithConcurrencyNotReached.isEmpty()) {
            return Optional.empty();
        }

        //check predecessor
        if (!concurrencyMap.containsKey(start)) {
            final Double predecessorKey = concurrencyMap.headMap(start).lastKey();

            final Integer predecessorConcurrency = concurrencyMap.get(predecessorKey);

            if (predecessorConcurrency < maximumConcurrency &&
                    RegionConcurrencyChecker.isSuccessorAlright(startTimesWithConcurrencyReached, start, duration)) {
                //here we already know that the concurrency is okay for the predecessor and for the current node
                // we also know that start + duration < end, as this is an assert
                // we just need to check if there isnt a concurrency limit until the end of our execution duration,
                // then we can start a bit early
                return Optional.of(start);
            }
        }

        // at this point the first element of startTimesWithConcurrencyNotReached is the first possible time
        // our function can start
        for (final Double startTime : startTimesWithConcurrencyNotReached) {

            final Integer currentConcurrency = concurrencyMap.get(startTime);

            if (currentConcurrency < maximumConcurrency &&
                    RegionConcurrencyChecker.isSuccessorAlright(startTimesWithConcurrencyReached, startTime, duration) &&
                    startTime + duration <= end) {
                if (startTime >= Double.MAX_VALUE) {
                    throw new SchedulingException("unexpected state occured when checking region concurrency");
                }

                return Optional.of(startTime);
            }
        }

        //if we have not returned until now, for this slot, no concurrency is free on this region
        return Optional.empty();
    }

    private static boolean isSuccessorAlright(final SortedSet<Double> concurrencyReachedStartTimes,
                                              final double start, final double duration) {
        return concurrencyReachedStartTimes.subSet(start, start + duration).isEmpty();
    }

    public void scheduleFunction(final Region region, final double from, final double to) {

        if (from >= to) {
            throw new SchedulingException("preconditions of schedulefunction not met");
        }

        final SortedMap<Double, Integer> concurrenyMap = this.regionConcurrencyMap.computeIfAbsent(region,
                RegionConcurrencyChecker::initializeInnerMap);

        final int maximumConcurrency =
                MetadataCache.get().getDetailedProviderFor(region)
                        .map(DetailedProvider::getMaxConcurrency)
                        .orElse(0);


        if (!concurrenyMap.containsKey(from)) {
            final Double predecessorKey = concurrenyMap.headMap(from).lastKey();
            concurrenyMap.put(from, concurrenyMap.get(predecessorKey));
        }

        concurrenyMap.subMap(from, to).replaceAll((k, v) -> {
            if ((v + 1) > maximumConcurrency) {
                throw new SchedulingException("invalid state occured when processing regions");
            }

            return v + 1;
        });


        if (!concurrenyMap.containsKey(to)) {
            final Double predecessor = concurrenyMap.headMap(to).lastKey();
            if (concurrenyMap.get(predecessor) <= 0) {
                throw new IllegalStateException("entry map in wrong state");
            }
            concurrenyMap.put(to, concurrenyMap.get(predecessor) - 1);
        }


        if (concurrenyMap.entrySet()
                .stream().filter(doubleIntegerEntry -> doubleIntegerEntry.getValue() == 0).count() < 2) {
            throw new IllegalStateException("entry map in wrong state");
        }


    }

    private static SortedMap<Double, Integer> initializeInnerMap(final Region region) {
        final TreeMap<Double, Integer> instance = new TreeMap<>();
        instance.put(0D, 0);
        instance.put(Double.MAX_VALUE, 0);
        return instance;
    }

}
