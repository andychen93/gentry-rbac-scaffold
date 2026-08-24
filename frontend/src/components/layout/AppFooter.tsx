import React from 'react';
import { useTranslation } from 'react-i18next';
import { APP_NAME_KEY } from '../../config/app';

const AppFooter: React.FC = () => {
  const { t } = useTranslation();
  return (
    <footer className="ps-footer">
      © {new Date().getFullYear()} {t(APP_NAME_KEY)}
    </footer>
  );
};

export default AppFooter;
