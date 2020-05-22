package org.kie.kogito.process;

public class ProcessInstanceMarshallingException extends RuntimeException {

    private static final long serialVersionUID = -707257541887233373L;
    private final String processInstanceId;

    public ProcessInstanceMarshallingException(String processInstanceId, Throwable cause, String msg) {
        super(msg + processInstanceId, cause);
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

}
