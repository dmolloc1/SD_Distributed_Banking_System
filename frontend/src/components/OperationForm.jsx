import { useState, useEffect } from 'react';

function OperationForm({ accounts, accessBank, loading, onSubmit }) {
  const hasAccounts = accounts && accounts.length > 0;

  const [operationType, setOperationType] = useState('deposit');
  const [transferMode, setTransferMode] = useState('own'); // 'own', 'third', 'interbank'
  const [selectedSourceId, setSelectedSourceId] = useState('');
  const [selectedDestBank, setSelectedDestBank] = useState('');
  const [selectedDestId, setSelectedDestId] = useState('');

  // Local accounts (reside in the current accessBank)
  const localAccounts = accounts ? accounts.filter(a => a.bankCode === accessBank) : [];

  // External accounts (reside in other banks)
  const externalAccounts = accounts ? accounts.filter(a => a.bankCode !== accessBank) : [];

  // Available external banks where the client actually has accounts
  const externalBanks = [...new Set(externalAccounts.map(a => a.bankCode))];

  // Sync selected values when accounts or accessBank change
  useEffect(() => {
    if (localAccounts.length > 0) {
      setSelectedSourceId(prevSource => {
        const exists = localAccounts.some(a => a.accountId === prevSource);
        return exists ? prevSource : localAccounts[0].accountId;
      });
    } else {
      setSelectedSourceId('');
    }

    if (externalAccounts.length > 0) {
      setSelectedDestBank(prevBank => {
        const exists = externalBanks.includes(prevBank);
        return exists ? prevBank : externalBanks[0];
      });
    } else {
      setSelectedDestBank('');
      setSelectedDestId('');
    }
  }, [accounts, accessBank]);

  // Sync destination account ID when destination bank changes
  useEffect(() => {
    if (selectedDestBank) {
      const bankAccs = externalAccounts.filter(a => a.bankCode === selectedDestBank);
      if (bankAccs.length > 0) {
        setSelectedDestId(prevDest => {
          const exists = bankAccs.some(a => a.accountId === prevDest);
          return exists ? prevDest : bankAccs[0].accountId;
        });
      } else {
        setSelectedDestId('');
      }
    } else {
      setSelectedDestId('');
    }
  }, [selectedDestBank, accounts]);

  // Filtering local destination accounts for "own" transfer (must not be the source account)
  const ownDestAccounts = localAccounts.filter(a => a.accountId !== selectedSourceId);

  // Set selected dest account for "own" mode automatically when source account or local accounts change
  useEffect(() => {
    if (ownDestAccounts.length > 0) {
      setSelectedDestId(prevOwnDest => {
        const exists = ownDestAccounts.some(a => a.accountId === prevOwnDest);
        return exists ? prevOwnDest : ownDestAccounts[0].accountId;
      });
    } else if (transferMode === 'own') {
      setSelectedDestId('');
    }
  }, [selectedSourceId, accounts, transferMode]);

  // Validation flags
  const isOwnTransferDisabled = operationType === 'transfer' && transferMode === 'own' && localAccounts.length <= 1;
  const isInterbankDisabled = operationType === 'transfer' && transferMode === 'interbank' && externalAccounts.length === 0;
  const isFormDisabled = !hasAccounts || loading || isOwnTransferDisabled || isInterbankDisabled;

  return (
    <form className="operations-panel" onSubmit={onSubmit}>
      <div className="panel-title-group">
        <span className="panel-subtitle">Transacciones</span>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Ejecutar Operación Bancaria
        </h2>
      </div>

      {/* 3 Tab Selector buttons shown only when operationType is 'transfer' */}
      {operationType === 'transfer' && (
        <div className="transfer-tabs-container">
          <button 
            type="button" 
            className={`transfer-tab-btn ${transferMode === 'own' ? 'active' : ''}`}
            onClick={() => setTransferMode('own')}
            disabled={loading}
          >
            Entre mis cuentas
          </button>
          <button 
            type="button" 
            className={`transfer-tab-btn ${transferMode === 'third' ? 'active' : ''}`}
            onClick={() => setTransferMode('third')}
            disabled={loading}
          >
            Con nro. de cuenta
          </button>
          <button 
            type="button" 
            className={`transfer-tab-btn ${transferMode === 'interbank' ? 'active' : ''}`}
            onClick={() => setTransferMode('interbank')}
            disabled={loading}
          >
            Interbancaria
          </button>
        </div>
      )}

      <div className="form-inputs-row">
        
        {/* Left Column: Operation Type and Source Account */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          
          <div className="input-group">
            <span className="input-label">Tipo de Operación</span>
            <div className="select-wrapper">
              <select 
                name="operationType" 
                className="form-select" 
                value={operationType}
                onChange={(e) => setOperationType(e.target.value)}
                disabled={!hasAccounts || loading}
              >
                <option value="deposit">Depósito (Deposit)</option>
                <option value="withdraw">Retiro (Withdraw)</option>
                <option value="transfer">Transferencia (Transfer)</option>
              </select>
            </div>
          </div>

          {operationType !== 'deposit' && (
            <div className="input-group">
              <span className="input-label">Cuenta de Origen (Source)</span>
              <div className="select-wrapper">
                <select 
                  name="sourceAccountId" 
                  className="form-select" 
                  value={selectedSourceId}
                  onChange={(e) => setSelectedSourceId(e.target.value)}
                  disabled={!hasAccounts || loading || localAccounts.length === 0}
                >
                  {localAccounts.map((account) => (
                    <option key={account.accountId} value={account.accountId}>
                      {account.accountId} - {account.bankCode} (${Number(account.balance).toFixed(2)} {account.currency})
                    </option>
                  ))}
                  {localAccounts.length === 0 && (
                    <option value="">No posee cuentas locales</option>
                  )}
                </select>
              </div>
            </div>
          )}

          {operationType === 'deposit' && (
            <div className="input-group">
              <span className="input-label">Cuenta a Depositar</span>
              <div className="select-wrapper">
                <select 
                  name="targetAccountId" 
                  className="form-select"
                  disabled={!hasAccounts || loading}
                >
                  {localAccounts.map((account) => (
                    <option key={account.accountId} value={account.accountId}>
                      {account.accountId} - {account.bankCode} (${Number(account.balance).toFixed(2)} {account.currency})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}

        </div>

        {/* Right Column: Dynamic Destination & Amount */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          
          {operationType === 'withdraw' && (
            <div className="input-group">
              <span className="input-label">Monto a Retirar (Withdraw)</span>
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
          )}

          {operationType === 'deposit' && (
            <div className="input-group">
              <span className="input-label">Monto a Depositar (Deposit)</span>
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
          )}

          {operationType === 'transfer' && (
            <>
              {/* Transfer mode: Own accounts */}
              {transferMode === 'own' && (
                <div className="input-group">
                  <span className="input-label">Cuenta de Destino (Propia)</span>
                  <div className="select-wrapper">
                    <select 
                      name="targetAccountId" 
                      className="form-select"
                      value={selectedDestId}
                      onChange={(e) => setSelectedDestId(e.target.value)}
                      disabled={isFormDisabled || ownDestAccounts.length === 0}
                    >
                      {ownDestAccounts.map((account) => (
                        <option key={account.accountId} value={account.accountId}>
                          {account.accountId} - {account.bankCode} (${Number(account.balance).toFixed(2)} {account.currency})
                        </option>
                      ))}
                      {ownDestAccounts.length === 0 && (
                        <option value="">No hay otras cuentas locales propias</option>
                      )}
                    </select>
                  </div>
                </div>
              )}

              {/* Transfer mode: Local Third-party (text input) */}
              {transferMode === 'third' && (
                <div className="input-group">
                  <span className="input-label">Nro. de Cuenta Destino (Terceros Local)</span>
                  <div className="input-wrapper">
                    <input 
                      name="targetAccountId" 
                      className="form-input" 
                      type="text" 
                      placeholder="Ej: A-1002" 
                      disabled={isFormDisabled} 
                      required 
                    />
                  </div>
                </div>
              )}

              {/* Transfer mode: Interbank */}
              {transferMode === 'interbank' && (
                <>
                  {!isInterbankDisabled ? (
                    <>
                      <div className="input-group">
                        <span className="input-label">Banco de Destino</span>
                        <div className="select-wrapper">
                          <select 
                            className="form-select"
                            value={selectedDestBank}
                            onChange={(e) => setSelectedDestBank(e.target.value)}
                            disabled={isFormDisabled}
                          >
                            {externalBanks.map((bank) => (
                              <option key={bank} value={bank}>{bank}</option>
                            ))}
                          </select>
                        </div>
                      </div>

                      <div className="input-group" style={{ marginTop: '12px' }}>
                        <span className="input-label">Cuenta de Destino (Interbancaria)</span>
                        <div className="select-wrapper">
                          <select 
                            name="targetAccountId" 
                            className="form-select"
                            value={selectedDestId}
                            onChange={(e) => setSelectedDestId(e.target.value)}
                            disabled={isFormDisabled || !selectedDestBank}
                          >
                            {externalAccounts.filter(a => a.bankCode === selectedDestBank).map((account) => (
                              <option key={account.accountId} value={account.accountId}>
                                {account.accountId} - {account.bankCode} (${Number(account.balance).toFixed(2)} {account.currency})
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>
                    </>
                  ) : (
                    <div className="input-group">
                      <span className="input-label">Cuenta de Destino (Interbancaria)</span>
                      <div className="select-wrapper">
                        <select className="form-select" disabled>
                          <option>Sin cuentas externas</option>
                        </select>
                      </div>
                    </div>
                  )}
                </>
              )}

              <div className="input-group" style={{ marginTop: '12px' }}>
                <span className="input-label">Monto (Amount)</span>
                <div className="input-wrapper">
                  <input 
                    name="amount" 
                    className="form-input" 
                    type="number" 
                    min="0.01" 
                    step="0.01" 
                    placeholder="100.00" 
                    disabled={isFormDisabled} 
                    required 
                  />
                </div>
              </div>
            </>
          )}

        </div>

      </div>

      {/* Validation warning alerts */}
      {operationType === 'transfer' && (
        <div style={{ marginTop: '16px' }}>
          {isOwnTransferDisabled && (
            <div className="validation-alert-danger">
              <svg viewBox="0 0 20 20" fill="currentColor" style={{ width: '20px', height: '20px', flexShrink: 0 }}>
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              <span>No posee más cuentas locales en este banco para transferirse a sí mismo.</span>
            </div>
          )}
          {isInterbankDisabled && (
            <div className="validation-alert-danger">
              <svg viewBox="0 0 20 20" fill="currentColor" style={{ width: '20px', height: '20px', flexShrink: 0 }}>
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              <span>Este cliente no posee cuentas configuradas en otros nodos bancarios de la red distribuida.</span>
            </div>
          )}
        </div>
      )}

      {/* Hidden input to pass accessBank to onSubmit */}
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
          disabled={isFormDisabled}
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
