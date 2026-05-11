import { useQuery } from '@tanstack/react-query';
import { statsApi } from '@/services/api/stats';

export const statsKeys = {
  all: ['stats'] as const,
  detail: () => [...statsKeys.all, 'detail'] as const,
};

export function useStats() {
  return useQuery({
    queryKey: statsKeys.detail(),
    queryFn: () => statsApi.get(),
  });
}
