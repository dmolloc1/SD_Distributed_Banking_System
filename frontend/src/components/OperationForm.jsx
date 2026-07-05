function OperationForm({ accounts, accessBank, loading, onSubmit }) {
  const hasAccounts = accounts.length > 0;

  return (
    <form className="operations-panel" onSubmit={onSubmit}>
      <div className="panel-title-group">
        <span className="panel-subtitle">Transacciones</span>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Ejecutar Operación Bancaria
        </h2>
      </div>

      <div className="form-inputs-row">
        {/* Left Column: Operation Type and Source Account */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          
          <div className="input-group">
            <span className="input-label">Tipo de Operación</span>
            <div className="select-wrapper">
              <select 
                name="operationType" 
                className="form-select" 
                disabled={!hasAccounts || loading}
              >
                <option value="deposit">Depósito (Deposit)</option>
                <option value="withdraw">Retiro (Withdraw)</option>
                <option value="transfer">Transferencia (Transfer)</option>
              </select>
            </div>
          </div>

          <div className="input-group">
            <span className="input-label">Cuenta de Origen (Source)</span>
            <div className="select-wrapper">
              <select 
                name="sourceAccountId" 
                className="form-select" 
                disabled={!hasAccounts || loading}
              >
                {accounts.map((account) => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.accountId} - {account.bankCode} (${Number(account.balance).toFixed(2)} {account.currency})
                  </option>
                ))}
              </select>
            </div>
          </div>

        </div>

        {/* Right Column: Destination Account and Amount */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          
          <div className="input-group">
            <span className="input-label">Cuenta de Destino (Destination)</span>
            <div className="input-wrapper">
              <input 
                name="targetAccountId" 
                className="form-input" 
                type="text" 
                placeholder="Ej: A-1001" 
                disabled={!hasAccounts || loading} 
              />
            </div>
          </div>

          <div className="input-group">
            <span className="input-label">Monto (Amount)</span>
            <div className="input-wrapper">
              <input 
                name="amount" 
                className="form-input" 
                type="number" 
                min="0.01" 
                step="0.01" 
                placeholder="100.00" 
                disabled={!hasAccounts || loading} 
                required 
              />
            </div>
          </div>

        </div>

      </div>

      <input name="accessBank" type="hidden" value={accessBank} />

      <div className="form-actions-row">
        <div className="demo-notice">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: '16px', height: '16px' }}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          Las operaciones viajan de forma distribuida a través de API Gateway.
        </div>
        
        <button 
          className="action-submit-btn" 
          type="submit" 
          disabled={!hasAccounts || loading}
        >
          {loading ? (
            'PROCESANDO...'
          ) : (
            <>
              <svg style={{ width: '18px', height: '18px', fill: 'currentColor' }} viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
              </svg>
              EJECUTAR OPERACIÓN
            </>
          )}
        </button>
      </div>

    </form>
  );
}

export default OperationForm;
