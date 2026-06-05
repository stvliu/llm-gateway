import { useState } from 'react';
import { Button, Popconfirm, Space, theme } from 'antd';
import { useTranslation } from 'react-i18next';

/**
 * 通用行内编辑列表组件
 * 支持查看、编辑、新增、删除操作
 */
export interface InlineEditableListProps<T> {
  /** 数据项列表 */
  items: T[];
  /** 渲染展示行 */
  renderItem: (item: T) => React.ReactNode;
  /** 渲染编辑表单 */
  renderEditForm: (item: T, onSave: (updated: T) => void, onCancel: () => void) => React.ReactNode;
  /** 渲染新增表单 */
  renderAddForm: (onSave: (newItem: Partial<T>) => void, onCancel: () => void) => React.ReactNode;
  /** 点击添加按钮回调 */
  onAdd: () => void;
  /** 删除项回调 */
  onDelete?: (item: T) => void;
  /** 编辑保存回调 */
  onSave?: (item: T) => void;
  /** 新增保存回调 */
  onSaveNew?: (item: Partial<T>) => void;
  /** 获取项的唯一标识 */
  getKey: (item: T) => string | number;
  /** 添加按钮文本 */
  addLabel?: string;
}

export function InlineEditableList<T>({
  items,
  renderItem,
  renderEditForm,
  renderAddForm,
  onAdd,
  onDelete,
  onSave,
  onSaveNew,
  getKey,
  addLabel,
}: InlineEditableListProps<T>) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  // 正在编辑的项ID
  const [editingKey, setEditingKey] = useState<string | number | null>(null);
  // 是否显示新增表单
  const [isAdding, setIsAdding] = useState(false);

  /** 开始编辑 */
  const handleEdit = (key: string | number) => {
    setEditingKey(key);
    setIsAdding(false);
  };

  /** 取消编辑 */
  const handleCancelEdit = () => {
    setEditingKey(null);
  };

  /** 保存编辑 */
  const handleSaveEdit = (updated: T) => {
    setEditingKey(null);
    onSave?.(updated);
  };

  /** 开始新增 */
  const handleStartAdd = () => {
    setIsAdding(true);
    setEditingKey(null);
    onAdd();
  };

  /** 取消新增 */
  const handleCancelAdd = () => {
    setIsAdding(false);
  };

  /** 保存新增 */
  const handleSaveAdd = (newItem: Partial<T>) => {
    setIsAdding(false);
    onSaveNew?.(newItem);
  };

  return (
    <div>
      {/* 数据列表 */}
      {items.map((item) => {
        const key = getKey(item);
        const isEditing = editingKey === key;

        return (
          <div key={key} style={{ marginBottom: 8 }}>
            {isEditing ? (
              // 编辑模式：蓝色边框高亮
              <div
                style={{
                  padding: 12,
                  border: `1px solid ${token.colorPrimary}`,
                  borderRadius: 6,
                  backgroundColor: token.colorPrimaryBg,
                }}
              >
                {renderEditForm(item, handleSaveEdit, handleCancelEdit)}
              </div>
            ) : (
              // 展示模式
              <div
                style={{
                  padding: 12,
                  border: `1px solid ${token.colorBorder}`,
                  borderRadius: 6,
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div style={{ flex: 1 }}>{renderItem(item)}</div>
                <Space size="small">
                  <Button
                    type="link"
                    size="small"
                    onClick={() => handleEdit(key)}
                  >
                    {t('inlineList.edit')}
                  </Button>
                  {onDelete && (
                    <Popconfirm
                      title={t('inlineList.confirmDelete')}
                      onConfirm={() => onDelete(item)}
                      okText={t('inlineList.confirm')}
                      cancelText={t('inlineList.cancel')}
                    >
                      <Button type="link" size="small" danger>
                        {t('inlineList.delete')}
                      </Button>
                    </Popconfirm>
                  )}
                </Space>
              </div>
            )}
          </div>
        );
      })}

      {/* 新增表单（列表末尾） */}
      {isAdding && (
        <div
          style={{
            padding: 12,
            border: `1px solid ${token.colorPrimary}`,
            borderRadius: 6,
            backgroundColor: token.colorPrimaryBg,
            marginBottom: 8,
          }}
        >
          {renderAddForm(handleSaveAdd, handleCancelAdd)}
        </div>
      )}

      {/* 添加按钮 */}
      {!isAdding && editingKey === null && (
        <Button type="dashed" block onClick={handleStartAdd}>
          + {addLabel || t('inlineList.add')}
        </Button>
      )}
    </div>
  );
}