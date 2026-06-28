import { useState } from 'react';
import { getDistributedAccounts, submitDeposit, submitTransfer, submitWithdraw } from '../api/coordinatorApi.js';
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

    try {
      const response = await submitSelectedOperation(operationType, payload);
      setOperationStatus({
        kind: response.ok ? 'success' : 'pending',
        message: response.ok
          ? 'Operacion completada.'
          : 'Ruta preparada en Gateway. Implementacion del coordinador pendiente.',
        details: response.data,
      });
    } catch (requestError) {
      setOperationStatus({
        kind: 'error',
        message: 'No se pudo enviar la operacion al API Gateway.',
      });
    } finally {
      setOperationLoading(false);
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

  return (
    <main className="page">
      <section className="panel">
        <div className="header">
          <span>Sistema Bancario Distribuido</span>
          <h1>Consulta consolidada de cuentas</h1>
        </div>

        <AccountSearch
          accessBank={accessBank}
          clientId={clientId}
          loading={loading}
          onAccessBankChange={setAccessBank}
          onClientIdChange={setClientId}
          onSubmit={handleSubmit}
        />

        {error && <p className="message error">{error}</p>}
        {!error && searched && !loading && accounts.length === 0 && (
          <p className="message">No se encontraron cuentas para el cliente consultado.</p>
        )}

        <AccountTable accounts={accounts} />
        <OperationForm
          accessBank={accessBank}
          accounts={accounts}
          loading={operationLoading}
          onSubmit={handleOperationSubmit}
        />
        <TransactionStatus status={operationStatus} />
      </section>
    </main>
  );
}

export default HomePage;
