package at.ac.uibk.scheduler;

import at.ac.uibk.scheduler.api.SchedulingAlgorithm;
import at.ac.uibk.scheduler.faast.FaaST;
import at.ac.uibk.scheduler.random.Random;
import at.ac.uibk.util.StreamingLambda;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AvailableImplementationsRequestHandler implements StreamingLambda<Void, Map<String, Object>> {

    @Override
    public Class<Void> getDeserializationClass() {
        return Void.class;
    }

    @Override
    public Optional<Map<String, Object>> handleRequest(Optional<Void> input, Context context) throws Exception {
        return Optional.of(Map.of("availableSchedulers", getAvailableAlgorithms().stream().map(Class::getSimpleName).collect(Collectors.toList())));
    }

    public static List<Class<? extends SchedulingAlgorithm>> getAvailableAlgorithms() {
        return List.of(FaaST.class, Random.class);
    }
}
