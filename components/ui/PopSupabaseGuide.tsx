import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { PopCard } from './PopCard';
import { PopButton } from './PopButton';
import { isLightColor } from '../../utils/colorUtils';
import { TRANSLATIONS } from '../../i18n';
import { Language } from '../../types';
import { POP_DESIGN_SYSTEM } from '../../styles/designSystem';

interface PopSupabaseGuideProps {
  themeColor: string;
  language?: string;
  onClose?: () => void;
}

interface GuideStep {
  title: string;
  content: React.ReactNode;
  image?: string;
}

export const PopSupabaseGuide: React.FC<PopSupabaseGuideProps> = ({ themeColor, language = 'en', onClose }) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [showGuide, setShowGuide] = useState(false);

  const textColor = isLightColor(themeColor) ? '#000000' : '#FFFFFF';
  
  const t = TRANSLATIONS[language as Language] || TRANSLATIONS.en;

  const steps: GuideStep[] = [
    {
      title: t.whatIsSupabase,
      content: (
        <div className="space-y-3">
          <p className="text-sm">
            {t.supabaseDescription}
          </p>
          <p className="text-sm">
            {t.supabaseUseCase}
          </p>
          <div className="bg-gray-50 border-2 border-black p-3 text-xs">
            <strong>{t.supabaseRequirements}</strong>
            <ul className="list-disc list-inside mt-1 space-y-1">
              <li>{t.supabaseEmail}</li>
              <li>{t.supabaseTime}</li>
            </ul>
          </div>
        </div>
      )
    },
    {
      title: t.supabaseStep1,
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li>
              {t.supabaseStep1Desc1}
              <a 
                href="https://supabase.com" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-blue-600 underline font-bold"
                style={{ color: themeColor }}
              >
                https://supabase.com
              </a>
            </li>
            <li>{t.supabaseStep1Desc2}</li>
            <li>{t.supabaseStep1Desc3}</li>
          </ol>
          <div className="bg-yellow-50 border-2 border-black p-2 text-xs">
            {t.supabaseTip}
          </div>
        </div>
      )
    },
    {
      title: t.supabaseStep2,
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li>{t.supabaseStep2Desc1}</li>
            <li>{t.supabaseStep2Desc2}</li>
            <li>{t.supabaseStep2Desc3}
              <ul className="list-disc list-inside ml-4 mt-1 space-y-1 text-xs">
                <li><strong>{t.supabaseStep2Desc4}</strong></li>
                <li><strong>{t.supabaseStep2Desc5}</strong></li>
                <li><strong>{t.supabaseStep2Desc6}</strong></li>
              </ul>
            </li>
            <li>{t.supabaseStep2Desc7}</li>
          </ol>
          <div className="bg-blue-50 border-2 border-black p-2 text-xs">
            {t.supabaseNote}
          </div>
        </div>
      )
    },
    {
      title: t.supabaseStep3,
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li>{t.supabaseStep3Desc1}</li>
            <li>{t.supabaseStep3Desc2}</li>
            <li>{t.supabaseStep3Desc3}</li>
            <li>{t.supabaseStep3Desc4}
              <div className="bg-gray-100 border-2 border-black p-2 mt-2 space-y-2">
                <div>
                  <span className="font-bold text-xs">{t.supabaseProjectUrl}</span>
                  <code className="block bg-white border border-gray-300 p-1 text-xs mt-1 break-all">
                    https://xxxxxxxxxxxxxxxx.supabase.co
                  </code>
                </div>
                <div>
                  <span className="font-bold text-xs">{t.supabaseAnonKey}</span>
                  <code className="block bg-white border border-gray-300 p-1 text-xs mt-1 break-all">
                    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                  </code>
                </div>
              </div>
            </li>
          </ol>
        </div>
      )
    },
    {
      title: t.supabaseStep4,
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li dangerouslySetInnerHTML={{ __html: t.supabaseStep4Desc1 }} />
            <li dangerouslySetInnerHTML={{ __html: t.supabaseStep4Desc2 }} />
            <li>{t.supabaseStep4Desc3}</li>
          </ol>
          <div className="bg-green-50 border-2 border-black p-3 text-xs space-y-2">
            <p dangerouslySetInnerHTML={{ __html: t.supabaseAfterConfig }} />
            <ul className="list-disc list-inside space-y-1">
              <li>{t.supabaseSyncDevices}</li>
              <li>{t.supabaseCloudBackup}</li>
              <li>{t.supabaseCloudSync}</li>
            </ul>
          </div>
        </div>
      )
    }
  ];

  const handleNext = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handlePrev = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleClose = () => {
    setShowGuide(false);
    setCurrentStep(0);
    onClose?.();
  };

  if (!showGuide) {
    return (
      <button
        onClick={() => setShowGuide(true)}
        className="text-sm underline cursor-pointer hover:opacity-70"
        style={{ color: themeColor }}
      >
        {t.howToGetSupabaseApi}
      </button>
    );
  }

  const modalContent = (
    <div 
      className="fixed bg-black bg-opacity-50 flex items-center justify-center"
      style={{ 
        top: 0, 
        left: 0, 
        width: '100vw', 
        height: '100vh', 
        zIndex: POP_DESIGN_SYSTEM.zIndex.modal 
      }}
    >
      <PopCard 
        className="w-full max-w-lg max-h-[90vh] overflow-y-auto"
        style={{ borderColor: themeColor }}
      >
        {/* 标题栏 */}
        <div 
          className="flex justify-between items-center border-b-2 border-black pb-3 mb-4"
          style={{ borderColor: themeColor }}
        >
          <h3 className="font-display text-xl font-bold">{t.supabaseGuideTitle}</h3>
          <button
            onClick={handleClose}
            className="text-2xl font-bold hover:opacity-70"
          >
            ×
          </button>
        </div>

        {/* 进度指示器 */}
        <div className="flex justify-center space-x-2 mb-4">
          {steps.map((_, index) => (
            <div
              key={index}
              className={`w-3 h-3 border-2 border-black ${
                index === currentStep ? 'bg-black' : 'bg-white'
              }`}
              style={index === currentStep ? { backgroundColor: themeColor, borderColor: themeColor } : {}}
            />
          ))}
        </div>

        {/* 步骤标题 */}
        <h4 className="font-bold text-lg mb-3" style={{ color: themeColor }}>
          {steps[currentStep].title}
        </h4>

        {/* 步骤内容 */}
        <div className="mb-6">
          {steps[currentStep].content}
        </div>

        {/* 导航按钮 */}
        <div className="flex justify-between pt-4 border-t-2 border-gray-200">
          <PopButton
            variant="secondary"
            themeColor={themeColor}
            onClick={handlePrev}
            disabled={currentStep === 0}
          >
            {t.supabasePrev}
          </PopButton>
          
          <div className="text-sm text-gray-500 self-center">
            {currentStep + 1} / {steps.length}
          </div>
          
          {currentStep === steps.length - 1 ? (
            <PopButton
              variant="primary"
              themeColor={themeColor}
              onClick={handleClose}
            >
              {t.supabaseComplete}
            </PopButton>
          ) : (
            <PopButton
              variant="primary"
              themeColor={themeColor}
              onClick={handleNext}
            >
              {t.supabaseNext}
            </PopButton>
          )}
        </div>
      </PopCard>
    </div>
  );

  return createPortal(modalContent, document.body);
};

export default PopSupabaseGuide;
