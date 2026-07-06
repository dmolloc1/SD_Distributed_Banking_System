package pe.unsa.sd.gateway.grpcserver;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.unsa.sd.gateway.controller.GatewayOperationController;
import pe.unsa.sd.gateway.grpc.Account;
import pe.unsa.sd.gateway.grpc.BankingGatewayGrpc;
import pe.unsa.sd.gateway.grpc.CustomerAccountsRequest;
import pe.unsa.sd.gateway.grpc.CustomerAccountsResponse;
import pe.unsa.sd.gateway.grpc.OperationRequest;
import pe.unsa.sd.gateway.grpc.OperationResponse;
import pe.unsa.sd.gateway.grpc.SagaStep;
import pe.unsa.sd.gateway.grpc.TransactionStatusRequest;
import pe.unsa.sd.gateway.grpc.TransactionStatusResponse;
import pe.unsa.sd.gateway.grpc.TransferRequest;
import pe.unsa.sd.gateway.grpc.TransferResponse;

@Service
public class BankingGatewayGrpcService extends BankingGatewayGrpc.BankingGatewayImplBase {

    private final GatewayOperationController gatewayOperationController;

    public BankingGatewayGrpcService(GatewayOperationController gatewayOperationController) {
        this.gatewayOperationController = gatewayOperationController;
    }

    @Override
    public void getCustomerAccounts(
            CustomerAccountsRequest request,
            StreamObserver<CustomerAccountsResponse> responseObserver) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = gatewayOperationController
                    .getCustomerAccounts(request.getCustomerId())
                    .block();
            CustomerAccountsResponse.Builder builder = CustomerAccountsResponse.newBuilder()
                    .setHttpStatus(statusCode(response));

            if (response != null && response.getBody() != null) {
                response.getBody().stream()
                        .map(this::toAccount)
                        .forEach(builder::addAccounts);
            }

