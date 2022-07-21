package at.ac.uibk.scheduler.api;

import at.ac.uibk.core.functions.ParallelFor;
import at.ac.uibk.core.functions.SequentialFor;
import at.ac.uibk.core.functions.objects.Condition;
import at.ac.uibk.core.functions.objects.LoopCounter;

import java.util.List;

public class DefaultPredictor implements Predictor {

    @Override
    public List<Integer> predict(ParallelFor parallelFor) {
        final LoopCounter loopCounter = parallelFor.getLoopCounter();
        try {
            int from = Integer.parseInt(loopCounter.getFrom());
            int to = Integer.parseInt(loopCounter.getTo());
            int step = Integer.parseInt(loopCounter.getStep());

            return List.of((to - from) / step + (to - from) % step);

        } catch (final Exception e) {
            try {
                return List.of(Integer.parseInt((String) loopCounter.getAdditionalProperties().get("predictor")));
            } catch (final Exception e1) {
                // noop return value below
            }
            return List.of(1);
        }

    }

    @Override
    public int predict(SequentialFor sequentialFor) {
        final LoopCounter loopCounter = sequentialFor.getLoopCounter();
        try {
            int from = Integer.parseInt(loopCounter.getFrom());
            int to = Integer.parseInt(loopCounter.getTo());
            int step = Integer.parseInt(loopCounter.getStep());

            return (to - from) / step + (to - from) % step;

        } catch (final Exception e) {
            try {
                return Integer.parseInt((String) loopCounter.getAdditionalProperties().get("predictor"));
            } catch (final Exception e1) {
                // noop return value below
            }
            return 1;
        }
    }


    public int predict(Condition condition) {
        return 1;
    }

}
