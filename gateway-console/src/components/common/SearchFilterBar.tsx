import { useState, useCallback } from 'react';
import { Input, Select, Button } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export interface FilterOption {
  key: string;
  label: string;
  options: { value: string; label: string }[];
  placeholder?: string;
}

export interface SearchFilters {
  keyword: string;
  [key: string]: string | undefined;
}

interface SearchFilterBarProps {
  placeholder?: string;
  filters?: FilterOption[];
  onSearch: (filters: SearchFilters) => void;
  onReset?: () => void;
  loading?: boolean;
}

/**
 * 搜索筛选栏组件
 * 支持关键词搜索和多条件筛选
 */
export function SearchFilterBar({
  placeholder,
  filters = [],
  onSearch,
  onReset,
  loading,
}: SearchFilterBarProps) {
  const { t } = useTranslation('models');
  const [keyword, setKeyword] = useState('');
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});

  const handleSearch = useCallback(() => {
    onSearch({
      keyword,
      ...filterValues,
    });
  }, [keyword, filterValues, onSearch]);

  const handleReset = useCallback(() => {
    setKeyword('');
    setFilterValues({});
    onReset?.();
  }, [onReset]);

  const handleKeywordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setKeyword(e.target.value);
  };

  const handleFilterChange = (key: string, value: string) => {
    setFilterValues((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  return (
    <>
      <Input
        placeholder={placeholder || t('search.placeholder')}
        prefix={<SearchOutlined />}
        value={keyword}
        onChange={handleKeywordChange}
        onPressEnter={handleSearch}
        allowClear
        style={{ width: 280 }}
      />

      {filters.map((filter) => (
        <Select
          key={filter.key}
          placeholder={filter.placeholder || filter.label}
          value={filterValues[filter.key]}
          onChange={(value) => handleFilterChange(filter.key, value)}
          allowClear
          style={{ width: 160 }}
          options={filter.options}
        />
      ))}

      <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch} loading={loading}>
        {t('search.searchBtn')}
      </Button>

      <Button icon={<ReloadOutlined />} onClick={handleReset}>
        {t('actions.reset', { ns: 'common' })}
      </Button>
    </>
  );
}
