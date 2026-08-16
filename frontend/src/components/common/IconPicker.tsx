import { useState, useMemo } from 'react';
import { Input, Popover, Empty, theme } from 'antd';
import * as Icons from '@ant-design/icons';

/** 可选图标列表（常用 Ant Design 图标） */
const ICON_LIST: string[] = [
  // 方向类
  'UpOutlined', 'DownOutlined', 'LeftOutlined', 'RightOutlined',
  'ArrowUpOutlined', 'ArrowDownOutlined', 'ArrowLeftOutlined', 'ArrowRightOutlined',
  // 建议类
  'QuestionCircleOutlined', 'InfoCircleOutlined', 'CheckCircleOutlined', 'CloseCircleOutlined',
  'ExclamationCircleOutlined', 'WarningOutlined',
  // 编辑类
  'EditOutlined', 'FormOutlined', 'CopyOutlined', 'ScissorOutlined', 'DeleteOutlined',
  'HighlightOutlined', 'AlignLeftOutlined',
  // 数据类
  'AreaChartOutlined', 'PieChartOutlined', 'BarChartOutlined', 'LineChartOutlined',
  'FundOutlined', 'StockOutlined',
  // 通用
  'HomeOutlined', 'SettingOutlined', 'UserOutlined', 'TeamOutlined',
  'AppstoreOutlined', 'ClusterOutlined', 'ApartmentOutlined',
  'SafetyOutlined', 'LockOutlined', 'UnlockOutlined',
  'FileOutlined', 'FileTextOutlined', 'FolderOutlined', 'FolderOpenOutlined',
  'CloudOutlined', 'DesktopOutlined', 'MobileOutlined', 'TabletOutlined',
  'DatabaseOutlined', 'GlobalOutlined', 'ToolOutlined', 'BugOutlined',
  'CodeOutlined', 'ApiOutlined', 'ThunderboltOutlined', 'RocketOutlined',
  'BellOutlined', 'CalendarOutlined', 'CameraOutlined', 'CarOutlined',
  'ShopOutlined', 'ShoppingCartOutlined', 'GiftOutlined', 'IdcardOutlined',
  'MailOutlined', 'MessageOutlined', 'PhoneOutlined', 'PrinterOutlined',
  'ReadOutlined', 'ScheduleOutlined', 'SearchOutlined', 'SoundOutlined',
  'StarOutlined', 'TagOutlined', 'TagsOutlined', 'TrophyOutlined',
  'VideoCameraOutlined', 'WalletOutlined', 'WifiOutlined',
  'DashboardOutlined', 'MenuOutlined', 'OrderedListOutlined', 'UnorderedListOutlined',
  'ProfileOutlined', 'TableOutlined', 'PictureOutlined', 'SmileOutlined',
  'HeartOutlined', 'EnvironmentOutlined', 'EyeOutlined', 'EyeInvisibleOutlined',
  'FilterOutlined', 'FireOutlined', 'FlagOutlined', 'KeyOutlined',
  'CompassOutlined', 'CrownOutlined', 'CustomerServiceOutlined',
  'SolutionOutlined', 'SyncOutlined', 'ReloadOutlined',
  'PlusOutlined', 'MinusOutlined', 'CloseOutlined', 'CheckOutlined',
  'PoweroffOutlined', 'LoginOutlined', 'LogoutOutlined',
  'PlusCircleOutlined', 'MinusCircleOutlined',
  'UploadOutlined', 'DownloadOutlined', 'CloudUploadOutlined', 'CloudDownloadOutlined',
  'SaveOutlined', 'ImportOutlined', 'ExportOutlined',
];

/** 根据图标名称获取图标组件 */
const getIconComponent = (name: string) => {
  const IconComp = (Icons as Record<string, any>)[name];
  return IconComp ? <IconComp /> : null;
};

interface IconPickerProps {
  value?: string | null;
  onChange?: (icon: string | null) => void;
}

export default function IconPicker({ value, onChange }: IconPickerProps) {
  const { token } = theme.useToken();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');

  const filteredIcons = useMemo(() => {
    if (!search) return ICON_LIST;
    const lower = search.toLowerCase();
    return ICON_LIST.filter((name) => name.toLowerCase().includes(lower));
  }, [search]);

  const handleSelect = (iconName: string) => {
    onChange?.(iconName);
    setOpen(false);
    setSearch('');
  };

  const handleClear = () => {
    onChange?.(null);
  };

  const content = (
    <div style={{ width: 360 }}>
      <Input
        placeholder="搜索图标"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        allowClear
        style={{ marginBottom: 8 }}
      />
      <div style={{ maxHeight: 280, overflowY: 'auto' }}>
        {filteredIcons.length === 0 ? (
          <Empty description="无匹配图标" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: 4 }}>
            {filteredIcons.map((name) => (
              <div
                key={name}
                onClick={() => handleSelect(name)}
                title={name}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 36,
                  height: 36,
                  fontSize: 18,
                  cursor: 'pointer',
                  borderRadius: 4,
                  border:
                    value === name
                      ? `2px solid ${token.colorPrimary}`
                      : `1px solid ${token.colorBorderSecondary}`,
                  // controlItemBgActive 是 antd「列表项选中底色」语义，跟着主色走
                  background: value === name ? token.controlItemBgActive : token.colorBgContainer,
                }}
              >
                {getIconComponent(name)}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );

  return (
    <Popover
      content={content}
      trigger="click"
      open={open}
      onOpenChange={setOpen}
      placement="bottomLeft"
    >
      <Input
        readOnly
        value={value || ''}
        placeholder="点击选择图标"
        prefix={value ? getIconComponent(value) : undefined}
        allowClear
        onClear={handleClear}
        style={{ cursor: 'pointer' }}
      />
    </Popover>
  );
}
