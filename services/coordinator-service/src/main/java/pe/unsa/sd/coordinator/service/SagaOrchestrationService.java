package pe.unsa.sd.coordinator.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pe.unsa.sd.coordinator.dto.BankOperationRequest;
import pe.unsa.sd.coordinator.dto.CompensationRequest;
import pe.unsa.sd.coordinator.dto.SagaTransactionDTO;
import pe.unsa.sd.coordinator.dto.TransferRequest;
import pe.unsa.sd.coordinator.model.BankId;
import pe.unsa.sd.coordinator.model.OperationType;
import pe.unsa.sd.coordinator.model.SagaStatus;
import pe.unsa.sd.coordinator.model.SagaStep;
import pe.unsa.sd.coordinator.model.SagaStepStatus;
import pe.unsa.sd.coordinator.model.SagaTransaction;
// Contiene la lógica de la Saga: ejecutar pasos, manejar errores, lanzar compensaciones.
@Service
public class SagaOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrationService.class);

    private final RestTemplate restTemplate;
    private final String bankAUrl;
    private final String bankBUrl;
    private final String bankCUrl;

    public SagaOrchestrationService(
            RestTemplate restTemplate,
            @Value("${banks.bank-a-url}") String bankAUrl,
            @Value("${banks.bank-b-url}") String bankBUrl,
            @Value("${banks.bank-c-url}") String bankCUrl) {
        this.restTemplate = restTemplate;
        this.bankAUrl = bankAUrl;
        this.bankBUrl = bankBUrl;
        this.bankCUrl = bankCUrl;
    }

    public SagaTransactionDTO executeSaga(TransferRequest request) {
        SagaTransaction saga = buildSaga(request);

        log.info("SAGA INICIADA ___________________txId={}", saga.getTransactionId());

        saga.setStatus(SagaStatus.EXECUTING_STEP_1);
        SagaStep step1 = saga.getSteps().get(0);
        executeStep(saga.getTransactionId(), step1, request.getCurrency());
        if (step1.getStepStatus() != SagaStepStatus.SUCCESS) {
            saga.setStatus(SagaStatus.ABORTED);
            saga.setError("STEP_1_FAILED");
            saga.setCompletedAt(OffsetDateTime.now());
            log.info("SAGA ABORTED ______________ txId={} reason=STEP_1_FAILED", saga.getTransactionId());
            return toDto(saga);
        }

        saga.setStatus(SagaStatus.EXECUTING_STEP_2);
        SagaStep step2 = saga.getSteps().get(1);
        executeStep(saga.getTransactionId(), step2, request.getCurrency());

        if (step2.getStepStatus() != SagaStepStatus.SUCCESS) {
            log.warn("Starting compensation txId={}", saga.getTransactionId());
            executeCompensation(saga, 2, request.getCurrency());
            saga.setStatus(SagaStatus.ABORTED);
            saga.setError("STEP_2_FAILED");
            saga.setCompletedAt(OffsetDateTime.now());
            log.info("=== SAGA ABORTED === txId={} reason=STEP_2_FAILED", saga.getTransactionId());
            return toDto(saga);
        }

        saga.setStatus(SagaStatus.COMMITTED);
        saga.setCompletedAt(OffsetDateTime.now());
        log.info("_________SAGA COMMITTED ________txId={}", saga.getTransactionId());
        return toDto(saga);
    }

    private SagaTransaction buildSaga(TransferRequest request) {
        SagaTransaction saga = new SagaTransaction();
        saga.setTransactionId(UUID.randomUUID().toString());
        saga.setSourceAccountId(request.getOriginAccountId());
        saga.setSourceBankId(BankId.fromText(request.getOriginBankId()));
        saga.setDestinationAccountId(request.getDestinationAccountId());
        saga.setDestinationBankId(BankId.fromText(request.getDestinationBankId()));
        saga.setAmount(request.getAmount());
        saga.setStatus(SagaStatus.PENDING);
        saga.setInitiatedAt(OffsetDateTime.now());

        List<SagaStep> steps = new ArrayList<>();
        steps.add(newStep(1, saga.getSourceBankId(), saga.getSourceAccountId(), OperationType.DEBIT, saga.getAmount()));
        steps.add(newStep(2, saga.getDestinationBankId(), saga.getDestinationAccountId(), OperationType.CREDIT, saga.getAmount()));
        saga.setSteps(steps);
        return saga;
    }

    private SagaStep newStep(int number, BankId bankId, String accountId, OperationType operationType, BigDecimal amount) {
        SagaStep step = new SagaStep();
        step.setStepId(UUID.randomUUID().toString());
        step.setStepNumber(number);
        step.setBankId(bankId);
        step.setAccountId(accountId);
        step.setOperationType(operationType);
        step.setAmount(amount);
        step.setStepStatus(SagaStepStatus.PENDING);
        step.setCompensationApplied(false);
        return step;
    }

    private void executeStep(String transactionId, SagaStep step, String currency) {
        String endpoint = getBankUrl(step.getBankId())
                + "/api/v1/bank/accounts/"
                + step.getAccountId()
                + "/"
                + step.getOperationType().name().toLowerCase();

        step.setStepStatus(SagaStepStatus.EXECUTING);
        log.info(
                "[STEP {}/2] Executing {} on {} account={} amount={}",
                step.getStepNumber(),
                step.getOperationType(),
                step.getBankId(),
                step.getAccountId(),
                step.getAmount());

        BankOperationRequest operationRequest = new BankOperationRequest();
        operationRequest.setTransactionId(transactionId);
        operationRequest.setAmount(step.getAmount());
        operationRequest.setCurrency(currency);
        try {
            restTemplate.postForEntity(endpoint, operationRequest, Void.class);
            step.setStepStatus(SagaStepStatus.SUCCESS);
            step.setExecutedAt(OffsetDateTime.now());
            log.info("STEP {} SUCCESS", step.getStepNumber());
        } catch (RestClientException exception) {
            step.setStepStatus(SagaStepStatus.FAILED);
            step.setExecutedAt(OffsetDateTime.now());
            step.setErrorMessage(exception.getMessage());
            log.error("STEP {} FAILED: {}", step.getStepNumber(), exception.getMessage());
        }
    }

    private void executeCompensation(SagaTransaction saga, int failedAtStep, String currency) {
        saga.setStatus(SagaStatus.COMPENSATING);
        for (int i = failedAtStep - 1; i >= 1; i--) {
            SagaStep original = saga.getSteps().get(i - 1);
            if (original.getStepStatus() != SagaStepStatus.SUCCESS) {
                continue;
            }

            OperationType inverseOperation = original.getOperationType() == OperationType.DEBIT
                    ? OperationType.CREDIT
                    : OperationType.DEBIT;
            String endpoint = getBankUrl(original.getBankId())
                    + "/api/v1/bank/accounts/"
                    + original.getAccountId()
                    + "/"
                    + inverseOperation.name().toLowerCase();

            CompensationRequest compensationRequest = new CompensationRequest();
            compensationRequest.setTransactionId(saga.getTransactionId());
            compensationRequest.setOriginalOperation(original.getOperationType().name());
            compensationRequest.setBankId(original.getBankId().name());
            compensationRequest.setAccountId(original.getAccountId());
            compensationRequest.setAmount(original.getAmount());
            compensationRequest.setCurrency(currency);
            compensationRequest.setReason("STEP_" + failedAtStep + "_FAILED");

            try {
                log.info(
                        "[COMPENSATION] {} on {} account={} amount={}",
                        inverseOperation,
                        original.getBankId(),
                        original.getAccountId(),
                        original.getAmount());
                restTemplate.postForEntity(endpoint, compensationRequest, Void.class);
                original.setStepStatus(SagaStepStatus.COMPENSATED);
                original.setCompensationApplied(true);
                original.setCompensationTimestamp(OffsetDateTime.now());
            } catch (RestClientException exception) {
                original.setErrorMessage("COMPENSATION_FAILED: " + exception.getMessage());
                log.error(
                        "Critical compensation failure txId={} step={} reason={}",
                        saga.getTransactionId(),
                        original.getStepNumber(),
                        exception.getMessage());
            }
        }
    }

    private String getBankUrl(BankId bankId) {
        return switch (bankId) {
            case BANK_A -> bankAUrl;
            case BANK_B -> bankBUrl;
            case BANK_C -> bankCUrl;
        };
    }

    private SagaTransactionDTO toDto(SagaTransaction saga) {
        SagaTransactionDTO dto = new SagaTransactionDTO();
        dto.setTransactionId(saga.getTransactionId());
        dto.setSourceAccountId(saga.getSourceAccountId());
        dto.setSourceBankId(saga.getSourceBankId().name());
        dto.setDestinationAccountId(saga.getDestinationAccountId());
        dto.setDestinationBankId(saga.getDestinationBankId().name());
        dto.setAmount(saga.getAmount());
        dto.setStatus(saga.getStatus().name());
        dto.setInitiatedAt(saga.getInitiatedAt());
        dto.setCompletedAt(saga.getCompletedAt());
        dto.setError(saga.getError());

        List<SagaTransactionDTO.SagaStepDTO> stepDtos = new ArrayList<>();
        for (SagaStep step : saga.getSteps()) {
            SagaTransactionDTO.SagaStepDTO stepDto = new SagaTransactionDTO.SagaStepDTO();
            stepDto.setStepId(step.getStepId());
            stepDto.setStepNumber(step.getStepNumber());
            stepDto.setBankId(step.getBankId().name());
            stepDto.setAccountId(step.getAccountId());
            stepDto.setOperationType(step.getOperationType().name());
            stepDto.setAmount(step.getAmount());
            stepDto.setStepStatus(step.getStepStatus().name());
            stepDto.setExecutedAt(step.getExecutedAt());
            stepDto.setErrorMessage(step.getErrorMessage());
            stepDto.setCompensationApplied(step.isCompensationApplied());
            stepDto.setCompensationTimestamp(step.getCompensationTimestamp());
            stepDtos.add(stepDto);
        }
        dto.setSteps(stepDtos);
        return dto;
    }
}
