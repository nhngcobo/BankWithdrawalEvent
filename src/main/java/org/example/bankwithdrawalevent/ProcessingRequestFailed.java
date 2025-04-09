package org.example.bankwithdrawalevent;

public class ProcessingRequestFailed extends RuntimeException {
    public ProcessingRequestFailed(String message) {
        super(message);
    }
}
