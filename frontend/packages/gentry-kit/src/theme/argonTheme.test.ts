import { describe, it, expect } from 'vitest';
import { argonTheme } from './argonTheme';
import { argonColors } from './argonColors';

describe('argonTheme', () => {
  it('colorPrimary 映射 Argon primary', () => {
    expect(argonTheme.token?.colorPrimary).toBe(argonColors.primary);
  });
  it('colorBgLayout = bodyBg', () => {
    expect(argonTheme.token?.colorBgLayout).toBe(argonColors.bodyBg);
  });
  it('borderRadius = 6', () => {
    expect(argonTheme.token?.borderRadius).toBe(6);
  });
  // 顶栏与侧栏统一为浅色（argon.less 中 .ps-header background 也是 #fff）
  it('Layout headerBg / siderBg = 白色', () => {
    expect(argonTheme.components?.Layout?.headerBg).toBe('#fff');
    expect(argonTheme.components?.Layout?.siderBg).toBe('#fff');
  });

  // 阶段三组1：7 个简单展示组件 token 锁定
  describe('阶段三组1：Argon 展示组件', () => {
    it('Badge 字重 600（$badge-font-weight）', () => {
      expect(argonTheme.components?.Badge?.textFontWeight).toBe(600);
    });
    it('Tag 默认底/字色取自 Argon', () => {
      expect(argonTheme.components?.Tag?.defaultBg).toBe(argonColors.gray100);
      expect(argonTheme.components?.Tag?.defaultColor).toBe(argonColors.gray700);
    });
    it('Avatar 三档尺寸取自 $avatar-*（48/58/36）+ group 叠加', () => {
      expect(argonTheme.components?.Avatar?.containerSize).toBe(48);
      expect(argonTheme.components?.Avatar?.containerSizeLG).toBe(58);
      expect(argonTheme.components?.Avatar?.containerSizeSM).toBe(36);
      expect(argonTheme.components?.Avatar?.groupOverlapping).toBe(16);
      expect(argonTheme.components?.Avatar?.groupBorderColor).toBe('#fff');
    });
    it('Alert 内边距对齐 $alert-padding-y=1rem', () => {
      expect(argonTheme.components?.Alert?.defaultPadding).toBe(16);
      expect(argonTheme.components?.Alert?.withDescriptionPadding).toBe(24);
    });
    it('Progress 默认色 = primary + 轨道色 = gray200', () => {
      expect(argonTheme.components?.Progress?.defaultColor).toBe(argonColors.primary);
      expect(argonTheme.components?.Progress?.remainingColor).toBe(argonColors.gray200);
      expect(argonTheme.components?.Progress?.lineBorderRadius).toBe(4);
    });
    it('Timeline 轴线 2px + 节点白底/2px 边框', () => {
      expect(argonTheme.components?.Timeline?.tailColor).toBe(argonColors.gray200);
      expect(argonTheme.components?.Timeline?.tailWidth).toBe(2);
      expect(argonTheme.components?.Timeline?.dotBg).toBe('#fff');
      expect(argonTheme.components?.Timeline?.dotBorderWidth).toBe(2);
    });
    it('Collapse 头部透明 + 内容白底', () => {
      expect(argonTheme.components?.Collapse?.headerBg).toBe('transparent');
      expect(argonTheme.components?.Collapse?.contentBg).toBe('#fff');
    });
  });

  // 阶段三组2：5 个交互组件 token 锁定（Modal/Tabs；Dropdown/Popover/Tooltip 因 antd v5 暴露 token 不足由 less 处理）
  describe('阶段三组2：Argon 交互组件', () => {
    it('Modal 标题 $h3-font-size=24 + line-height 1.1 + 三段白底', () => {
      expect(argonTheme.components?.Modal?.titleFontSize).toBe(24);
      expect(argonTheme.components?.Modal?.titleLineHeight).toBe(1.1);
      expect(argonTheme.components?.Modal?.titleColor).toBe(argonColors.gray800);
      expect(argonTheme.components?.Modal?.contentBg).toBe('#fff');
      expect(argonTheme.components?.Modal?.headerBg).toBe('#fff');
      expect(argonTheme.components?.Modal?.footerBg).toBe('#fff');
    });
    it('Tabs line 模式 active 强调色全 primary + 默认字 gray600', () => {
      expect(argonTheme.components?.Tabs?.itemColor).toBe(argonColors.gray600);
      expect(argonTheme.components?.Tabs?.itemSelectedColor).toBe(argonColors.primary);
      expect(argonTheme.components?.Tabs?.itemHoverColor).toBe(argonColors.primary);
      expect(argonTheme.components?.Tabs?.itemActiveColor).toBe(argonColors.primary);
      expect(argonTheme.components?.Tabs?.inkBarColor).toBe(argonColors.primary);
      expect(argonTheme.components?.Tabs?.titleFontSize).toBe(14);
    });
  });

  // 阶段三组5：Table/Breadcrumb/Statistic/Steps token 锁定（核心色由 token，差额由 less）
  describe('阶段三组5：Table/Breadcrumb/Statistic/Steps', () => {
    it('Table 表头底/字色/字号/内边距对齐 $table-*（ProTable 内置 Table 自动受益）', () => {
      expect(argonTheme.components?.Table?.headerBg).toBe(argonColors.gray100);     // $table-head-bg
      expect(argonTheme.components?.Table?.headerColor).toBe(argonColors.gray600);  // $table-head-color
      expect(argonTheme.components?.Table?.borderColor).toBe(argonColors.gray200);  // $table-border-color
      expect(argonTheme.components?.Table?.rowHoverBg).toBe(argonColors.gray100);
      expect(argonTheme.components?.Table?.cellPaddingBlock).toBe(12);              // $table-head-spacer-y=.75rem
      expect(argonTheme.components?.Table?.cellPaddingInline).toBe(16);             // $table-head-spacer-x=1rem
      expect(argonTheme.components?.Table?.cellFontSize).toBe(13);                  // $table-body-font-size=.8125rem
      expect(argonTheme.components?.Table?.headerSplitColor).toBe('transparent');   // Argon 无 vertical divider
    });
    it('Breadcrumb 5 色对齐 Argon（item/link/separator gray600 + last gray800 + hover primary）', () => {
      expect(argonTheme.components?.Breadcrumb?.itemColor).toBe(argonColors.gray600);
      expect(argonTheme.components?.Breadcrumb?.linkColor).toBe(argonColors.gray600);
      expect(argonTheme.components?.Breadcrumb?.linkHoverColor).toBe(argonColors.primary);
      expect(argonTheme.components?.Breadcrumb?.lastItemColor).toBe(argonColors.gray800);
      expect(argonTheme.components?.Breadcrumb?.separatorColor).toBe(argonColors.gray600);
    });
    it('Statistic 标题 11 / 内容 30（数字大字号 + Argon 数据卡视觉量级）', () => {
      expect(argonTheme.components?.Statistic?.titleFontSize).toBe(11);
      expect(argonTheme.components?.Statistic?.contentFontSize).toBe(30);
    });
    it('Steps 节点 32 + 标题行高 1.5 + 描述宽 200（Argon $timeline-step 33px 近似）', () => {
      expect(argonTheme.components?.Steps?.iconSize).toBe(32);
      expect(argonTheme.components?.Steps?.titleLineHeight).toBe(1.5);
      expect(argonTheme.components?.Steps?.descriptionMaxWidth).toBe(200);
    });
  });

  // 阶段三组6：Drawer/Result/Spin token 锁定（Empty antd v5 无 ComponentToken 字段，全靠 less）
  describe('阶段三组6：Drawer/Result/Spin（Argon 视觉语言映射）', () => {
    it('Drawer footer 内边距对齐 Modal spacer（12/16）', () => {
      expect(argonTheme.components?.Drawer?.footerPaddingBlock).toBe(12);
      expect(argonTheme.components?.Drawer?.footerPaddingInline).toBe(16);
    });
    it('Result 三档字号对齐 SweetAlert（icon 64 / title 22 / subtitle 14）', () => {
      expect(argonTheme.components?.Result?.iconFontSize).toBe(64);
      expect(argonTheme.components?.Result?.titleFontSize).toBe(22);
      expect(argonTheme.components?.Result?.subtitleFontSize).toBe(14);
      expect(argonTheme.components?.Result?.extraMargin).toBe('24px 0 0');
    });
    it('Spin 三档 dotSize 对齐 Argon loader（default 20 / sm 14 / lg 32）', () => {
      expect(argonTheme.components?.Spin?.dotSize).toBe(20);
      expect(argonTheme.components?.Spin?.dotSizeSM).toBe(14);
      expect(argonTheme.components?.Spin?.dotSizeLG).toBe(32);
    });
  });

  // 阶段三收尾：Descriptions/List token 锁定（核心色/间距由 token，差额由 less）
  describe('阶段三收尾：Descriptions/List', () => {
    it('Descriptions label/title/content 色对齐 Argon 数据卡语言（gray100/600/800/700）', () => {
      expect(argonTheme.components?.Descriptions?.labelBg).toBe(argonColors.gray100);
      expect(argonTheme.components?.Descriptions?.labelColor).toBe(argonColors.gray600);
      expect(argonTheme.components?.Descriptions?.titleColor).toBe(argonColors.gray800);
      expect(argonTheme.components?.Descriptions?.contentColor).toBe(argonColors.gray700);
      expect(argonTheme.components?.Descriptions?.extraColor).toBe(argonColors.gray600);
      expect(argonTheme.components?.Descriptions?.titleMarginBottom).toBe(16);
    });
    it('List 三档 itemPadding 对齐 $list-group-item-padding（12/20、sm 8/16、lg 16/24）', () => {
      expect(argonTheme.components?.List?.itemPadding).toBe('12px 20px');
      expect(argonTheme.components?.List?.itemPaddingSM).toBe('8px 16px');
      expect(argonTheme.components?.List?.itemPaddingLG).toBe('16px 24px');
      expect(argonTheme.components?.List?.headerBg).toBe('transparent');
      expect(argonTheme.components?.List?.footerBg).toBe('transparent');
    });
  });
});
