package at.ac.uibk.scheduler.random;

import at.ac.uibk.metadata.api.model.FunctionType;
import at.ac.uibk.metadata.api.model.Region;
import at.ac.uibk.metadata.api.model.functions.FunctionDeployment;
import at.ac.uibk.scheduler.api.Communication;
import at.ac.uibk.scheduler.api.MetadataCache;
import at.ac.uibk.scheduler.api.SchedulingAlgorithm;
import at.ac.uibk.scheduler.api.node.AtomicFunctionNode;
import at.ac.uibk.scheduler.api.node.FunctionNode;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Random implements SchedulingAlgorithm {

    final Map<Integer, Map<Region, Integer>> levelConcurrencyMap = new HashMap<>();

    @Override
    public void schedule(DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {

        TopologicalOrderIterator<FunctionNode, Communication> graphIterator = new TopologicalOrderIterator<>(graph);

        FunctionNode start = null;

        while (graphIterator.hasNext()) {

            final FunctionNode next = graphIterator.next();

            if (start == null) {
                start = next;
            }

            if (!(next instanceof AtomicFunctionNode)) {
                continue;
            }

            var paths = new AllDirectedPaths<>(graph).getAllPaths(start, next, true, null);

            int depth = paths.stream()
                    .mapToInt(GraphPath::getLength)
                    .max().orElseThrow();

            final Map<Region, Integer> regionsOnCurrentLevel = this.levelConcurrencyMap
                    .computeIfAbsent(depth, k -> new HashMap<>());

            final String functionTypeName = ((AtomicFunctionNode) next).getAtomicFunction().getType();

            final FunctionType functionType = MetadataCache.get().getFunctionTypesByName().get(functionTypeName);

            java.util.Random randomNumber = new java.util.Random();

            List<FunctionDeployment> possibleDeployments = MetadataCache.get().getDeploymentsByFunctionType().get(functionType)
                    .stream()
                    .filter(fd -> {
                        synchronized (regionsOnCurrentLevel) {
                            var region = MetadataCache.get().getRegionFor(fd).orElseThrow();
                            return !regionsOnCurrentLevel.containsKey(region) || regionsOnCurrentLevel.get(region) < MetadataCache.get().getMaxConcurrencyByDeployment().get(fd);
                        }
                    })
                    .collect(Collectors.toList());


            final FunctionDeployment random = possibleDeployments.get(randomNumber.nextInt(possibleDeployments.size()));

            final Region regionOfRandom = MetadataCache.get().getRegionFor(random).orElseThrow();

            if (regionsOnCurrentLevel.containsKey(regionOfRandom)) {
                regionsOnCurrentLevel.put(regionOfRandom, regionsOnCurrentLevel.get(regionOfRandom) + 1);
            } else {
                regionsOnCurrentLevel.put(regionOfRandom, 1);
            }

            ((AtomicFunctionNode) next).setSchedulingDecision(random);

        }

    }

    /**
     * Not needed for random scheduling.
     *
     * @return 0
     */
    @Override
    public double calculateAverageTime(AtomicFunctionNode node) {
        return 0;
    }

}
