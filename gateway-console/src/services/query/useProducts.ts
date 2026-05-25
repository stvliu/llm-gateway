import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productApi, channelCredentialApi } from '@/services/api/product';
import type { CreateProductRequest, UpdateProductRequest, CreateProductApiKeyRequest, UpdateProductApiKeyRequest } from '@/types/product';

export const productKeys = {
  all: ['products'] as const,
  lists: () => [...productKeys.all, 'list'] as const,
  list: (providerId: number) => [...productKeys.lists(), providerId] as const,
  details: () => [...productKeys.all, 'detail'] as const,
  detail: (id: number) => [...productKeys.details(), id] as const,
  keys: (productId: number) => [...productKeys.all, 'keys', productId] as const,
};

/** 获取供应商下的产品列表 */
export function useProducts(providerId: number) {
  return useQuery({
    queryKey: productKeys.list(providerId),
    queryFn: () => productApi.list(providerId),
    enabled: !!providerId,
  });
}

/** 获取产品详情 */
export function useProduct(id: number) {
  return useQuery({
    queryKey: productKeys.detail(id),
    queryFn: () => productApi.get(id),
    enabled: !!id,
  });
}

/** 创建产品 */
export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProductRequest) => productApi.create(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.list(variables.providerId) });
    },
  });
}

/** 更新产品 */
export function useUpdateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProductRequest }) =>
      productApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: productKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

/** 删除产品 */
export function useDeleteProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: number; providerId: number }) =>
      productApi.delete(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.list(variables.providerId) });
    },
  });
}

/** 获取渠道下的凭证列表 */
export function useChannelCredentials(channelId: number) {
  return useQuery({
    queryKey: productKeys.keys(channelId),
    queryFn: () => channelCredentialApi.list(channelId),
    enabled: !!channelId,
  });
}

/** 创建渠道凭证 */
export function useCreateChannelCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, data }: { channelId: number; data: CreateProductApiKeyRequest }) =>
      channelCredentialApi.create(channelId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.keys(variables.channelId) });
    },
  });
}

/** 更新渠道凭证 */
export function useUpdateChannelCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id, data }: { channelId: number; id: number; data: UpdateProductApiKeyRequest }) =>
      channelCredentialApi.update(channelId, id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.keys(variables.channelId) });
    },
  });
}

/** 删除渠道凭证 */
export function useDeleteChannelCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id }: { channelId: number; id: number }) =>
      channelCredentialApi.delete(channelId, id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.keys(variables.channelId) });
    },
  });
}

/** 测试渠道凭证 */
export function useTestChannelCredential() {
  return useMutation({
    mutationFn: ({ channelId, id }: { channelId: number; id: number }) =>
      channelCredentialApi.test(channelId, id),
  });
}