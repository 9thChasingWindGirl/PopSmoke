import React from 'react';
import { PopCard } from './PopCard';
import { PopButton } from './PopButton';
import { Language } from '../../types';
import { TRANSLATIONS } from '../../i18n';

interface PopUpdateDialogProps {
  isOpen: boolean;
  currentVersion: string;
  latestVersion: string;
  releaseNotes: string;
  releaseUrl: string;
  language: Language;
  onUpdate: () => void;
  onDismiss: () => void;
}

export const PopUpdateDialog: React.FC<PopUpdateDialogProps> = ({
  isOpen,
  currentVersion,
  latestVersion,
  releaseNotes,
  releaseUrl,
  language,
  onUpdate,
  onDismiss
}) => {
  if (!isOpen) return null;

  const t = TRANSLATIONS[language];

  const formatReleaseNotes = (notes: string): string => {
    if (!notes) return t.noReleaseNotes || 'No release notes available';
    
    const lines = notes.split('\n').filter(line => line.trim());
    if (lines.length <= 5) return notes;
    
    return lines.slice(0, 5).join('\n') + '\n...';
  };

  const handleUpdate = () => {
    if (releaseUrl) {
      window.open(releaseUrl, '_blank');
    }
    onUpdate();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
      <div 
        className="w-full max-w-md transform transition-all"
        style={{ animation: 'popIn 0.3s ease-out' }}
      >
        <PopCard className="relative" style={{ border: '4px solid #000' }}>
          {/* Header */}
          <div className="bg-gradient-to-r from-yellow-400 to-yellow-500 border-b-4 border-black p-4">
            <div className="flex items-center gap-3">
              <div className="text-3xl">🚀</div>
              <div>
                <h2 className="font-display text-xl font-black text-black uppercase tracking-wider">
                  {t.newVersionAvailable || 'New Version Available'}
                </h2>
                <p className="font-body text-sm font-bold text-black/70">
                  v{currentVersion} → v{latestVersion}
                </p>
              </div>
            </div>
          </div>

          {/* Content */}
          <div className="p-6 space-y-4">
            {/* Release Notes */}
            <div className="bg-gray-50 border-2 border-gray-200 rounded-lg p-4 max-h-48 overflow-y-auto">
              <h3 className="font-display text-sm font-bold uppercase text-gray-700 mb-2">
                {t.releaseNotes || "What's New"}
              </h3>
              <pre className="font-body text-xs text-gray-600 whitespace-pre-wrap font-mono leading-relaxed">
                {formatReleaseNotes(releaseNotes)}
              </pre>
            </div>

            {/* Info */}
            <div className="flex items-center gap-2 text-xs text-gray-500">
              <span className="font-body">ℹ️</span>
              <span className="font-body">
                {t.updateHint || 'Updating to the latest version ensures you have the newest features and bug fixes.'}
              </span>
            </div>
          </div>

          {/* Actions */}
          <div className="border-t-4 border-black p-4 flex gap-3">
            <PopButton
              onClick={onDismiss}
              className="flex-1"
              themeColor="#E5E7EB"
            >
              {t.later || 'Later'}
            </PopButton>
            <PopButton
              onClick={handleUpdate}
              className="flex-1"
              themeColor="#FFD700"
            >
              {t.updateNow || 'Update Now'}
            </PopButton>
          </div>
        </PopCard>
      </div>

      <style>{`
        @keyframes popIn {
          0% {
            opacity: 0;
            transform: scale(0.9) translateY(10px);
          }
          100% {
            opacity: 1;
            transform: scale(1) translateY(0);
          }
        }
      `}</style>
    </div>
  );
};
