const API_GATEWAY_BASE_URL = 'http://localhost:8080';

export async function getDistributedAccounts(clientId) {
  const response = await fetch(`${API_GATEWAY_BASE_URL}/api/customers/${encodeURIComponent(clientId)}/accounts`);

  if (!response.ok) {
    throw new Error('API Gateway unavailable');
  }

  return response.json();
}
