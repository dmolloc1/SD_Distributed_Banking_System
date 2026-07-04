function TransactionStatus({ status }) {
  if (!status) {
    return null;
  }

  return (
    <section className={`transaction-status ${status.kind}`}>
      <h2>Estado de operacion</h2>
      <p>{status.message}</p>
      {status.details && (
        <pre>{JSON.stringify(status.details, null, 2)}</pre>
      )}
    </section>
  );
}

export default TransactionStatus;
