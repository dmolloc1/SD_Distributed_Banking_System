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
    <section className="account-groups">
      {Object.entries(accountsByBank).map(([bankCode, bankAccounts]) => (
        <div className="account-group" key={bankCode}>
          <h2>{bankCode}</h2>
          <div className="table-wrapper">
            <table>
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
                    <td>{account.accountId}</td>
                    <td>{account.clientId}</td>
                    <td>{account.type}</td>
                    <td>{account.currency}</td>
                    <td>{Number(account.balance).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ))}
    </section>
  );
}

export default AccountTable;
