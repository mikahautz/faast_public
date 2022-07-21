package at.ac.uibk;

import at.ac.uibk.core.Workflow;
import at.ac.uibk.scheduler.ScheduleRequestHandler;
import at.ac.uibk.scheduler.SchedulerRequestInput;
import at.ac.uibk.util.ObjectMapperUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;

/**
 * Creates a Scheduled workflow but not via a deployed lambda, but using a local configuration
 */
public class ScheduleInvoker {

    /**
     * Must correspond to the class name of any {@link at.ac.uibk.scheduler.api.SchedulingAlgorithm}
     */
    private static final String ALGORITHM = "FaaST";

    private static final int PREDICTOR_COUNT = 60;

    /**
     * Use {@link ObjectMapperUtil#YAML_MAPPER} if your input is in yaml
     * or {@link ObjectMapperUtil#JSON_MAPPER} if your input is json
     */
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperUtil.YAML_MAPPER;

    /**
     * Specify in and output files, will be AFCL and CFCL workflows
     */
    private static final Path INPUT = Path.of("BWAfaast4_afcl.yaml");
    private static final Path OUTPUT = Path.of("BWAfaast4_cfcl.yaml");

    public static void main(String[] args) throws Exception {

        try (final InputStream is = Files.newInputStream(INPUT);
             final OutputStream os = Files.newOutputStream(OUTPUT, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Workflow wf = OBJECT_MAPPER.readValue(is, Workflow.class);

            final SchedulerRequestInput input = new SchedulerRequestInput();

            input.setHeaders(Map.of("algorithm", ALGORITHM,
                    "predictor", String.valueOf(PREDICTOR_COUNT)));

            input.setBody(wf);

            wf = new ScheduleRequestHandler().handleRequest(Optional.of(input), null).orElseThrow();

            OBJECT_MAPPER.writeValue(os, wf);
        }
    }

}
