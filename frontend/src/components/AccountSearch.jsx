function AccountSearch({ accessBank, clientId, loading, onAccessBankChange, onClientIdChange, onSubmit }) {
  return (
    <form className="search-container" onSubmit={onSubmit}>
      <span className="search-label">Punto de Acceso</span>
      <select 
        className="search-select" 
        value={accessBank} 
        onChange={(event) => onAccessBankChange(event.target.value)}
      >
        <option value="BANK_A">BANK A</option>
        <option value="BANK_B">BANK B</option>
        <option value="BANK_C">BANK C</option>
      </select>

      <span className="search-label">ID Cliente</span>
      <div className="search-input-wrapper">
        <input
          id="clientId"
          className="search-input"
          type="text"
          value={clientId}
          onChange={(event) => onClientIdChange(event.target.value)}
          placeholder="Ej: CLI-123 o C005"
          autoComplete="off"
        />
      </div>

      <button className="search-btn" type="submit" disabled={loading}>
        {loading ? 'Consultando...' : 'CONSULTAR'}
      </button>
    </form>
  );
}

export default AccountSearch;

