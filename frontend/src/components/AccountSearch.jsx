function AccountSearch({ accessBank, clientId, loading, onAccessBankChange, onClientIdChange, onSubmit }) {
  return (
    <form className="search-form" onSubmit={onSubmit}>
      <div className="form-grid">
        <label>
          Banco de acceso
          <select value={accessBank} onChange={(event) => onAccessBankChange(event.target.value)}>
            <option value="BANK_A">Banco A</option>
            <option value="BANK_B">Banco B</option>
            <option value="BANK_C">Banco C</option>
          </select>
        </label>

        <label>
          Cliente
          <input
            id="clientId"
            type="text"
            value={clientId}
            onChange={(event) => onClientIdChange(event.target.value)}
            placeholder="C001"
            autoComplete="off"
          />
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Consultando...' : 'Consultar cuentas'}
        </button>
      </div>
    </form>
  );
}

export default AccountSearch;
