import React, { useState } from 'react';
import MenuList from './MenuList';
import type { MenuItem } from '../../types/menu';
import { APP_NAME } from '../../config/app';
import { useTranslation } from 'react-i18next';

export interface GentrySidenavProps {
  items: MenuItem[];
  selectedKey?: string;
  openKeys?: string[];
  onSelect: (key: string) => void;
  onOpenChange: (keys: string[]) => void;
  pinned: boolean;
}

const GentrySidenav: React.FC<GentrySidenavProps> = ({
  items, selectedKey, openKeys, onSelect, onOpenChange, pinned,
}) => {
  const [hovered, setHovered] = useState(false);
  const { t } = useTranslation('common');
  const mini = !pinned;
  const showHoverOverlay = mini && hovered;

  return (
    <aside
      className={['ps-sidenav', pinned ? 'is-pinned' : 'is-mini', showHoverOverlay ? 'is-hover' : ''].filter(Boolean).join(' ')}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      data-collapsed={mini}
    >
      <div className="ps-sidenav__brand">
        <span className="ps-sidenav__logo">P</span>
        <span className="ps-sidenav__brand-text">{APP_NAME}</span>
      </div>
      <nav aria-label={t('sidebar.nav')}>
        <MenuList
          items={items}
          selectedKey={selectedKey}
          openKeys={mini && !showHoverOverlay ? undefined : openKeys}
          onSelect={onSelect}
          onOpenChange={onOpenChange}
          collapsed={mini && !showHoverOverlay}
        />
      </nav>
    </aside>
  );
};

export default GentrySidenav;
