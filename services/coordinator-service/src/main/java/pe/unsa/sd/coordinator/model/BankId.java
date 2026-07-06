package pe.unsa.sd.coordinator.model;

public enum BankId {
    BANK_A,
    BANK_B,
    BANK_C;

    public static BankId fromText(String value) {
        return BankId.valueOf(value);
    }
}
