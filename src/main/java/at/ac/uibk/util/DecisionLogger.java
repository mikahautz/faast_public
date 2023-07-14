package at.ac.uibk.util;

import at.ac.uibk.scheduler.api.node.AtomicFunctionNode;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DecisionLogger {

    private Map<String, Pair<Double, Double>> logMap = new HashMap<>();

    private Logger logger;

    public DecisionLogger(Logger logger) {
        this.logger = logger;
    }

    public void saveEntry(String resourceUrl, final double est, final double eft) {
        this.logMap.put(resourceUrl, Pair.of(est, eft));
    }

    public Logger getLogger() {
        return logger;
    }

    public void log(String resourceUrl, AtomicFunctionNode node) {
        logger.info("---------------------------------------------------");
        logger.info(" ---- For function " + node.getAtomicFunction().getName() + " scheduling decision  was " + resourceUrl + " with following results: ");

        logMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().getValue()))
                .forEach(entry -> logger.info(entry.getKey() + ": EST " + entry.getValue().getLeft() + " - EFT " + entry.getValue().getRight()));

        logger.info("----------------------------------------------------");
    }


}
