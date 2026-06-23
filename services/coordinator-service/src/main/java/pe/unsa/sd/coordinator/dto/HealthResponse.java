package pe.unsa.sd.coordinator.dto;

public class HealthResponse {

    private String service;
    private String status;
    private int port;

    public HealthResponse() {
    }

    public HealthResponse(String service, String status, int port) {
        this.service = service;
        this.status = status;
        this.port = port;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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
