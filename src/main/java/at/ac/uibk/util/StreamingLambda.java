package at.ac.uibk.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public interface StreamingLambda<I, O> extends RequestStreamHandler {

    default void log(final String message, final Context context) {
        if (context != null) {
            context.getLogger().log(message);
        } else {
            System.out.println(message);
        }
    }

    static Map<String, String> keysToLowerCase(final Map<String, String> initial) {
        final TreeMap<String, String> reorderd = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        reorderd.putAll(initial);
        return reorderd;
    }

    default void log(final Throwable throwable, final Context context) {
        this.log(throwable.getMessage(), context);
    }

    @Override
    default void handleRequest(final InputStream input, final OutputStream output, final Context context) throws IOException {

        final ObjectMapper mapper = ObjectMapperUtil.JSON_MAPPER;

        final Optional<I> inputObject = Optional.ofNullable(input).map(i -> {
            try {
                return mapper.readValue(i, this.getDeserializationClass());
            } catch (final IOException e) {
                this.log(e, context);
                return null;
            }
        });

        final Optional<O> result;
        try {
            result = this.handleRequest(inputObject, context);
        } catch (final Exception e) {
            this.log(e, context);
            throw new LambdaException(e);
        }

        result.ifPresent(r -> {
            try {
                mapper.writeValue(output, r);
            } catch (final IOException e) {
                this.log(e, context);
            }
        });
    }

    Class<I> getDeserializationClass();

    Optional<O> handleRequest(Optional<I> input, Context context) throws Exception;
}
