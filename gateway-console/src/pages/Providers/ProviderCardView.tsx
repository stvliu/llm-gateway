import { Row, Col } from 'antd';
import type { Provider } from '@/types/provider';
import ProviderCard from './ProviderCard';

interface Props {
  providers: Provider[];
  onSelect: (provider: Provider) => void;
  onEdit: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onViewProducts?: (provider: Provider) => void;
}

export default function ProviderCardView({ providers, onSelect, onEdit, onDelete, onViewProducts }: Props) {
  return (
    <Row gutter={[16, 16]}>
      {providers.map((provider) => (
        <Col key={provider.id} xs={24} sm={12} md={8} lg={6}>
          <ProviderCard
            provider={provider}
            onView={() => onSelect(provider)}
            onEdit={() => onEdit(provider)}
            onDelete={() => onDelete(provider)}
            onViewProducts={() => onViewProducts?.(provider)}
          />
        </Col>
      ))}
    </Row>
  );
}