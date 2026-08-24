import React from 'react';
import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

/**
 * AccessDenied 组件 - 403 无权限提示页面
 *
 * 当用户尝试访问没有权限的页面时显示此组件。
 * 使用 Ant Design Result 组件展示友好的错误提示。
 */
const AccessDenied: React.FC = () => {
  const { t } = useTranslation('common');
  const navigate = useNavigate();

  return (
    <Result
      status="403"
      title="403"
      subTitle={t('common:accessDenied.subTitle')}
      extra={
        <Button type="primary" onClick={() => navigate(-1)}>
          {t('common:accessDenied.back')}
        </Button>
      }
    />
  );
};

export default AccessDenied;
