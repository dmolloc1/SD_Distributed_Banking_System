import { useState, useEffect } from 'react';
import {
  getDistributedAccounts,
  getTransactionStatus,
  submitDeposit,
  submitTransfer,
  submitWithdraw,
} from '../api/coordinatorApi.js';
import AccountSearch from '../components/AccountSearch.jsx';
import AccountTable from '../components/AccountTable.jsx';
import OperationForm from '../components/OperationForm.jsx';
import TransactionStatus from '../components/TransactionStatus.jsx';

function HomePage() {
  const [accessBank, setAccessBank] = useState('BANK_A');
  const [clientId, setClientId] = useState('C005');
  const [accounts, setAccounts] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [operationLoading, setOperationLoading] = useState(false);
  const [operationStatus, setOperationStatus] = useState(null);
  const [error, setError] = useState('');
  const [pollingIntervalId, setPollingIntervalId] = useState(null);

  // UTC clock state
  const [timeStr, setTimeStr] = useState('');

  useEffect(() => {
    // Set digital UTC clock in real-time
    const updateTime = () => {
      const now = new Date();
      const utcStr = now.toISOString().slice(11, 19) + ' UTC';
      setTimeStr(utcStr);
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    return () => {
      if (pollingIntervalId) {
        clearInterval(pollingIntervalId);
      }
    };
  }, [pollingIntervalId]);

  async function handleSubmit(event) {
    event.preventDefault();
    const normalizedClientId = clientId.trim();

    if (!normalizedClientId) {
      setError('Ingrese un clientId.');
      setAccounts([]);
      setSearched(false);
      return;
    }

    setLoading(true);
    setError('');
    setOperationStatus(null);
    setSearched(true);

    try {
      const result = await getDistributedAccounts(normalizedClientId);
      setAccounts(result);
    } catch (requestError) {
      setAccounts([]);
      setError('No se pudo conectar con el API Gateway.');
    } finally {
      setLoading(false);
    }
  }

  async function handleOperationSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const operationType = formData.get('operationType');
    const amount = Number(formData.get('amount'));

    if (!amount || amount <= 0) {
      setOperationStatus({
        kind: 'error',
        message: 'Ingrese un monto valido para la operacion.',
      });
      return;
    }

    if (pollingIntervalId) {
      clearInterval(pollingIntervalId);
      setPollingIntervalId(null);
    }

    const payload = {
      accessBank,
      customerId: clientId.trim(),
      sourceAccountId: formData.get('sourceAccountId'),
      targetAccountId: formData.get('targetAccountId'),
      amount,
    };

    setOperationLoading(true);
    setOperationStatus({
      kind: 'info',
      message: 'Validando solicitud en API Gateway.',
    });

    let shouldKeepLoading = false;
    try {
      const response = await submitSelectedOperation(operationType, payload);

      
      if (operationType === 'transfer' && response.statusCode === 202) {
        const txId = response.data.transactionId;
        if (!txId) {
          throw new Error("No transactionId returned from transfer submission");
        }

        shouldKeepLoading = true;

        setOperationStatus({
          kind: 'processing',
          message: 'Procesando...',
          details: response.data,
        });

        const intervalId = setInterval(async () => {
          try {
            const pollRes = await fetch(`http://localhost:8080/api/transfers/${txId}`);
            const data = await pollRes.json();
            
            if (data.status === 'COMMITTED') {
              clearInterval(intervalId);
              setPollingIntervalId(null);
              setOperationLoading(false);
              setOperationStatus({
                kind: 'success',
                message: `Transferencia completada con exito. (Estado Saga: ${data.status})`,
                details: data,
              });
              const normalizedClientId = clientId.trim();
              if (normalizedClientId) {
                const result = await getDistributedAccounts(normalizedClientId);
                setAccounts(result);
              }
            } else if (data.status === 'COMPENSATED' || data.status === 'ABORTED' || data.status === 'FAILED') {
              clearInterval(intervalId);
              setPollingIntervalId(null);
              setOperationLoading(false);
              setOperationStatus({
                kind: 'error',
                message: `Transferencia fallida y compensada. (Estado Saga: ${data.status})`,
                details: data,
              });
              const normalizedClientId = clientId.trim();
              if (normalizedClientId) {
                const result = await getDistributedAccounts(normalizedClientId);
                setAccounts(result);
              }
            } else {
              setOperationStatus({
                kind: 'processing',
                message: `Procesando... (${data.message || data.status})`,
                details: data,
              });
            }
          } catch (pollErr) {
            console.error("Error polling transfer status:", pollErr);
          }
        }, 1500);

        setPollingIntervalId(intervalId);
        return;
      }

      setOperationStatus({
        kind: 'success',
        message: 'Operacion completada.',
        details: response.data,
      });

      if (response.ok) {
        const normalizedClientId = clientId.trim();
        if (normalizedClientId) {
          const result = await getDistributedAccounts(normalizedClientId);
          setAccounts(result);
        }
      }
    } catch (requestError) {
      setOperationStatus({
        kind: 'error',
        message: 'No se pudo enviar la operacion al API Gateway.',
      });
    } finally {
      if (!shouldKeepLoading) {
        setOperationLoading(false);
      }
    }
  }

  function submitSelectedOperation(operationType, payload) {
    if (operationType === 'deposit') {
      return submitDeposit(payload);
    }
    if (operationType === 'withdraw') {
      return submitWithdraw(payload);
    }
    return submitTransfer(payload);
  }

  async function pollTransferStatus(transactionId) {
    setOperationStatus({
      kind: 'pending',
      message: 'Transferencia iniciada. Consultando estado de la Saga...',
      details: { transactionId },
    });

    for (let attempt = 1; attempt <= 10; attempt += 1) {
      await new Promise((resolve) => setTimeout(resolve, 700));
      const statusResponse = await getTransactionStatus(transactionId);
      const transaction = statusResponse.data;
      const status = transaction?.status;

      if (status === 'COMMITTED') {
        setOperationStatus({
          kind: 'success',
          message: 'Transferencia completada por el coordinador.',
          details: transaction,
        });
        return;
      }

      if (status === 'ABORTED' || status === 'COMPENSATING') {
        setOperationStatus({
          kind: 'error',
          message: transaction?.message || 'La transferencia fue revertida por el coordinador.',
          details: transaction,
        });
        return;
      }

      setOperationStatus({
        kind: 'pending',
        message: transaction?.message || `Transferencia en proceso. Intento ${attempt}/10.`,
        details: transaction,
      });
    }

    setOperationStatus({
      kind: 'pending',
      message: 'La transferencia sigue en proceso. Consulte nuevamente el estado de la transaccion.',
      details: { transactionId },
    });
  }

  // Calculate top bank cards dynamically from real loaded accounts,
  // or show the mockup values from image.png as visual defaults on mount
  const hasAccounts = accounts && accounts.length > 0;
  const bankATotal = hasAccounts
    ? accounts.filter(a => a.bankCode === 'BANK_A').reduce((sum, a) => sum + Number(a.balance), 0)
    : 1300.00;
  const bankBTotal = hasAccounts
    ? accounts.filter(a => a.bankCode === 'BANK_B').reduce((sum, a) => sum + Number(a.balance), 0)
    : 450.50;
  const bankCTotal = hasAccounts
    ? accounts.filter(a => a.bankCode === 'BANK_C').reduce((sum, a) => sum + Number(a.balance), 0)
    : 2100.25;
  
  const totalLiquidValue = bankATotal + bankBTotal + bankCTotal;

  // Static mockup transactions table matching the screenshot row-for-row
  const mockTransactions = [
    { id: 'TX-7732-A', from: 'BANK_A', to: 'BANK_B', amount: 12000.00, timestamp: '2025-11-24 05:12:33', status: 'COMMITTED' },
    { id: 'TX-7714-C', from: 'BANK_C', to: 'BANK_A', amount: 1450.25, timestamp: '2025-11-24 08:45:01', status: 'COMMITTED' },
  ];

  return (
    <div className="app-container">
      
      {/* LEFT SIDEBAR */}
      <aside className="sidebar">
        <div>
          <div className="sidebar-logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <circle cx="12" cy="12" r="3" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 2v3m0 14v3M2 12h3m14 0h3m-3.5-6.5l-2 2m-7 7l-2 2m0-11l2 2m7 7l2-2" />
            </svg>
            <span>Ledger</span>
          </div>

          <nav className="sidebar-menu">
            <span className="menu-item active">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
              </svg>
              Dashboard
            </span>
            <span className="menu-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
              Accounts
            </span>
            <span className="menu-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
              </svg>
              Transfers
            </span>
            <span className="menu-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
              </svg>
              Ledger Explorer
            </span>
            <span className="menu-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              Security
            </span>
            <span className="menu-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              Settings
            </span>
          </nav>
        </div>
      </aside>


      {/* RIGHT MAIN CONTENT */}
      <main className="main-content">
        
        {/* TOP BAR */}
        <header className="top-bar">
          <div className="network-status-container">
            <div className="network-status-divider"></div>
            <div className="bank-status">
              <span className="bank-node-status">
                <span className="node-dot"></span>
                BANK A
              </span>
              <span className="bank-node-status">
                <span className="node-dot"></span>
                BANK B
              </span>
              <span className="bank-node-status">
                <span className="node-dot"></span>
                BANK C
              </span>
            </div>
          </div>

        </header>

        {/* ASSET DISTRIBUTION PANEL */}
        <section className="dashboard-panel">
          <div className="panel-header-row">
            <div className="panel-title-group">
              <span className="panel-subtitle">Asset Distribution</span>
              <h1 className="panel-title">Multi-Bank Consolidated Portfolio</h1>
            </div>
            
            <div className="portfolio-value-group">
              <div className="portfolio-value-label">Total Liquid Value</div>
              <div className="portfolio-value">
                {totalLiquidValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                <span className="portfolio-currency">USD</span>
              </div>
            </div>
          </div>

          <div className="bank-cards-grid">
            <div className="bank-card">
              <div className="bank-card-header">
                <span className="bank-card-title">Bank A</span>
                <div className="bank-card-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                </div>
              </div>
              <div className="bank-card-body">
                <div className="bank-card-amount">${bankATotal.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
              </div>
            </div>

            <div className="bank-card">
              <div className="bank-card-header">
                <span className="bank-card-title">Bank B</span>
                <div className="bank-card-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
                  </svg>
                </div>
              </div>
              <div className="bank-card-body">
                <div className="bank-card-amount">${bankBTotal.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
              </div>
            </div>

            <div className="bank-card">
              <div className="bank-card-header">
                <span className="bank-card-title">Bank C </span>
                <div className="bank-card-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
              </div>
              <div className="bank-card-body">
                <div className="bank-card-amount">${bankCTotal.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>

              </div>
            </div>
          </div>
        </section>

        {/* ACCOUNT SEARCH FORM */}
        <AccountSearch
          accessBank={accessBank}
          clientId={clientId}
          loading={loading}
          onAccessBankChange={setAccessBank}
          onClientIdChange={setClientId}
          onSubmit={handleSubmit}
        />

        {error && (
          <div className="status-section error" style={{ marginBottom: '24px' }}>
            <p className="status-message">{error}</p>
          </div>
        )}
        
        {!error && searched && !loading && accounts.length === 0 && (
          <div className="status-section info" style={{ marginBottom: '24px' }}>
            <p className="status-message">No se encontraron cuentas para el cliente consultado.</p>
          </div>
        )}

        {/* INTERACTION PANEL: ACCOUNTS TABLE & OPERATIONS FORM */}
        {searched && !loading && accounts.length > 0 && (
          <section className="operations-panel" style={{ gap: '24px' }}>
            <div className="panel-title-group">
              <span className="panel-subtitle">Consulta consolidada</span>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                Cuentas del Cliente: {clientId}
              </h2>
            </div>

            <AccountTable accounts={accounts} />
          </section>
        )}

        <OperationForm
          accessBank={accessBank}
          accounts={accounts}
          loading={operationLoading}
          onSubmit={handleOperationSubmit}
        />

        <TransactionStatus status={operationStatus} />

        {/* RECENT LEDGER ACTIVITY */}
        <section className="ledger-activity-panel">
          <div className="activity-header">
            <h3 className="activity-title">Recent Ledger Activity</h3>
            <span className="view-logs-link" onClick={() => alert('Mostrando logs históricos del Ledger')}>View All Logs</span>
          </div>

          <div className="table-container">
            <table className="activity-table">
              <thead>
                <tr>
                  <th>Transaction ID</th>
                  <th>From</th>
                  <th>To</th>
                  <th>Amount</th>
                  <th>Timestamp</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {mockTransactions.map((tx) => (
                  <tr key={tx.id}>
                    <td className="tx-id-cell">{tx.id}</td>
                    <td className="tx-bank-cell">{tx.from}</td>
                    <td className="tx-bank-cell">{tx.to}</td>
                    <td className="tx-amount-cell">${Number(tx.amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    <td className="tx-time-cell">{tx.timestamp}</td>
                    <td>
                      <span className={`status-badge ${tx.status.toLowerCase()}`}>
                        {tx.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

      </main>
    </div>
  );
}

export default HomePage;


