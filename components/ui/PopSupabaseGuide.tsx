import React, { useState } from 'react';
import { PopCard } from './PopCard';
import { PopButton } from './PopButton';

interface PopSupabaseGuideProps {
  themeColor: string;
  onClose?: () => void;
}

interface GuideStep {
  title: string;
  content: React.ReactNode;
  image?: string;
}

export const PopSupabaseGuide: React.FC<PopSupabaseGuideProps> = ({ themeColor, onClose }) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [showGuide, setShowGuide] = useState(false);

  const steps: GuideStep[] = [
    {
      title: '什么是 Supabase？',
      content: (
        <div className="space-y-3">
          <p className="text-sm">
            Supabase 是一个开源的 Firebase 替代品，提供 PostgreSQL 数据库、身份验证、实时订阅等功能。
          </p>
          <p className="text-sm">
            使用 Supabase 可以让您的吸烟记录数据在多个设备间同步，并在云端安全备份。
          </p>
          <div className="bg-gray-50 border-2 border-black p-3 text-xs">
            <strong>需要准备：</strong>
            <ul className="list-disc list-inside mt-1 space-y-1">
              <li>一个邮箱地址（用于注册 Supabase 账号）</li>
              <li>约 5-10 分钟时间完成配置</li>
            </ul>
          </div>
        </div>
      )
    },
    {
      title: '步骤 1：注册 Supabase 账号',
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li>
              访问 Supabase 官网：
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
            <li>点击右上角的 "Start your project" 或 "Sign In"</li>
            <li>使用 GitHub 账号或邮箱注册/登录</li>
          </ol>
          <div className="bg-yellow-50 border-2 border-black p-2 text-xs">
            💡 <strong>提示：</strong>Supabase 提供免费套餐，足以满足个人使用需求。
          </div>
        </div>
      )
    },
    {
      title: '步骤 2：创建新项目',
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li>登录后点击 "New project" 按钮</li>
            <li>选择或创建一个组织（Organization）</li>
            <li>填写项目信息：
              <ul className="list-disc list-inside ml-4 mt-1 space-y-1 text-xs">
                <li><strong>Name：</strong>项目名称（如 popsmoke-tracker）</li>
                <li><strong>Database Password：</strong>设置数据库密码（请牢记）</li>
                <li><strong>Region：</strong>选择距离您最近的服务器区域</li>
              </ul>
            </li>
            <li>点击 "Create new project" 创建项目</li>
          </ol>
          <div className="bg-blue-50 border-2 border-black p-2 text-xs">
            ⏳ <strong>注意：</strong>项目创建需要 1-2 分钟，请耐心等待。
          </div>
        </div>
      )
    },
    {
      title: '步骤 3：获取 API 凭证',
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li>项目创建完成后，进入项目 Dashboard</li>
            <li>点击左侧菜单中的 "Project Settings"（项目设置）</li>
            <li>选择 "API" 选项卡</li>
            <li>在 "Project API keys" 部分找到以下信息：
              <div className="bg-gray-100 border-2 border-black p-2 mt-2 space-y-2">
                <div>
                  <span className="font-bold text-xs">Project URL：</span>
                  <code className="block bg-white border border-gray-300 p-1 text-xs mt-1 break-all">
                    https://xxxxxxxxxxxxxxxx.supabase.co
                  </code>
                </div>
                <div>
                  <span className="font-bold text-xs">anon public：</span>
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
      title: '步骤 4：配置到本应用',
      content: (
        <div className="space-y-3">
          <ol className="list-decimal list-inside space-y-2 text-sm">
            <li>复制 "Project URL" 到本应用的 <strong>SUPABASE_URL</strong> 字段</li>
            <li>复制 "anon public" 密钥到本应用的 <strong>SUPABASE_ANON_KEY</strong> 字段</li>
            <li>点击保存按钮完成配置</li>
          </ol>
          <div className="bg-green-50 border-2 border-black p-3 text-xs space-y-2">
            <p>✅ <strong>配置完成后，您可以：</strong></p>
            <ul className="list-disc list-inside space-y-1">
              <li>在不同设备间同步吸烟记录</li>
              <li>数据云端备份，防止丢失</li>
              <li>使用云同步功能下载/上传数据</li>
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
        如何获取 Supabase API 凭证？
      </button>
    );
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <PopCard 
        className="w-full max-w-lg max-h-[90vh] overflow-y-auto"
        style={{ borderColor: themeColor }}
      >
        {/* 标题栏 */}
        <div 
          className="flex justify-between items-center border-b-2 border-black pb-3 mb-4"
          style={{ borderColor: themeColor }}
        >
          <h3 className="font-display text-xl font-bold">Supabase 配置指南</h3>
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
            上一步
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
              完成
            </PopButton>
          ) : (
            <PopButton
              variant="primary"
              themeColor={themeColor}
              onClick={handleNext}
            >
              下一步
            </PopButton>
          )}
        </div>
      </PopCard>
    </div>
  );
};

export default PopSupabaseGuide;
