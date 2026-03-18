import React, { useRef } from 'react';
import { ViewState, Language } from '../../types';
import { PopColorPicker } from './PopColorPicker';

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

  return (
    <div className="w-full max-w-4xl mx-auto px-2 pt-4">
      <nav className={`p-3 md:p-4 flex justify-between items-center bg-white border-4 border-black shrink-0 z-50 ${isAndroid ? 'h-[100px] pt-12' : 'h-[60px]'}`} style={{ backgroundColor: 'white' }}>
        <div className="font-display text-xl md:text-2xl tracking-wide cursor-pointer" onClick={() => setView(ViewState.DASHBOARD)}>
          POP<span style={{ color: settings.themeColor }}>SMOKE</span>
        </div>
        
        <div className="flex items-center space-x-2 md:space-x-4">
          {/* 移动端语言和主题颜色选择器 */}
          <div className="flex md:hidden items-center space-x-2">
            {/* 语言选择 */}
            <select
              value={settings.language}
              onChange={(e) => handleUpdateSettings({ ...settings, language: e.target.value as Language })}
              className="border-2 border-black px-1 py-1 text-sm font-bold cursor-pointer"
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
                className="w-8 h-8 border-2 border-black cursor-pointer"
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
          <div className="hidden md:flex items-center space-x-6">
            <button onClick={() => setView(ViewState.DASHBOARD)} className={`font-bold uppercase border-b-2 border-transparent hover:border-black ${view === ViewState.DASHBOARD ? 'border-black' : ''}`} style={{ width: '100px', textAlign: 'center' }}>{t.tracker}</button>
            <button onClick={() => setView(ViewState.ANALYSIS)} className={`font-bold uppercase border-b-2 border-transparent hover:border-black ${view === ViewState.ANALYSIS ? 'border-black' : ''}`} style={{ width: '100px', textAlign: 'center' }}>{t.analysis}</button>
            <button onClick={() => setView(ViewState.HISTORY)} className={`font-bold uppercase border-b-2 border-transparent hover:border-black ${view === ViewState.HISTORY ? 'border-black' : ''}`} style={{ width: '100px', textAlign: 'center' }}>{t.history}</button>
            <button onClick={() => setView(ViewState.API)} className={`font-bold uppercase border-b-2 border-transparent hover:border-black ${view === ViewState.API ? 'border-black' : ''}`} style={{ width: '100px', textAlign: 'center' }}>{t.apiManagement}</button>
            <button onClick={() => setView(ViewState.SETTINGS)} className={`font-bold uppercase border-b-2 border-transparent hover:border-black ${view === ViewState.SETTINGS ? 'border-black' : ''}`} style={{ width: '100px', textAlign: 'center' }}>{t.settings}</button>
          </div>
        </div>
      </nav>
    </div>
  );
};

export default PopNav;