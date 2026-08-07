import React from 'react';
import { APP_NAME } from '../../config/app';

const AppFooter: React.FC = () => (
  <footer className="ps-footer">
    © {new Date().getFullYear()} {APP_NAME}
  </footer>
);

export default AppFooter;
