const COORDINATOR_BASE_URL = 'http://localhost:8080';

export async function getDistributedAccounts(clientId) {
  const response = await fetch(`${COORDINATOR_BASE_URL}/distributed/accounts/${encodeURIComponent(clientId)}`);

  if (!response.ok) {
    throw new Error('Coordinator unavailable');
  }

  return response.json();
}
