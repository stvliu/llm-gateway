import { App, theme } from 'antd';
import type { GlobalToken } from 'antd/es/theme/interface';
import { ExclamationCircleFilled, InfoCircleFilled } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ReactNode } from 'react';

/**
 * 确认框场景类型
 * - danger: 不可恢复的危险操作（删除等），红色图标 + 红色按钮
 * - warning: 有风险但可恢复的操作（重置密码、禁用等），橙色图标 + 主题色按钮
 * - info: 常规确认操作（启用、发布等），蓝色图标 + 主题色按钮
 */
export type ConfirmType = 'danger' | 'warning' | 'info';

export interface ConfirmOptions {
  /** 场景类型，决定图标和按钮颜色 */
  type?: ConfirmType;
  /**
   * 标题国际化 key
   * type='danger' 时可选，有 entityName 用 confirm.deleteEntityTitle，无则用 confirm.delete
   */
  title?: string;
  /** 标题国际化参数 */
  titleParams?: Record<string, string>;
  /** 内容国际化 key，type='danger' 时默认 confirm.deleteWarning */
  content?: string;
  /** 内容国际化参数 */
  contentParams?: Record<string, string>;
  /**
   * 删除目标名称，仅 type='danger' 时有效
   * 有值时标题显示 "确定要删除"xxx"吗？"
   */
  entityName?: string;
  /** 确认按钮文字国际化 key，danger 默认 actions.delete，其他默认 actions.confirm */
  okText?: string;
  /** 确认回调 */
  onConfirm: () => void | Promise<void>;
  /** 成功提示国际化 key，danger 默认 message.deleteSuccess，其他默认 message.success */
  successMessage?: string;
}

/**
 * 场景图标配置
 * 使用 Ant Design theme token 确保颜色跟随主题切换
 */
function getIconConfig(type: ConfirmType, token: GlobalToken): { icon: ReactNode; okButtonDanger: boolean } {
  switch (type) {
    case 'danger':
      return {
        icon: <ExclamationCircleFilled style={{ color: token.colorError }} />,
        okButtonDanger: true,
      };
    case 'warning':
      return {
        icon: <ExclamationCircleFilled style={{ color: token.colorWarning }} />,
        okButtonDanger: false,
      };
    case 'info':
      return {
        icon: <InfoCircleFilled style={{ color: token.colorInfo }} />,
        okButtonDanger: false,
      };
  }
}

/**
 * 通用确认框 hook
 *
 * 封装 Ant Design Modal.confirm，支持 danger/warning/info 三种场景类型，
 * 图标和按钮颜色使用主题 Token，自动跟随主题切换。
 */
export function useConfirm() {
  const { modal, message } = App.useApp();
  const { token } = theme.useToken();
  const { t: tc } = useTranslation('common');

  function confirm(options: ConfirmOptions) {
    const type = options.type ?? 'info';
    const { icon, okButtonDanger } = getIconConfig(type, token);

    // danger 场景自动推导标题和内容
    let title: string;
    let content: string | undefined;

    if (type === 'danger') {
      title = options.entityName
        ? tc('confirm.deleteEntityTitle', { name: options.entityName })
        : tc('confirm.delete');
      content = options.content
        ? (options.contentParams ? tc(options.content, options.contentParams) : tc(options.content))
        : tc('confirm.deleteWarning');
    } else {
      title = options.titleParams
        ? tc(options.title!, options.titleParams)
        : tc(options.title!);
      content = options.content
        ? (options.contentParams ? tc(options.content, options.contentParams) : tc(options.content))
        : undefined;
    }

    const defaultOkText = type === 'danger' ? tc('actions.delete') : tc('actions.confirm');
    const defaultSuccessKey = type === 'danger' ? 'message.deleteSuccess' : 'message.success';
    const errorKey = type === 'danger' ? 'message.deleteFailed' : 'message.error';

    modal.confirm({
      title,
      content,
      icon,
      okText: options.okText ?? defaultOkText,
      cancelText: tc('actions.cancel'),
      okButtonProps: { danger: okButtonDanger, type: 'primary' as const },
      centered: true,
      onOk: async () => {
        try {
          await options.onConfirm();
          message.success(options.successMessage ? tc(options.successMessage) : tc(defaultSuccessKey));
        } catch {
          message.error(tc(errorKey));
        }
      },
    });
  }

  return { confirm };
}