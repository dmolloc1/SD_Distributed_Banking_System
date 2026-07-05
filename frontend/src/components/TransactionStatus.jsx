function TransactionStatus({ status }) {
  if (!status) {
    return null;
  }

  return (
    <div className={`status-section ${status.kind}`}>
      <div className={`status-title ${status.kind}`}>
        {status.kind === 'success' && (
          <svg style={{ width: '18px', height: '18px', fill: 'currentColor' }} viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        )}
        {status.kind === 'error' && (
          <svg style={{ width: '18px', height: '18px', fill: 'currentColor' }} viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        )}
        {status.kind === 'pending' && (
          <svg style={{ width: '18px', height: '18px', fill: 'currentColor' }} viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        )}
        {status.kind === 'info' && (
          <svg style={{ width: '18px', height: '18px', fill: 'currentColor' }} viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        )}
        Estado de Operación: {status.kind.toUpperCase()}
      </div>
      <p className="status-message">{status.message}</p>
      {status.details && (
        <pre className="status-details">{JSON.stringify(status.details, null, 2)}</pre>
      )}
    </div>
  );
}

export default TransactionStatus;

