import { useState, useEffect } from 'react';
import { Table, Button, Space, Dropdown, Checkbox, theme, Tooltip } from 'antd';
import type { TableProps, MenuProps } from 'antd';
import { SettingOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

export interface ColumnConfig {
  key: string;
  title: string;
  dataIndex?: string;
  width?: number | string;
  sortable?: boolean;
  visible?: boolean;
  /** 样式函数 - 根据值动态改变单元格样式 */
  cellStyleFunction?: (value: unknown, record: unknown) => React.CSSProperties;
  /** 内容函数 - 自定义内容渲染 */
  cellContentFunction?: (value: unknown, record: unknown) => ReactNode;
  render?: (value: unknown, record: unknown, index: number) => ReactNode;
}

export interface PaginationConfig {
  current: number;
  pageSize: number;
  total: number;
  onChange: (page: number, pageSize: number) => void;
  showSizeChanger?: boolean;
  showQuickJumper?: boolean;
  showTotal?: (total: number) => ReactNode;
}

export interface SelectionConfig<T> {
  selectedRowKeys: (string | number)[];
  onChange: (keys: (string | number)[], rows: T[]) => void;
  /** 批量操作配置 */
  actions?: {
    key: string;
    label: string;
    icon?: ReactNode;
    onClick: (keys: (string | number)[], rows: T[]) => void;
  }[];
}

export interface EntityTableProps<T> {
  dataSource: T[];
  loading?: boolean;
  columns: ColumnConfig[];
  rowKey: string | ((record: T) => string);
  selectedRowId?: string | number;
  onRowClick?: (record: T, index: number) => void;
  pagination?: PaginationConfig | false;
  /** 批量选择配置 */
  selection?: SelectionConfig<T>;
  /** 显示列配置按钮 */
  showColumnConfig?: boolean;
  /** 列配置变更回调 */
  onColumnConfigChange?: (columns: ColumnConfig[]) => void;
  /** 显示刷新按钮 */
  showRefresh?: boolean;
  /** 刷新回调 */
  onRefresh?: () => void;
  /** 表格尺寸 */
  size?: 'small' | 'middle' | 'large';
}

export function EntityTable<T extends object>({
  dataSource,
  loading = false,
  columns,
  rowKey,
  selectedRowId,
  onRowClick,
  pagination,
  selection,
  showColumnConfig = false,
  onColumnConfigChange,
  showRefresh = false,
  onRefresh,
  size = 'middle',
}: EntityTableProps<T>) {
  const { t } = useTranslation();
  const { token } = theme.useToken();
  const [internalColumns, setInternalColumns] = useState(columns);

  // 同步外部 columns prop 到内部状态
  useEffect(() => {
    setInternalColumns(columns);
  }, [columns]);

  // 可见列
  const visibleColumns = internalColumns.filter((c) => c.visible !== false);

  // 列配置菜单
  const columnConfigMenu: MenuProps = {
    items: internalColumns.map((col) => ({
      key: col.key,
      label: (
        <Checkbox
          checked={col.visible !== false}
          onChange={(e) => {
            const newColumns = internalColumns.map((c) =>
              c.key === col.key ? { ...c, visible: e.target.checked } : c
            );
            setInternalColumns(newColumns);
            onColumnConfigChange?.(newColumns);
          }}
        >
          {col.title}
        </Checkbox>
      ),
    })),
  };

  // 转换为 Ant Design 列配置
  const antdColumns: TableProps<T>['columns'] = visibleColumns.map((col) => ({
    key: col.key,
    title: col.title,
    dataIndex: col.dataIndex,
    width: col.width,
    sorter: col.sortable ? true : undefined,
    render: (value: unknown, record: T, index: number) => {
      // 应用内容函数
      let content: ReactNode = value as ReactNode;
      if (col.cellContentFunction) {
        content = col.cellContentFunction(value, record);
      } else if (col.render) {
        content = col.render(value, record, index);
      }

      // 应用样式函数
      const style = col.cellStyleFunction?.(value, record);

      return <span style={style}>{content}</span>;
    },
  }));

  // 表格属性
  const tableProps: TableProps<T> = {
    columns: antdColumns,
    dataSource,
    loading,
    rowKey,
    size,
    pagination:
      pagination === false
        ? false
        : {
            current: (pagination as PaginationConfig).current,
            pageSize: (pagination as PaginationConfig).pageSize,
            total: (pagination as PaginationConfig).total,
            onChange: (pagination as PaginationConfig).onChange,
            showSizeChanger: (pagination as PaginationConfig).showSizeChanger ?? true,
            showQuickJumper: (pagination as PaginationConfig).showQuickJumper ?? true,
            showTotal: (pagination as PaginationConfig).showTotal ?? ((total) => t('pagination.total', { count: total })),
          },
    rowSelection: selection
      ? {
          type: 'checkbox',
          selectedRowKeys: selection.selectedRowKeys,
          onChange: (keys, rows) =>
            selection.onChange(keys as (string | number)[], rows as T[]),
        }
      : undefined,
    onRow: (record, index) => ({
      onClick: (e) => {
        // 排除复选框和操作按钮区域
        const target = e.target as HTMLElement;
        if (
          target.closest('.ant-checkbox-wrapper') ||
          target.closest('.ant-btn') ||
          target.closest('.table-action-cell')
        ) {
          return;
        }
        if (onRowClick && index !== undefined) {
          onRowClick(record, index);
        }
      },
      style: {
        cursor: onRowClick ? 'pointer' : 'default',
      },
    }),
    rowClassName: (record) => {
      const key =
        typeof rowKey === 'function' ? rowKey(record) : (record as Record<string, unknown>)[rowKey as string];
      return key === selectedRowId ? 'table-row-selected' : '';
    },
  };

  // 批量操作栏
  const batchActionBar =
    selection && selection.selectedRowKeys.length > 0 && (
      <div
        style={{
          background: token.colorPrimaryBg,
          padding: '8px 16px',
          marginBottom: 8,
          borderRadius: 6,
          display: 'flex',
          alignItems: 'center',
          gap: 16,
        }}
      >
        <span style={{ fontSize: 14, color: token.colorPrimaryTextActive }}>
          {t('table.selected', { count: selection.selectedRowKeys.length })}
        </span>
        <Space size="small">
          {selection.actions?.map((action) => (
            <Button
              key={action.key}
              type="text"
              size="small"
              icon={action.icon}
              onClick={() => {
                const selectedRows = dataSource.filter((row) => {
                  const key = typeof rowKey === 'function' ? rowKey(row) : (row as Record<string, unknown>)[rowKey as string];
                  return selection.selectedRowKeys.includes(key as string | number);
                });
                action.onClick(selection.selectedRowKeys, selectedRows);
              }}
            >
              {action.label}
            </Button>
          ))}
        </Space>
      </div>
    );

  return (
    <div className="entity-table-wrapper">
      {/* 工具栏 */}
      {(showColumnConfig || showRefresh) && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginBottom: 8 }}>
          {showRefresh && (
            <Tooltip title={t('actions.refresh')}>
              <Button type="text" icon={<ReloadOutlined />} onClick={onRefresh} />
            </Tooltip>
          )}
          {showColumnConfig && (
            <Dropdown menu={columnConfigMenu} trigger={['click']}>
              <Tooltip title={t('table.columnConfig')}>
                <Button type="text" icon={<SettingOutlined />} />
              </Tooltip>
            </Dropdown>
          )}
        </div>
      )}

      {/* 批量操作栏 */}
      {batchActionBar}

      {/* 表格 */}
      <Table {...tableProps} />
    </div>
  );
}
