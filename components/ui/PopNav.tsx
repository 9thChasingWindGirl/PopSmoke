import React, { useRef, useEffect, useState } from 'react';
import { ViewState, Language } from '../../types';
import { PopColorPicker } from './PopColorPicker';
import { POP_COMPONENT_STYLES } from '../../styles/componentStyles';
import { Capacitor } from '@capacitor/core';

interface PopNavProps {
  view: ViewState;
  setView: (view: ViewState) => void;
  settings: {
    language: Language;
    themeColor: string;
  };
  handleUpdateSettings: (settings: any) => void;
  t: any;
  isAndroid: boolean;
}

const PopNav: React.FC<PopNavProps> = ({
  view,
  setView,
  settings,
  handleUpdateSettings,
  t,
  isAndroid
}) => {
  const mobileColorPickerRef = useRef<HTMLDivElement>(null);
  const [showMobileColorPicker, setShowMobileColorPicker] = React.useState(false);
  const [statusBarHeight, setStatusBarHeight] = useState(0);

  useEffect(() => {
    if (isAndroid && Capacitor.isNativePlatform()) {
      setStatusBarHeight(24);
    }
  }, [isAndroid]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (mobileColorPickerRef.current && !mobileColorPickerRef.current.contains(event.target as Node)) {
        setShowMobileColorPicker(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const navStyle = {
    backgroundColor: 'white',
    height: isAndroid ? `${60 + statusBarHeight}px` : '60px',
    paddingTop: isAndroid ? `${statusBarHeight}px` : '0'
  };

  return (
    <div className="w-full max-w-4xl mx-auto px-2 pt-4">
      <nav className={POP_COMPONENT_STYLES.nav.header.container} style={navStyle}>
        <div className={POP_COMPONENT_STYLES.nav.header.title} onClick={() => setView(ViewState.DASHBOARD)}>
          POP<span style={{ color: settings.themeColor }}>SMOKE</span>
        </div>
        
        <div className="flex items-center space-x-2 md:space-x-4">
          {/* 移动端语言和主题颜色选择器 */}
          <div className="flex md:hidden items-center space-x-2">
            {/* 语言选择 */}
            <select
              value={settings.language}
              onChange={(e) => handleUpdateSettings({ ...settings, language: e.target.value as Language })}
              className={POP_COMPONENT_STYLES.nav.header.mobileSelect}
              style={{ backgroundColor: settings.themeColor }}
            >
              <option value="en">EN</option>
              <option value="zh">中</option>
              <option value="ja">日</option>
              <option value="ko">한</option>
            </select>
            
            {/* 主题颜色选择 */}
            <div className="relative" ref={mobileColorPickerRef}>
              <div
                className={POP_COMPONENT_STYLES.nav.header.mobileColorPicker}
                style={{ backgroundColor: settings.themeColor }}
                onClick={() => setShowMobileColorPicker(!showMobileColorPicker)}
              />
              {showMobileColorPicker && (
                <div className="absolute right-0 top-10 bg-white border-4 border-black shadow-pop-lg p-3 z-50 w-[280px]">
                  <PopColorPicker
                    value={settings.themeColor}
                    onChange={(color) => handleUpdateSettings({ ...settings, themeColor: color })}
                    themeColor={settings.themeColor}
                    onClose={() => setShowMobileColorPicker(false)}
                  />
                </div>
              )}
            </div>
          </div>
          
          {/* 桌面端导航 */}
          <div className={POP_COMPONENT_STYLES.nav.header.desktopNav.container}>
            <button onClick={() => setView(ViewState.DASHBOARD)} className={`${POP_COMPONENT_STYLES.nav.header.desktopNav.item} ${view === ViewState.DASHBOARD ? POP_COMPONENT_STYLES.nav.header.desktopNav.active : ''}`}>{t.tracker}</button>
            <button onClick={() => setView(ViewState.ANALYSIS)} className={`${POP_COMPONENT_STYLES.nav.header.desktopNav.item} ${view === ViewState.ANALYSIS ? POP_COMPONENT_STYLES.nav.header.desktopNav.active : ''}`}>{t.analysis}</button>
            <button onClick={() => setView(ViewState.HISTORY)} className={`${POP_COMPONENT_STYLES.nav.header.desktopNav.item} ${view === ViewState.HISTORY ? POP_COMPONENT_STYLES.nav.header.desktopNav.active : ''}`}>{t.history}</button>
            <button onClick={() => setView(ViewState.API)} className={`${POP_COMPONENT_STYLES.nav.header.desktopNav.item} ${view === ViewState.API ? POP_COMPONENT_STYLES.nav.header.desktopNav.active : ''}`}>{t.apiManagement}</button>
            <button onClick={() => setView(ViewState.SETTINGS)} className={`${POP_COMPONENT_STYLES.nav.header.desktopNav.item} ${view === ViewState.SETTINGS ? POP_COMPONENT_STYLES.nav.header.desktopNav.active : ''}`}>{t.settings}</button>
          </div>
        </div>
      </nav>
    </div>
  );
};

export default PopNav;