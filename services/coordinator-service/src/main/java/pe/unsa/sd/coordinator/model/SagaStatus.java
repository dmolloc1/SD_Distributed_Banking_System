package pe.unsa.sd.coordinator.model;

public enum SagaStatus {
    PENDING,
    EXECUTING_STEP_1,
    EXECUTING_STEP_2,
    COMMITTED,
    COMPENSATING,
    ABORTED
}
