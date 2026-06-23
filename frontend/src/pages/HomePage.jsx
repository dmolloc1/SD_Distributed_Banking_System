import { useState } from 'react';
import { getDistributedAccounts } from '../api/coordinatorApi.js';
import AccountSearch from '../components/AccountSearch.jsx';
import AccountTable from '../components/AccountTable.jsx';

function HomePage() {
  const [clientId, setClientId] = useState('C005');
  const [accounts, setAccounts] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
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
    setSearched(true);

    try {
      const result = await getDistributedAccounts(normalizedClientId);
      setAccounts(result);
    } catch (requestError) {
      setAccounts([]);
      setError('No se pudo conectar con el coordinator-service.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page">
      <section className="panel">
        <div className="header">
          <span>Sistema Bancario Distribuido</span>
          <h1>Consulta consolidada de cuentas</h1>
        </div>

        <AccountSearch
          clientId={clientId}
          loading={loading}
          onClientIdChange={setClientId}
          onSubmit={handleSubmit}
        />

        {error && <p className="message error">{error}</p>}
        {!error && searched && !loading && accounts.length === 0 && (
          <p className="message">No se encontraron cuentas para el cliente consultado.</p>
        )}

        <AccountTable accounts={accounts} />
      </section>
    </main>
  );
}

export default HomePage;
