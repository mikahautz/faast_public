package at.ac.uibk.scheduler;

import at.ac.uibk.core.Workflow;
import at.ac.uibk.core.functions.ParallelFor;
import at.ac.uibk.core.functions.SequentialFor;
import at.ac.uibk.core.functions.objects.Condition;
import at.ac.uibk.scheduler.api.Predictor;
import at.ac.uibk.scheduler.api.Scheduler;
import at.ac.uibk.scheduler.api.SchedulingAlgorithm;
import at.ac.uibk.util.StreamingLambda;
import com.amazonaws.services.lambda.runtime.Context;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

public class ScheduleRequestHandler implements StreamingLambda<SchedulerRequestInput, Workflow> {

    @Override
    public Class<SchedulerRequestInput> getDeserializationClass() {
        return SchedulerRequestInput.class;
    }

    @Override
    public Optional<Workflow> handleRequest(final Optional<SchedulerRequestInput> input, final Context context) throws Exception {

        if (input.isEmpty()) {
            return Optional.empty();
        }

        final Class<? extends SchedulingAlgorithm> algorithmClazz =
                input.map(SchedulerRequestInput::getHeaders).map(h -> h.get("algorithm"))
                        .flatMap(algoName -> AvailableImplementationsRequestHandler.getAvailableAlgorithms().stream().filter(clazz -> clazz.getSimpleName().equals(algoName)).findFirst())
                        .orElseThrow();

        final int predictorValue = input.map(SchedulerRequestInput::getHeaders)
                .map(StreamingLambda::keysToLowerCase)
                .map(h -> h.get("predictor"))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .orElse(1);

        final SchedulingAlgorithm algorithm = algorithmClazz.getDeclaredConstructor().newInstance();

        return Optional.of(Scheduler.schedule(input.get().getBody(), () -> algorithm, new Predictor() {
            @Override
            public List<Integer> predict(ParallelFor parallelFor) {
                return List.of(predictorValue);
            }

            @Override
            public int predict(SequentialFor sequentialFor) {
                return predictorValue;
            }

            @Override
            public int predict(Condition condition) {
                return predictorValue;
            }
        }));
    }
}
