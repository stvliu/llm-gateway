/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState } from 'react';
import { Button, Space, theme, Tooltip } from 'antd';
import { EditOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

/**
 * 通用行内编辑列表组件
 * 支持查看、编辑、新增、删除操作。
 *
 * <h3>删除回调约定（自第 8 章起）</h3>
 * <p>本组件<strong>不再</strong>硬编码 Popconfirm 二次确认。<code>onDelete</code> 在
 * 用户点击"删除"按钮后立即被调用，<strong>由调用方注入自己的确认逻辑</strong>，例如：</p>
 * <ul>
 *   <li>轻量场景：包一层 <code>Popconfirm</code> 或 <code>useDangerConfirm</code></li>
 *   <li>危险场景：直接弹 <code>Modal.confirm</code> 含明确影响说明</li>
 * </ul>
 * <p>调用方可在 onDelete 内部访问完整 item 对象用于构造 description（keyMasked、baseUrl 等）。</p>
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
  /**
   * 删除项回调。
   * 自第 8 章起：调用方负责弹危险确认（Modal.confirm + okType=danger），
   * 本组件不再硬编码二次确认；onDelete 接收完整 item 对象。
   */
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
                  <Tooltip title={t('inlineList.edit')}>
                    <Button
                      type="text"
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => handleEdit(key)}
                    />
                  </Tooltip>
                  {onDelete && (
                    <Tooltip title={t('inlineList.delete')}>
                      <Button
                        type="text"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => onDelete(item)}
                      />
                    </Tooltip>
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
        <Button type="dashed" block icon={<PlusOutlined />} onClick={handleStartAdd}>
          {addLabel || t('inlineList.add')}
        </Button>
      )}
    </div>
  );
}