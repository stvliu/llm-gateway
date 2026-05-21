import apiClient from './client';
import type { ProtocolInfo } from '../../types/product';

const BASE_URL = '/api/protocols';

export const protocolApi = {
  /** 获取所有可用协议 */
  list: async (): Promise<ProtocolInfo[]> => {
    const response = await apiClient.get(BASE_URL);
    return response.data;
  },
};