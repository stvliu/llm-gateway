/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { Row, Col, Card, Statistic } from 'antd';
import {
  AppstoreOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviders } from '@/services/query/useProviders';
import { useApplications } from '@/services/query/useApplications';

/** 用户仪表盘 */
export default function UserView() {
  const { t } = useTranslation('dashboard');
  const { data: providersData } = useProviders();
  const { data: applications } = useApplications();

  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} sm={12}>
        <Card>
          <Statistic
            title={t('stats.providers', { defaultValue: '供应商' })}
            value={providersData?.pagination?.total ?? 0}
            prefix={<CloudServerOutlined />}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12}>
        <Card>
          <Statistic
            title={t('stats.applications', { defaultValue: '我的应用' })}
            value={applications?.length ?? 0}
            prefix={<AppstoreOutlined />}
          />
        </Card>
      </Col>
    </Row>
  );
}
