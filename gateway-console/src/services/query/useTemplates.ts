import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { templateApi } from '@/services/api/template';
import type {
  CreateTemplateRequest,
  UpdateTemplateRequest,
  ApplyTemplateRequest,
  TemplateListParams,
  MarketStatus,
} from '@/types/template';

export const templateKeys = {
  all: ['templates'] as const,
  lists: () => [...templateKeys.all, 'list'] as const,
  list: (params?: TemplateListParams) => [...templateKeys.lists(), params] as const,
  details: () => [...templateKeys.all, 'detail'] as const,
  detail: (id: number) => [...templateKeys.details(), id] as const,
};

export function useTemplates(params?: TemplateListParams) {
  return useQuery({
    queryKey: templateKeys.list(params),
    queryFn: () => templateApi.list(params),
  });
}

export function useTemplate(id: number) {
  return useQuery({
    queryKey: templateKeys.detail(id),
    queryFn: () => templateApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTemplateRequest) => templateApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
    },
  });
}

export function useUpdateTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateTemplateRequest }) =>
      templateApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: templateKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
    },
  });
}

export function useDeleteTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => templateApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
    },
  });
}

export function useUpdateTemplateMarketState() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, marketState }: { id: number; marketState: MarketStatus }) =>
      templateApi.updateMarketState(id, marketState),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: templateKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
    },
  });
}

export function useApplyTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ApplyTemplateRequest }) =>
      templateApi.apply(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providers'] });
    },
  });
}

export function useExportTemplate() {
  return useMutation({
    mutationFn: (id: number) => templateApi.exportTemplate(id),
  });
}

export function useExportTemplates() {
  return useMutation({
    mutationFn: (ids: number[]) => templateApi.exportBatch(ids),
  });
}

export function useImportTemplates() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (formData: FormData) => templateApi.import(formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
    },
  });
}
