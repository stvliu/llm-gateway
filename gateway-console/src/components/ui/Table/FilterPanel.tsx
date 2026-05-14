import { useState } from 'react';
import { Modal, Select, Input, Button, Space, Radio } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export interface FilterCondition {
  key: string;
  field: string;
  operator: string;
  value: string;
}

export interface FilterPanelProps {
  open: boolean;
  onClose: () => void;
  onApply: (conditions: FilterCondition[], logic: 'and' | 'or') => void;
  fields: {
    key: string;
    label: string;
    type: 'string' | 'number' | 'date' | 'boolean';
  }[];
  initialConditions?: FilterCondition[];
  initialLogic?: 'and' | 'or';
}

/**
 * 过滤器面板组件
 * 支持多条件过滤和逻辑关系配置
 */
export function FilterPanel({
  open,
  onClose,
  onApply,
  fields,
  initialConditions = [],
  initialLogic = 'and',
}: FilterPanelProps) {
  const { t } = useTranslation();

  const [conditions, setConditions] = useState<FilterCondition[]>(
    initialConditions.length > 0
      ? initialConditions
      : [{ key: '1', field: '', operator: '', value: '' }]
  );
  const [logic, setLogic] = useState<'and' | 'or'>(initialLogic);

  // 操作符配置
  const operators = {
    string: [
      { key: 'eq', label: t('operators.eq') },
      { key: 'ne', label: t('operators.ne') },
      { key: 'contains', label: t('operators.contains') },
      { key: 'notContains', label: t('operators.notContains') },
      { key: 'startsWith', label: t('operators.startsWith') },
      { key: 'endsWith', label: t('operators.endsWith') },
    ],
    number: [
      { key: 'eq', label: t('operators.eq') },
      { key: 'ne', label: t('operators.ne') },
      { key: 'gt', label: t('operators.gt') },
      { key: 'lt', label: t('operators.lt') },
      { key: 'gte', label: t('operators.gte') },
      { key: 'lte', label: t('operators.lte') },
    ],
    date: [
      { key: 'eq', label: t('operators.eq') },
      { key: 'ne', label: t('operators.ne') },
      { key: 'before', label: t('operators.before') },
      { key: 'after', label: t('operators.after') },
      { key: 'between', label: t('operators.between') },
    ],
    boolean: [{ key: 'eq', label: t('operators.eq') }],
  };

  const addCondition = () => {
    setConditions([
      ...conditions,
      { key: Date.now().toString(), field: '', operator: '', value: '' },
    ]);
  };

  const removeCondition = (key: string) => {
    if (conditions.length > 1) {
      setConditions(conditions.filter((c) => c.key !== key));
    }
  };

  const updateCondition = (
    key: string,
    updates: Partial<FilterCondition>
  ) => {
    setConditions(
      conditions.map((c) => (c.key === key ? { ...c, ...updates } : c))
    );
  };

  const handleApply = () => {
    const validConditions = conditions.filter(
      (c) => c.field && c.operator && c.value
    );
    onApply(validConditions, logic);
    onClose();
  };

  const handleReset = () => {
    setConditions([{ key: '1', field: '', operator: '', value: '' }]);
    setLogic('and');
  };

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title={t('filter.title')}
      width={600}
      footer={
        <div className="flex justify-between">
          <Button onClick={handleReset}>{t('actions.reset')}</Button>
          <Space>
            <Button onClick={onClose}>{t('actions.cancel')}</Button>
            <Button type="primary" onClick={handleApply}>
              {t('filter.apply')}
            </Button>
          </Space>
        </div>
      }
    >
      <div className="py-4">
        {/* 过滤条件列表 */}
        <div className="space-y-3">
          {conditions.map((condition) => {
            const field = fields.find((f) => f.key === condition.field);
            const fieldType = field?.type || 'string';
            const fieldOperators = operators[fieldType];

            return (
              <div key={condition.key} className="flex items-center gap-2">
                {/* 字段选择 */}
                <Select
                  value={condition.field}
                  onChange={(v) =>
                    updateCondition(condition.key, {
                      field: v,
                      operator: '',
                      value: '',
                    })
                  }
                  placeholder={t('filter.addField')}
                  className="w-32"
                  options={fields.map((f) => ({
                    value: f.key,
                    label: f.label,
                  }))}
                />

                {/* 操作符选择 */}
                <Select
                  value={condition.operator}
                  onChange={(v) =>
                    updateCondition(condition.key, { operator: v })
                  }
                  placeholder={t('operators.eq')}
                  className="w-28"
                  options={fieldOperators.map((o) => ({
                    value: o.key,
                    label: o.label,
                  }))}
                />

                {/* 值输入 */}
                <Input
                  value={condition.value}
                  onChange={(e) =>
                    updateCondition(condition.key, { value: e.target.value })
                  }
                  placeholder={t('actions.search')}
                  className="flex-1"
                />

                {/* 删除按钮 */}
                <Button
                  type="text"
                  icon={<DeleteOutlined />}
                  onClick={() => removeCondition(condition.key)}
                  disabled={conditions.length <= 1}
                  danger
                />
              </div>
            );
          })}
        </div>

        {/* 添加条件按钮 */}
        <Button
          type="dashed"
          icon={<PlusOutlined />}
          onClick={addCondition}
          className="w-full mt-3"
        >
          {t('filter.addField')}
        </Button>

        {/* 条件关系 */}
        <div className="mt-4">
          <span className="text-sm text-gray-500 mr-4">
            {t('filter.logicLabel')}:
          </span>
          <Radio.Group value={logic} onChange={(e) => setLogic(e.target.value)}>
            <Radio value="and">{t('filter.logicAnd')}</Radio>
            <Radio value="or">{t('filter.logicOr')}</Radio>
          </Radio.Group>
        </div>
      </div>
    </Modal>
  );
}