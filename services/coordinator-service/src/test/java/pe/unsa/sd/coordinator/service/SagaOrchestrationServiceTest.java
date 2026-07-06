package pe.unsa.sd.coordinator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import pe.unsa.sd.coordinator.dto.SagaTransactionDTO;
import pe.unsa.sd.coordinator.dto.TransferRequest;

class SagaOrchestrationServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private SagaOrchestrationService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();
        service = new SagaOrchestrationService(
                restTemplate,
                "http://bank-a.test",
                "http://bank-b.test",
                "http://bank-c.test");
    }

    @Test
    void executeSagaCommitsWhenDebitAndCreditSucceed() {
        TransferRequest request = transfer("A-1001", "BANK_A", "B-2001", "BANK_B", "75.00");

        server.expect(requestTo("http://bank-a.test/api/v1/bank/accounts/A-1001/debit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.transactionId").value("tx-success"))
                .andExpect(jsonPath("$.amount").value(75.00))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://bank-b.test/api/v1/bank/accounts/B-2001/credit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.transactionId").value("tx-success"))
                .andExpect(jsonPath("$.amount").value(75.00))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        SagaTransactionDTO result = service.executeSaga(request, "tx-success");

        assertEquals("COMMITTED", result.getStatus());
        assertEquals("Transferencia completada exitosamente", result.getMessage());
        assertEquals(2, result.getSteps().size());
        assertTrue(result.getSteps().stream().allMatch(step -> "SUCCESS".equals(step.getStepStatus())));
        server.verify();
    }

    @Test
    void executeSagaCompensatesOriginWhenDestinationCreditFails() {
        TransferRequest request = transfer("A-1001", "BANK_A", "B-9999", "BANK_B", "50.00");

        server.expect(requestTo("http://bank-a.test/api/v1/bank/accounts/A-1001/debit"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://bank-b.test/api/v1/bank/accounts/B-9999/credit"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        server.expect(requestTo("http://bank-a.test/api/v1/bank/accounts/A-1001/credit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.reason").value("STEP_2_FAILED"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        SagaTransactionDTO result = service.executeSaga(request, "tx-compensated");

        assertEquals("ABORTED", result.getStatus());
        assertEquals("STEP_2_FAILED", result.getError());
        assertEquals("COMPENSATED", result.getSteps().get(0).getStepStatus());
        assertTrue(result.getSteps().get(0).isCompensationApplied());
        server.verify();
    }

    private TransferRequest transfer(
            String originAccount,
            String originBank,
            String destinationAccount,
            String destinationBank,
            String amount) {
        TransferRequest request = new TransferRequest();
        request.setOriginAccountId(originAccount);
        request.setOriginBankId(originBank);
        request.setDestinationAccountId(destinationAccount);
        request.setDestinationBankId(destinationBank);
        request.setAmount(new BigDecimal(amount));
        request.setCurrency("USD");
        return request;
    }
}
