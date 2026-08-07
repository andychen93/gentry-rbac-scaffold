import React, { useState } from 'react';
import MenuList from './MenuList';
import type { MenuItem } from '../../types/menu';

export interface PrecisionSidenavProps {
  items: MenuItem[];
  selectedKey?: string;
  openKeys?: string[];
  onSelect: (key: string) => void;
  onOpenChange: (keys: string[]) => void;
  pinned: boolean;
}

const PrecisionSidenav: React.FC<PrecisionSidenavProps> = ({
  items, selectedKey, openKeys, onSelect, onOpenChange, pinned,
}) => {
  const [hovered, setHovered] = useState(false);
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
        <span className="ps-sidenav__brand-text">Precision</span>
      </div>
      <nav aria-label="主导航菜单">
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

export default PrecisionSidenav;
