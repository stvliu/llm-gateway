/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { applicationApi } from '@/services/api/application';
import type {
  CreateApplicationRequest,
  UpdateApplicationRequest,
} from '@/types/application';

/** 应用 Query Keys */
export const applicationKeys = {
  all: ['applications'] as const,
  lists: () => [...applicationKeys.all, 'list'] as const,
  details: () => [...applicationKeys.all, 'detail'] as const,
  detail: (id: number) => [...applicationKeys.details(), id] as const,
  channels: (id: number) => [...applicationKeys.detail(id), 'channels'] as const,
};

/** 应用列表 */
export function useApplications() {
  return useQuery({
    queryKey: applicationKeys.lists(),
    queryFn: () => applicationApi.list(),
  });
}

/** 创建应用 */
export function useCreateApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateApplicationRequest) => applicationApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: applicationKeys.lists() });
    },
  });
}

/** 更新应用 */
export function useUpdateApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateApplicationRequest }) =>
      applicationApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: applicationKeys.lists() });
    },
  });
}

/** 删除应用 */
export function useDeleteApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => applicationApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: applicationKeys.lists() });
    },
  });
}
