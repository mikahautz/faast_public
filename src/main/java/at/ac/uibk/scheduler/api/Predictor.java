package at.ac.uibk.scheduler.api;

import at.ac.uibk.core.functions.ParallelFor;
import at.ac.uibk.core.functions.SequentialFor;
import at.ac.uibk.core.functions.objects.Condition;

import java.util.List;

public interface Predictor {

    int predict(Condition condition);

    List<Integer> predict(ParallelFor parallelFor);

    int predict(SequentialFor sequentialFor);

}
