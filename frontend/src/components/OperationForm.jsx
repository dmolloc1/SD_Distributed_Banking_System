function OperationForm({ accounts, accessBank, loading, onSubmit }) {
  const hasAccounts = accounts.length > 0;

  return (
    <form className="operation-form" onSubmit={onSubmit}>
      <h2>Operaciones</h2>
      <div className="form-grid">
        <label>
          Tipo
          <select name="operationType" disabled={!hasAccounts || loading}>
            <option value="deposit">Deposito</option>
            <option value="withdraw">Retiro</option>
            <option value="transfer">Transferencia</option>
          </select>
        </label>

        <label>
          Cuenta origen
          <select name="sourceAccountId" disabled={!hasAccounts || loading}>
            {accounts.map((account) => (
              <option key={account.accountId} value={account.accountId}>
                {account.accountId} - {account.bankCode}
              </option>
            ))}
          </select>
        </label>

        <label>
          Cuenta destino
          <input name="targetAccountId" type="text" placeholder="A-1001" disabled={!hasAccounts || loading} />
        </label>

        <label>
          Monto
          <input name="amount" type="number" min="0.01" step="0.01" placeholder="100.00" disabled={!hasAccounts || loading} />
        </label>

        <input name="accessBank" type="hidden" value={accessBank} />

        <button type="submit" disabled={!hasAccounts || loading}>
          {loading ? 'Enviando...' : 'Ejecutar operacion'}
        </button>
      </div>
    </form>
  );
}

export default OperationForm;