            complete(responseObserver, builder.build());
        } catch (Exception exception) {
            fail(responseObserver, exception);
        }
    }

    @Override
    public void deposit(OperationRequest request, StreamObserver<OperationResponse> responseObserver) {
        try {
            ResponseEntity<Map<String, Object>> response = gatewayOperationController
                    .deposit(Map.of(
                            "targetAccountId", request.getAccountId(),
                            "amount", request.getAmount()))
                    .block();
            complete(responseObserver, toOperationResponse(response));
        } catch (Exception exception) {
            fail(responseObserver, exception);
        }
    }

    @Override
    public void withdraw(OperationRequest request, StreamObserver<OperationResponse> responseObserver) {
        try {
            ResponseEntity<Map<String, Object>> response = gatewayOperationController
                    .withdraw(Map.of(
                            "sourceAccountId", request.getAccountId(),
                            "amount", request.getAmount()))
                    .block();
            complete(responseObserver, toOperationResponse(response));
        } catch (Exception exception) {
            fail(responseObserver, exception);
        }
    }

    @Override
    public void transfer(TransferRequest request, StreamObserver<TransferResponse> responseObserver) {
        try {
            ResponseEntity<Map<String, Object>> response = gatewayOperationController
                    .transfer(Map.of(
                            "sourceAccountId", request.getSourceAccountId(),
                            "targetAccountId", request.getTargetAccountId(),
                            "amount", request.getAmount()))
                    .block();
            complete(responseObserver, toTransferResponse(response));
        } catch (Exception exception) {
            fail(responseObserver, exception);
        }
    }

    @Override
    public void getTransactionStatus(
            TransactionStatusRequest request,
            StreamObserver<TransactionStatusResponse> responseObserver) {
        try {
            ResponseEntity<Map<String, Object>> response = gatewayOperationController
                    .getTransactionStatus(request.getTransactionId())
                    .block();
            complete(responseObserver, toTransactionStatusResponse(response));
        } catch (Exception exception) {
            fail(responseObserver, exception);
        }
    }

    private Account toAccount(Map<String, Object> account) {
        return Account.newBuilder()
                .setAccountId(asString(account.get("accountId")))
                .setClientId(asString(account.get("clientId")))
                .setBankCode(asString(account.get("bankCode")))
                .setType(asString(account.get("type")))
                .setCurrency(asString(account.get("currency")))
                .setBalance(asString(account.get("balance")))
                .build();
    }

    private OperationResponse toOperationResponse(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = body(response);
        return OperationResponse.newBuilder()
                .setStatus(asString(body.get("status")))
                .setAccountId(asString(body.get("accountId")))
                .setBalance(asString(body.get("balance")))
                .setTransactionId(asString(body.get("transactionId")))
                .setError(asString(body.get("error")))
                .setMessage(asString(body.get("message")))
                .setHttpStatus(statusCode(response))
                .build();
    }

    private TransferResponse toTransferResponse(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = body(response);
        return TransferResponse.newBuilder()
                .setTransactionId(asString(body.get("transactionId")))
                .setStatus(asString(body.get("status")))
                .setMessage(asString(body.get("message")))
                .setError(asString(body.get("error")))
                .setHttpStatus(statusCode(response))
                .build();
    }

    private TransactionStatusResponse toTransactionStatusResponse(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = body(response);
        TransactionStatusResponse.Builder builder = TransactionStatusResponse.newBuilder()
                .setTransactionId(asString(body.get("transactionId")))
                .setStatus(asString(body.get("status")))
                .setMessage(asString(body.get("message")))
                .setError(asString(body.get("error")))
                .setSourceAccountId(asString(body.get("sourceAccountId")))
                .setSourceBankId(asString(body.get("sourceBankId")))
                .setDestinationAccountId(asString(body.get("destinationAccountId")))
                .setDestinationBankId(asString(body.get("destinationBankId")))
                .setAmount(asString(body.get("amount")))
                .setCurrentStep(asInt(body.get("currentStep")))
                .setTotalSteps(asInt(body.get("totalSteps")))
                .setInitiatedAt(asString(body.get("initiatedAt")))
                .setCompletedAt(asString(body.get("completedAt")))
                .setHttpStatus(statusCode(response));

        Object steps = body.get("steps");
        if (steps instanceof List<?> stepList) {
            stepList.stream()
                    .filter(Map.class::isInstance)
                    .map(step -> toSagaStep((Map<?, ?>) step))
                    .forEach(builder::addSteps);
        }
        return builder.build();
    }

    private SagaStep toSagaStep(Map<?, ?> step) {
        return SagaStep.newBuilder()
                .setStepId(asString(step.get("stepId")))
                .setStepNumber(asInt(step.get("stepNumber")))
                .setBankId(asString(step.get("bankId")))
                .setAccountId(asString(step.get("accountId")))
                .setOperationType(asString(step.get("operationType")))
                .setAmount(asString(step.get("amount")))
                .setStepStatus(asString(step.get("stepStatus")))
                .setExecutedAt(asString(step.get("executedAt")))
                .setErrorMessage(asString(step.get("errorMessage")))
                .setCompensationApplied(asBoolean(step.get("compensationApplied")))
                .setCompensationTimestamp(asString(step.get("compensationTimestamp")))
                .build();
    }

    private Map<String, Object> body(ResponseEntity<Map<String, Object>> response) {
        if (response == null || response.getBody() == null) {
            return Map.of();
        }
        return response.getBody();
    }

    private int statusCode(ResponseEntity<?> response) {
        if (response == null) {
            return 500;
        }
        return response.getStatusCode().value();
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private <T> void complete(StreamObserver<T> responseObserver, T response) {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void fail(StreamObserver<?> responseObserver, Exception exception) {
        responseObserver.onError(Status.INTERNAL
                .withDescription(exception.getMessage())
                .withCause(exception)
                .asRuntimeException());
    }
}
