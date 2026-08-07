import React from 'react';
import { useLayoutStore } from '../../stores/layoutStore';

export interface PageShellProps {
  title?: React.ReactNode;
  subTitle?: React.ReactNode;
  extra?: React.ReactNode;
  children: React.ReactNode;
}

const PageShell: React.FC<PageShellProps> = ({ title, subTitle, extra, children }) => {
  const pinned = useLayoutStore((s) => s.sidebarPinned);
  return (
    <div className="ps-page" data-pinned={pinned}>
      {(title || extra) && (
        <div className="ps-page__header">
          <div>
            {subTitle && <div className="ps-page__subtitle">{subTitle}</div>}
            {title && <div className="ps-page__title">{title}</div>}
          </div>
          {extra && <div className="ps-page__extra">{extra}</div>}
        </div>
      )}
      <main className="ps-page__body">{children}</main>
    </div>
  );
};

export default PageShell;
