import { Row, Col } from 'antd';
import type { Provider } from '@/types/provider';
import ProviderCard from './ProviderCard';

interface Props {
  providers: Provider[];
  onSelect: (provider: Provider) => void;
  onViewProducts?: (provider: Provider) => void;
}

export default function ProviderCardView({ providers, onSelect, onViewProducts }: Props) {
  return (
    <Row gutter={[16, 16]}>
      {providers.map((provider) => (
        <Col key={provider.id} xs={24} sm={12} md={8} lg={6}>
          <ProviderCard
            provider={provider}
            onClick={() => onSelect(provider)}
            onViewProducts={() => onViewProducts?.(provider)}
          />
        </Col>
      ))}
    </Row>
  );
}