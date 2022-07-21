package at.ac.uibk.scheduler.api.node;

import at.ac.uibk.scheduler.api.Communication;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public abstract class FunctionNode {

    private static final AtomicLong atomicLong = new AtomicLong();

    private final long id;

    private Object algorithmInfo;

    protected FunctionNode() {
        this.id = FunctionNode.atomicLong.getAndIncrement();
    }

    public long getId() {
        return this.id;
    }

    public Object getAlgorithmInfo() {
        return this.algorithmInfo;
    }

    private Stream<FunctionNode> getDirectParentsStream(final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {
        try {
            return graph.incomingEdgesOf(this).stream().map(graph::getEdgeSource);
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    public <T> Stream<T> getFirstAlgorithmInfosTypedFromAllPredecessors(final DefaultDirectedWeightedGraph<FunctionNode, Communication> graph) {
        return this.getDirectParentsStream(graph)
                .flatMap(p -> {
                    final T parentInfo = p.getAlgorithmInfoTyped();
                    if (parentInfo != null) {
                        return Stream.of(parentInfo);
                    }
                    return p.getFirstAlgorithmInfosTypedFromAllPredecessors(graph);
                });
    }

    @SuppressWarnings("unchecked")
    public <T> T getAlgorithmInfoTyped() {
        return (T) this.getAlgorithmInfo();
    }

    public void setAlgorithmInfo(final Object algorithmInfo) {
        this.algorithmInfo = algorithmInfo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionNode)) {
            return false;
        }
        final FunctionNode that = (FunctionNode) o;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
