const API_GATEWAY_BASE_URL = 'http://localhost:8080';

export async function getDistributedAccounts(clientId) {
  const response = await fetch(`${API_GATEWAY_BASE_URL}/api/customers/${encodeURIComponent(clientId)}/accounts`);

  if (!response.ok) {
    throw new Error('API Gateway unavailable');
  }

  return response.json();
}

export async function submitDeposit(payload) {
  return postGatewayOperation('/api/operations/deposit', payload);
}

export async function submitWithdraw(payload) {
  return postGatewayOperation('/api/operations/withdraw', payload);
}

export async function submitTransfer(payload) {
  return postGatewayOperation('/api/transfers', payload);
}

async function postGatewayOperation(path, payload) {
  const response = await fetch(`${API_GATEWAY_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const data = await response.json().catch(() => ({}));

  return {
    ok: response.ok,
    statusCode: response.status,
    data,
  };
}
