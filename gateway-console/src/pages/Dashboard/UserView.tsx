import { Row, Col, Card, Statistic } from 'antd';
import {
  TeamOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviders } from '@/services/query/useProviders';
import { useTeams } from '@/services/query/useTeams';

/** 用户仪表盘 */
export default function UserView() {
  const { t } = useTranslation('dashboard');
  const { data: providersData } = useProviders();
  const { data: teams } = useTeams();

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
            title={t('stats.teams', { defaultValue: '我的团队' })}
            value={teams?.length ?? 0}
            prefix={<TeamOutlined />}
          />
        </Card>
      </Col>
    </Row>
  );
}