function AccountTable({ accounts }) {
  if (accounts.length === 0) {
    return null;
  }

  const accountsByBank = accounts.reduce((groups, account) => {
    const bankCode = account.bankCode || 'SIN_BANCO';
    return {
      ...groups,
      [bankCode]: [...(groups[bankCode] || []), account],
    };
  }, {});

  return (
    <div className="account-groups" style={{ display: 'flex', flexDirection: 'column', gap: '24px', width: '100%' }}>
      {Object.entries(accountsByBank).map(([bankCode, bankAccounts]) => (
        <div className="account-group" key={bankCode} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <h3 style={{ fontSize: '0.875rem', fontWeight: '700', color: 'var(--accent-green)', textTransform: 'uppercase', letterSpacing: '0.05em', fontFamily: 'JetBrains Mono, monospace' }}>
            {bankCode}
          </h3>
          <div className="table-container">
            <table className="activity-table">
              <thead>
                <tr>
                  <th>accountId</th>
                  <th>clientId</th>
                  <th>type</th>
                  <th>currency</th>
                  <th>balance</th>
                </tr>
              </thead>
              <tbody>
                {bankAccounts.map((account) => (
                  <tr key={account.accountId}>
                    <td className="tx-id-cell">{account.accountId}</td>
                    <td>{account.clientId}</td>
                    <td>{account.type}</td>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', color: 'var(--text-secondary)' }}>{account.currency}</td>
                    <td className="tx-amount-cell">${Number(account.balance).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ))}
    </div>
  );
}

export default AccountTable;
