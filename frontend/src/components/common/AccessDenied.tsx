import React from 'react';
import { Result, Button } from 'antd';
import { useNavigate } from 'react-router-dom';

/**
 * AccessDenied 组件 - 403 无权限提示页面
 *
 * 当用户尝试访问没有权限的页面时显示此组件。
 * 使用 Ant Design Result 组件展示友好的错误提示。
 */
const AccessDenied: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Result
      status="403"
      title="403"
      subTitle="抱歉，您没有权限访问此页面。"
      extra={
        <Button type="primary" onClick={() => navigate(-1)}>
          返回上一页
        </Button>
      }
    />
  );
};

export default AccessDenied;
