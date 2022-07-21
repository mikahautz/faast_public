package at.ac.uibk.scheduler;


import at.ac.uibk.core.Workflow;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulerRequestInput implements Serializable {

    private static final long serialVersionUID = 5467352961959631190L;

    private Workflow body;

    private Map<String, String> headers;

    private String method;

    public Workflow getBody() {
        return this.body;
    }

    public void setBody(final Workflow body) {
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }
}
