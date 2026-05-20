import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productApi, productApiKeyApi } from '@/services/api/product';
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

/** 获取产品下的 API Key 列表 */
export function useProductApiKeys(productId: number) {
  return useQuery({
    queryKey: productKeys.keys(productId),
    queryFn: () => productApiKeyApi.list(productId),
    enabled: !!productId,
  });
}

/** 创建产品 API Key */
export function useCreateProductApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ productId, data }: { productId: number; data: CreateProductApiKeyRequest }) =>
      productApiKeyApi.create(productId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.keys(variables.productId) });
    },
  });
}

/** 更新产品 API Key */
export function useUpdateProductApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ productId, id, data }: { productId: number; id: number; data: UpdateProductApiKeyRequest }) =>
      productApiKeyApi.update(productId, id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.keys(variables.productId) });
    },
  });
}

/** 删除产品 API Key */
export function useDeleteProductApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ productId, id }: { productId: number; id: number }) =>
      productApiKeyApi.delete(productId, id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: productKeys.keys(variables.productId) });
    },
  });
}