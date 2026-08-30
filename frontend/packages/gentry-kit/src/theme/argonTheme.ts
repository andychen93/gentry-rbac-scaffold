import type { ThemeConfig } from 'antd';
import { argonColors } from './argonColors';

export const argonTheme: ThemeConfig = {
  token: {
    colorPrimary: argonColors.primary,
    colorInfo: argonColors.info,
    colorSuccess: argonColors.success,
    colorWarning: argonColors.warning,
    colorError: argonColors.danger,
    colorBgLayout: argonColors.bodyBg,
    colorText: argonColors.gray700,
    colorTextHeading: argonColors.gray800,
    colorBorder: argonColors.gray200,
    colorBgContainer: '#fff',
    borderRadius: 6,
    fontFamily: "'Open Sans','PingFang SC',system-ui,sans-serif",
  },
  components: {
    // 顶栏白色（与白色侧栏统一），实际背景/文字由 argon.less .ps-header 主控（!important 覆盖此 token）
    Layout: { siderBg: '#fff', headerBg: '#fff' },
    Menu: {
      itemBg: 'transparent',
      itemSelectedBg: argonColors.gray100,
      itemSelectedColor: argonColors.primary,
      subMenuItemBg: 'transparent',
    },
    Button: {
      fontWeight: 600,
      controlHeight: 38,
      controlHeightSM: 30,
      controlHeightLG: 46,
      paddingInlineSM: 14,
      paddingInline: 20,
      paddingInlineLG: 28,
      borderRadius: 4,
      borderRadiusSM: 4,
      borderRadiusLG: 4,
      boxShadow: '0 4px 6px rgba(50,50,93,.11), 0 1px 3px rgba(0,0,0,.08)',
      primaryShadow: '0 4px 6px rgba(50,50,93,.11), 0 1px 3px rgba(0,0,0,.08)',
      defaultShadow: '0 4px 6px rgba(50,50,93,.11), 0 1px 3px rgba(0,0,0,.08)',
      defaultBg: '#fff',
    },
    Input: {
      controlHeight: 40,
      borderRadius: 4,
      activeBorderColor: argonColors.primary,
      hoverBorderColor: argonColors.primary,
      activeShadow: '0 4px 6px rgba(50,50,93,.11), 0 1px 3px rgba(0,0,0,.08)',
      paddingBlock: 8,
      paddingInline: 14,
    },
    Select: {
      controlHeight: 40,
      borderRadius: 4,
    },
    // DatePicker / RangePicker / TimePicker：与 Input 同款 Argon 风格（40px 高 + 4px 圆角 + hover/focus primary 边框）
    //   Argon 无独立 picker，参考已对齐的 Input（同一组变量 $input-*）
    //   注：DatePicker token 覆盖全部 picker 系（RangePicker/TimePicker/WeekPicker/MonthPicker 同源）
    //       focus 柔和阴影 + cubic-bezier 过渡由 argon.less 补（picker 无 activeShadow 落地点 / 无 transition token）
    DatePicker: {
      controlHeight: 40,
      borderRadius: 4,
      activeBorderColor: argonColors.primary,   // $input-focus-border-color = $primary
      hoverBorderColor: argonColors.primary,    // $input-hover-border-color（Argon 标准 hover = primary）
    },
    Card: {
      borderRadiusLG: 6,
      paddingLG: 20,
    },
    // ===== 阶段三组1：Argon 简单展示组件（取自 core/badges|avatars|alerts|progresses|timeline|collapse|list-groups + custom/_variables.scss）=====
    // Badge：$badge-font-weight=600（其余 uppercase/圆点尺寸由 argon.less 处理）
    Badge: {
      textFontWeight: 600,
    },
    // Tag：list-group-content 文字色 + $gray-100 浅底（圆角由全局 borderRadius=6 → borderRadiusSM=4 自然降一档）
    Tag: {
      defaultBg: argonColors.gray100,
      defaultColor: argonColors.gray700,
    },
    // Avatar：$avatar-* size 表（default 48 / sm 36 / lg 58）+ group 叠加 + groupBorderColor=$card-bg=#fff
    //   注：xs(24) antd v5 无 token，由 less 补；默认 bg/字重 也由 less 补
    Avatar: {
      containerSize: 48,
      containerSizeLG: 58,
      containerSizeSM: 36,
      textFontSize: 16,
      textFontSizeLG: 14,
      textFontSizeSM: 14,
      groupOverlapping: 16,        // Argon _avatar-group: margin-left -1rem
      groupBorderColor: '#fff',    // Argon _avatar-group: 2px solid $card-bg
    },
    // Alert：$alert-padding-y=1rem（其余由 argon.less：左 border 强调 + 标题字号/字重 + 图标间距）
    Alert: {
      defaultPadding: 16,
      withDescriptionPadding: 24,
    },
    // Progress：defaultColor→primary + remainingColor=$progress-bg + lineBorderRadius=$border-radius-sm
    Progress: {
      defaultColor: argonColors.primary,
      remainingColor: argonColors.gray200,
      lineBorderRadius: 4,
    },
    // Timeline：$timeline-axis-width=2 + $timeline-axis-color + $timeline-step-* (圆/白底/边框)
    Timeline: {
      tailColor: argonColors.gray200,
      tailWidth: 2,
      dotBg: '#fff',
      dotBorderWidth: 2,
      itemPaddingBottom: 20,
    },
    // Collapse：accordion 简洁边框风（其余字重/标题色 hover=primary 由 argon.less）
    Collapse: {
      headerBg: 'transparent',
      contentBg: '#fff',
      headerPadding: '12px 16px',
      contentPadding: '16px',
    },
    // ===== 阶段三组2：Argon 交互组件（取自 core/{modals|navs|dropdowns|popovers} + bootstrap/_tooltip）=====
    // Modal：title $h3-font-size=24 + line-height 1.1 + 三段白底（圆角/强阴影/backdrop 由 less）
    Modal: {
      titleFontSize: 24,        // $modal-title-font-size = $h3-font-size ≈ 1.5rem
      titleLineHeight: 1.1,     // $modal-title-line-height
      titleColor: argonColors.gray800,
      contentBg: '#fff',        // $modal-content-bg
      headerBg: '#fff',
      footerBg: '#fff',
    },
    // Tabs：line 模式 active 强调色全 primary + 默认字 gray600（card pill 卡片风由 less）
    Tabs: {
      itemColor: argonColors.gray600,
      itemSelectedColor: argonColors.primary,
      itemHoverColor: argonColors.primary,
      itemActiveColor: argonColors.primary,
      inkBarColor: argonColors.primary,
      titleFontSize: 14,
    },
    // 注：Dropdown/Popover/Tooltip 在 antd v5 暴露的 ComponentToken 不足（无 bg/shadow），
    //   一律在 argon.less 处理；Menu token 已被 Sider 占用，避免污染故不在此追加
    // ===== 阶段三组3：Argon Pagination 精美复刻（取自 core/paginations/_pagination.scss + custom/_variables.scss :904-920）=====
    //   token 负责：基础底色 + 固定 36×36 尺寸（itemSize=36 → antd 注入 min-width/height/line-height）
    //   less 负责：border-radius:50%（antd token 无圆形）+ 边框/文字色 + active box-shadow + 3px 间距 + flex 居中
    //   token itemSize=36 已让 antd 调整 min-width/height/line-height，less 仅补 width=36 强制成正圆
    Pagination: {
      itemBg: '#fff',                          // $pagination-bg
      itemSize: 36,                            // _pagination.scss :19-20 width/height: 36px
      itemSizeSM: 30,                          // _pagination.scss :43 .pagination-sm 30px
      itemActiveBg: argonColors.primary,       // $pagination-active-bg = $component-active-bg = primary #5e72e4
      itemActiveColor: '#fff',                 // $pagination-active-color = $component-active-color = $white
      itemActiveColorHover: '#fff',            // active hover 保持白字（避免回退到 antd 默认 hover）
      itemLinkBg: '#fff',                      // $pagination-bg（prev/next link）
      itemActiveBgDisabled: argonColors.gray200,    // $pagination-disabled-border-color = $gray-300 → 浅底（取 gray-200 更柔和）
      itemActiveColorDisabled: argonColors.gray500, // disabled 浅字
      itemInputBg: '#fff',                     // $pagination-bg（quick jumper input）
    },
    // ===== 阶段三组4：Argon 表单类（取自 core/custom-forms + plugins/_plugin-{nouislider,dropzone} + _variables.scss）=====
    // Checkbox：checked 色 primary（_custom-checkbox.scss + $custom-control-indicator-checked-bg = $component-active-bg）
    //   token 触：colorPrimary(checked)；less 补：hover 边框 + transition
    Checkbox: {
      colorPrimary: argonColors.primary,       // $custom-control-indicator-checked-bg
      borderRadiusSM: 4,                       // $custom-checkbox-indicator-border-radius = $border-radius-sm = .25rem
    },
    // Radio：checked 色 primary（_custom-radio.scss + $custom-control-indicator-checked-bg）
    //   token 触：colorPrimary(checked) + buttonSolidCheckedBg；less 补：hover 边框 + transition
    Radio: {
      colorPrimary: argonColors.primary,       // $custom-control-indicator-checked-bg
      buttonSolidCheckedBg: argonColors.primary,    // Argon button-solid active 实色 primary
      buttonSolidCheckedColor: '#fff',              // color-yiq(primary) = white
      buttonSolidCheckedHoverBg: argonColors.primary,
      buttonSolidCheckedActiveBg: argonColors.primary,
    },
    // InputNumber：与 Input 同款 Argon 风格（focus 柔和阴影 + 40px 高 + 4px 圆角）
    //   Argon 无独立 InputNumber，参考已对齐的 Input（同一组变量 $input-*）
    InputNumber: {
      controlHeight: 40,
      borderRadius: 4,
      activeBorderColor: argonColors.primary,  // $input-focus-border-color = $primary
      hoverBorderColor: argonColors.primary,
      activeShadow: '0 4px 6px rgba(50,50,93,.11), 0 1px 3px rgba(0,0,0,.08)',  // $input-focus-box-shadow
      paddingBlock: 8,
      paddingInline: 14,
    },
    // Slider：Argon noUi 复刻（_plugin-nouislider.scss + $noui-* 变量 :1031-1045）
    //   token 触：railSize/handleSize/handleColor/trackBg；less 补：rail inset 阴影 + track 渐变 + handle 放大
    Slider: {
      railSize: 5,                             // $noui-target-thickness = 5px
      handleSize: 15,                          // $noui-handle-width = 15px
      handleSizeHover: 18,                     // .noUi-handle.noUi-active transform:scale(1.2) ≈ 18px
      handleColor: argonColors.primary,        // $noui-handle-bg = primary
      trackBg: argonColors.primary,            // .noUi-connect bg = theme-color(primary)
      trackHoverBg: argonColors.primary,
      dotBorderColor: argonColors.gray200,     // .noUi-marker #CCC → 浅灰（Argon 灰阶）
      dotActiveBorderColor: argonColors.primary,
    },
    // Upload：antd v5 Upload ComponentToken 仅 actionsColor/duration
    //   Dragger 虚线 + hover 全部由 argon.less 处理（参考 _dropzone.scss / _plugin-dropzone.scss）
    Upload: {
      actionsColor: argonColors.gray600,       // $gray-600 dropzone 文字色（_plugin-dropzone .dz-button color）
    },
    // ===== 阶段三组5：Table/Breadcrumb/Statistic/Steps（取自 core/{tables|breadcrumbs|timeline} + Argon 数据卡风格）=====
    // Table：thead gray-100 底 + gray-600 字（uppercase 由 less）+ body .8125rem + card 包裹风
    //   token 触：headerBg/headerColor/borderColor/rowHoverBg/cellPaddingBlock/Inline/cellFontSize
    //   less 补：thead uppercase + letter-spacing 1px + .65rem 字号 + 字重 600（ProTable 内置 Table 自动受益）
    Table: {
      headerBg: argonColors.gray100,                  // $table-head-bg = $gray-100
      headerColor: argonColors.gray600,               // $table-head-color = $gray-600
      headerSortActiveBg: argonColors.gray100,        // 排序激活保留灰底（与 Argon 表头一致）
      headerSortHoverBg: argonColors.gray100,         // 排序 hover 灰底
      bodySortBg: argonColors.gray100,                // 排序列 body 灰底
      rowHoverBg: argonColors.gray100,                // $gray-100 行 hover（Argon hover 风）
      cellPaddingBlock: 12,                            // $table-head-spacer-y = .75rem
      cellPaddingInline: 16,                           // $table-head-spacer-x = 1rem
      cellFontSize: 13,                                // $table-body-font-size = .8125rem
      borderColor: argonColors.gray200,                // $table-border-color = $gray-200
      headerSplitColor: 'transparent',                 // Argon 表头无 vertical divider
      footerBg: '#fff',                                // $table-footer-bg = $white
      footerColor: argonColors.gray600,                // $table-footer-color = $gray-600
      headerBorderRadius: 0,                           // Argon card 包裹表头无圆角
    },
    // Breadcrumb：itemColor/linkColor/separatorColor gray600 + lastItemColor gray800 + linkHoverColor primary
    //   token 触：itemColor/linkColor/lastItemColor/linkHoverColor/separatorColor
    //   less 补：font-size .875rem + padding 0 + background transparent（Argon .breadcrumb-links）
    Breadcrumb: {
      itemColor: argonColors.gray600,                  // 面包屑项默认色（Argon .breadcrumb-item color）
      linkColor: argonColors.gray600,                  // 链接文字色
      linkHoverColor: argonColors.primary,             // Argon 标准 hover = primary
      lastItemColor: argonColors.gray800,              // 当前页强调 gray800（任务简报指定）
      separatorColor: argonColors.gray600,             // $breadcrumb-divider-color
      separatorMargin: 8,                              // 分隔符两侧 8px 间距
      iconFontSize: 14,                                // $font-size-sm
    },
    // Statistic：Argon 无独立组件，参考数据卡风格（数字 $h1-font-size 大 + 标签 uppercase gray600）
    //   token 触：titleFontSize/contentFontSize
    //   less 补：title uppercase + 字重 600 + content-value 字重 700 + color gray800
    Statistic: {
      titleFontSize: 11,                                // Argon stat-card label 11px uppercase
      contentFontSize: 30,                              // 大数字字号（Argon 数据卡视觉量级）
    },
    // Steps：Argon 无独立组件，参考 timeline 风格（节点圆 33px + 轴线 gray-200 + 完成色 primary）
    //   token 触：iconSize/titleLineHeight/descriptionMaxWidth（antd v5 公开 token 不足，余下靠 less）
    //   less 补：tail 色 + 节点边框 + 标题/描述色
    Steps: {
      iconSize: 32,                                     // Argon $timeline-step width: 33px（antd 32 最接近）
      titleLineHeight: 1.5,                             // Argon 标题行高 1.5
      descriptionMaxWidth: 200,                         // 描述区域 200px（Argon timeline 内容宽）
    },
    // ===== 阶段三组6：Drawer/Result/Empty/Spin（Argon 无直接 scss，按 Argon 视觉语言映射）=====
    // 配色取 argonColors；阴影用 Modal 浮层常量（$modal-content-box-shadow-xs/sm-up）；圆角 6/4；字重 600/700；过渡 cubic-bezier
    //
    // Drawer：Argon 用 Modal 代 Drawer。参考已对齐的 Modal（圆角 7 + Modal 阴影 + mask .16 + gray800 标题）
    //   antd v5 Drawer ComponentToken 极少（仅 zIndexPopup/footerPadding*），全部主要风格靠 less
    //   token 仅补 footer 内边距，让 Argon Modal 节奏（header/footer padding 12-16）一致
    Drawer: {
      footerPaddingBlock: 12,                            // $modal-inner-spacer-y = .75rem
      footerPaddingInline: 16,                           // $modal-inner-spacer-x = 1rem
    },
    // Result：Argon 用 SweetAlert 代 Result。参考 SweetAlert（中央大圆 + 22px 700 标题 + 14px 正文）
    //   antd Result 图标已用 colorSuccess/colorError/colorWarning = success/danger/warning 自动 Argon 化
    //   token 触：iconFontSize/titleFontSize/subtitleFontSize/extraMargin；less 补渐变圆背景
    Result: {
      iconFontSize: 64,                                  // SweetAlert 圆内 40px icon + 圆 80px → antd 默认 72 偏小，取 64 更戏剧（4 字体图标变体配合 less 80px 圆）
      titleFontSize: 22,                                 // SweetAlert h3 = 22px 700
      subtitleFontSize: 14,                              // SweetAlert content = 14px gray-700
      extraMargin: '24px 0 0',                           // extra 按钮区上间距 1.5rem
    },
    // Empty：Argon 无独立组件，按文字简洁风（大图标 gray-400 + gray-600 描述）。
    //   antd v5 Empty ComponentToken 无（无任何字段暴露），全部 less 处理
    // Spin：Argon loader 用 primary 色 + 圆角 + 简洁 size lg/sm（_variables.scss $spinner-* 同款）
    //   antd Spin 默认就用 colorPrimary 渲染 dot（已知），故色由全局 colorPrimary=primary 自动 Argon 化
    //   token 仅触 dotSize 三档：lg/sm/default
    Spin: {
      dotSize: 20,                                       // 默认 spinner 加大到 20px（Argon 标志性显眼 loader）
      dotSizeSM: 14,                                     // sm 保持紧凑
      dotSizeLG: 32,                                     // lg 与 Argon 数据卡 loader 一致
    },
    // ===== 阶段三收尾：Descriptions/List（Argon 无直接 scss，按 Argon 视觉语言映射）=====
    // Descriptions：Argon 无独立组件，参考 Argon 表格/数据卡语言（label gray-100 底 + gray-600 uppercase + 内容 gray-700 + 标题 gray-800 700）
    //   token 触：labelBg=gray100 / labelColor=gray600 / titleColor=gray800 / contentColor=gray700 / extraColor=gray600
    //   less 补：label text-transform uppercase + letter-spacing + 字重 600；title 字重 700
    //   注：colonMarginRight/Left 保留 antd 默认（与字距配合视觉舒适）
    Descriptions: {
      labelBg: argonColors.gray100,                     // Argon label 浅底（与 Table thead gray-100 一致）
      labelColor: argonColors.gray600,                  // $gray-600 label 字色
      titleColor: argonColors.gray800,                  // $headings-color = gray800
      contentColor: argonColors.gray700,                // $body-color = gray700
      extraColor: argonColors.gray600,                  // extra 操作区 gray-600（次要）
      titleMarginBottom: 16,                            // Argon 标题下间距 1rem（更舒展）
    },
    // List：取自 core/list-groups/_list-group.scss + bootstrap/_variables.scss :1018-1039 + Argon _variables.scss override
    //   Argon list-group-item padding-y=.75rem/x=1.25rem（12/20）/ hover-bg=$gray-100 / border=$gray-200 /
    //   action-color=$gray-700 / active-bg=$component-active-bg=primary + 白字
    //   antd v5 List ComponentToken 仅 padding/headerBg/footerBg（无 itemColor/borderColor），余下靠 less
    //   token 触：itemPadding 三档 + headerBg/footerBg transparent（让 header 文字色由 less 处理）
    List: {
      itemPadding: '12px 20px',                         // $list-group-item-padding-y=.75rem / padding-x=1.25rem
      itemPaddingSM: '8px 16px',                        // sm 紧凑档
      itemPaddingLG: '16px 24px',                       // lg 宽松档（接近 Card paddingLG）
      headerBg: 'transparent',                          // Argon list header 无底色（文字灰阶处理）
      footerBg: 'transparent',                          // Argon list footer 无底色
    },
  },
};
