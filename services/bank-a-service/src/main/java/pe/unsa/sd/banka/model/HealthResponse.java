package pe.unsa.sd.banka.model;

public class HealthResponse {

    private String service;
    private String bankCode;
    private String status;
    private int port;

    public HealthResponse() {
    }

    public HealthResponse(String service, String bankCode, String status, int port) {
        this.service = service;
        this.bankCode = bankCode;
        this.status = status;
        this.port = port;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
