/**
 * 应用级品牌配置。
 *
 * **系统全名不在这里，在语言包里**：`locales/{zh-CN,en-US}/common.json` 的 `app.name`。
 * 品牌名一旦写成常量，英文界面就会露出中文（探针在登录页抓到过），所以它和其他
 * 界面文案一样走 i18n；本文件只留与语言无关的那部分。
 *
 * 新项目改名字：改两处 `common.json` 的 `app.name` + `APP_INITIAL` + `index.html` 的 `<title>`。
 * 引用位置：Logo（侧栏）、GentrySidenav、LoginPage（登录页标题）、AppFooter（页脚）。
 */

/** 系统全名的 i18n key（namespace 为 `common`） */
export const APP_NAME_KEY = 'app.name';

/** Logo 圆形图标里的单字符缩写。与语言无关，留在这里 */
export const APP_INITIAL = 'R';
