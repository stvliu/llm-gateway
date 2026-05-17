import { useState, useCallback } from 'react';
import { providerApi, type ConnectivityTestRequest, type ConnectivityTestResult } from '@/services/api/provider';

export type TestStatus = 'idle' | 'testing' | 'success' | 'failed';

interface UseConnectivityTestOptions {
  onSuccess?: (result: ConnectivityTestResult) => void;
  onError?: (error: Error) => void;
}

interface UseConnectivityTestReturn {
  status: TestStatus;
  result: ConnectivityTestResult | null;
  error: Error | null;
  test: (request: ConnectivityTestRequest) => Promise<ConnectivityTestResult | null>;
  reset: () => void;
}

/**
 * 连通性测试 Hook
 *
 * 用于执行 Provider API Key 连通性测试
 */
export function useConnectivityTest(options?: UseConnectivityTestOptions): UseConnectivityTestReturn {
  const [status, setStatus] = useState<TestStatus>('idle');
  const [result, setResult] = useState<ConnectivityTestResult | null>(null);
  const [error, setError] = useState<Error | null>(null);

  const test = useCallback(async (request: ConnectivityTestRequest): Promise<ConnectivityTestResult | null> => {
    setStatus('testing');
    setError(null);

    try {
      const response = await providerApi.testConnectivity(request);
      setResult(response);
      setStatus(response.success ? 'success' : 'failed');
      options?.onSuccess?.(response);
      return response;
    } catch (err) {
      const error = err instanceof Error ? err : new Error(String(err));
      setError(error);
      setStatus('failed');
      options?.onError?.(error);
      return null;
    }
  }, [options]);

  const reset = useCallback(() => {
    setStatus('idle');
    setResult(null);
    setError(null);
  }, []);

  return { status, result, error, test, reset };
}

export type { ConnectivityTestRequest, ConnectivityTestResult };
