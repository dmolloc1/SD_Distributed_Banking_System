function AccountSearch({ clientId, loading, onClientIdChange, onSubmit }) {
  return (
    <form className="search-form" onSubmit={onSubmit}>
      <label htmlFor="clientId">Cliente</label>
      <div className="search-row">
        <input
          id="clientId"
          type="text"
          value={clientId}
          onChange={(event) => onClientIdChange(event.target.value)}
          placeholder="C001"
          autoComplete="off"
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Consultando...' : 'Consultar cuentas'}
        </button>
      </div>
    </form>
  );
}

export default AccountSearch;
