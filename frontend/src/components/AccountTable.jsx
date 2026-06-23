function AccountTable({ accounts }) {
  if (accounts.length === 0) {
    return null;
  }

  return (
    <div className="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>accountId</th>
            <th>clientId</th>
            <th>bankCode</th>
            <th>type</th>
            <th>currency</th>
            <th>balance</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((account) => (
            <tr key={account.accountId}>
              <td>{account.accountId}</td>
              <td>{account.clientId}</td>
              <td>{account.bankCode}</td>
              <td>{account.type}</td>
              <td>{account.currency}</td>
              <td>{Number(account.balance).toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default AccountTable;
