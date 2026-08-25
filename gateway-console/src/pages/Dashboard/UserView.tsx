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
import { Row, Col, Card, Statistic } from 'antd';
import { AppstoreOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useApplications } from '@/services/query/useApplications';

/**
 * 用户仪表盘
 *
 * <p>普通用户无供应商读权限，仅展示可用应用数量。</p>
 */
export default function UserView() {
  const { t } = useTranslation('dashboard');
  const { data: applications } = useApplications();

  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} sm={12}>
        <Card>
          <Statistic
            title={t('stats.applications', { defaultValue: '可用应用' })}
            value={applications?.length ?? 0}
            prefix={<AppstoreOutlined />}
          />
        </Card>
      </Col>
    </Row>
  );
}
