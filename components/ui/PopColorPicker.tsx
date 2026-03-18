import React, { useState, useRef, useEffect, useCallback } from 'react';
import type { I18nTranslations } from '../../i18n';

interface PopColorPickerProps {
  value: string;
  onChange: (color: string) => void;
  themeColor?: string;
  onClose?: () => void;
  translations?: I18nTranslations;
}

export const PopColorPicker: React.FC<PopColorPickerProps> = ({ 
  value, 
  onChange, 
  themeColor, 
  onClose,
  translations 
}) => {
  const [inputValue, setInputValue] = useState(value);
  const [showPicker, setShowPicker] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const slBoxRef = useRef<HTMLDivElement>(null);
  const hueSliderRef = useRef<HTMLDivElement>(null);
  
  // Drag state
  const [isDraggingSL, setIsDraggingSL] = useState(false);
  const [isDraggingHue, setIsDraggingHue] = useState(false);

  // Close when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setShowPicker(false);
      }
    };
    if (showPicker) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
    return undefined;
  }, [showPicker]);

  // Parse hex color to RGB
  const hexToRgb = (hex: string): { r: number; g: number; b: number } | null => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : null;
  };

  // Convert RGB to hex
  const rgbToHex = (r: number, g: number, b: number): string => {
    return '#' + [r, g, b].map(x => {
      const hex = Math.max(0, Math.min(255, Math.round(x))).toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    }).join('');
  };

  // Convert RGB to HSL
  const rgbToHsl = (r: number, g: number, b: number): { h: number; s: number; l: number } => {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0, s = 0, l = (max + min) / 2;

    if (max !== min) {
      const d = max - min;
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      switch (max) {
        case r: h = (g - b) / d + (g < b ? 6 : 0); break;
        case g: h = (b - r) / d + 2; break;
        case b: h = (r - g) / d + 4; break;
      }
      h /= 6;
    }
    return { h: h * 360, s: s * 100, l: l * 100 };
  };

  // Convert HSL to RGB
  const hslToRgb = (h: number, s: number, l: number): { r: number; g: number; b: number } => {
    h /= 360; s /= 100; l /= 100;
    let r: number, g: number, b: number;

    if (s === 0) {
      r = g = b = l;
    } else {
      const hue2rgb = (p: number, q: number, t: number) => {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1/6) return p + (q - p) * 6 * t;
        if (t < 1/2) return q;
        if (t < 2/3) return p + (q - p) * (2/3 - t) * 6;
        return p;
      };
      const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
      const p = 2 * l - q;
      r = hue2rgb(p, q, h + 1/3);
      g = hue2rgb(p, q, h);
      b = hue2rgb(p, q, h - 1/3);
    }
    return { r: r * 255, g: g * 255, b: b * 255 };
  };

  const rgb = hexToRgb(value) || { r: 255, g: 215, b: 0 };
  const hsl = rgbToHsl(rgb.r, rgb.g, rgb.b);

  const [hue, setHue] = useState(hsl.h);
  const [saturation, setSaturation] = useState(hsl.s);
  const [lightness, setLightness] = useState(hsl.l);

  // Update HSL when value changes externally
  useEffect(() => {
    const newRgb = hexToRgb(value);
    if (newRgb) {
      const newHsl = rgbToHsl(newRgb.r, newRgb.g, newRgb.b);
      setHue(newHsl.h);
      setSaturation(newHsl.s);
      setLightness(newHsl.l);
    }
    setInputValue(value);
  }, [value]);

  // Handle SL box interaction
  const handleSLInteraction = useCallback((clientX: number, clientY: number) => {
    if (!slBoxRef.current) return;
    const rect = slBoxRef.current.getBoundingClientRect();
    const x = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const y = Math.max(0, Math.min(1, (clientY - rect.top) / rect.height));
    setSaturation(x * 100);
    setLightness((1 - y) * 100);
    const newRgb = hslToRgb(hue, x * 100, (1 - y) * 100);
    const newHex = rgbToHex(newRgb.r, newRgb.g, newRgb.b);
    onChange(newHex);
    setInputValue(newHex);
  }, [hue, onChange]);

  // Handle hue slider interaction
  const handleHueInteraction = useCallback((clientX: number) => {
    if (!hueSliderRef.current) return;
    const rect = hueSliderRef.current.getBoundingClientRect();
    const x = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const newHue = x * 360;
    setHue(newHue);
    const newRgb = hslToRgb(newHue, saturation, lightness);
    const newHex = rgbToHex(newRgb.r, newRgb.g, newRgb.b);
    onChange(newHex);
    setInputValue(newHex);
  }, [saturation, lightness, onChange]);

  // Mouse event handlers for SL box
  const handleSLMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDraggingSL(true);
    handleSLInteraction(e.clientX, e.clientY);
  };

  // Mouse event handlers for hue slider
  const handleHueMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDraggingHue(true);
    handleHueInteraction(e.clientX);
  };

  // Global mouse move and up handlers
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (isDraggingSL) {
        handleSLInteraction(e.clientX, e.clientY);
      }
      if (isDraggingHue) {
        handleHueInteraction(e.clientX);
      }
    };

    const handleMouseUp = () => {
      setIsDraggingSL(false);
      setIsDraggingHue(false);
    };

    if (isDraggingSL || isDraggingHue) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
    return undefined;
  }, [isDraggingSL, isDraggingHue, handleSLInteraction, handleHueInteraction]);

  // Touch event handlers for mobile support
  const handleSLTouchStart = (e: React.TouchEvent) => {
    setIsDraggingSL(true);
    const touch = e.touches[0];
    handleSLInteraction(touch.clientX, touch.clientY);
  };

  const handleSLTouchMove = (e: React.TouchEvent) => {
    if (isDraggingSL) {
      const touch = e.touches[0];
      handleSLInteraction(touch.clientX, touch.clientY);
    }
  };

  const handleSLTouchEnd = () => {
    setIsDraggingSL(false);
  };

  const handleHueTouchStart = (e: React.TouchEvent) => {
    setIsDraggingHue(true);
    const touch = e.touches[0];
    handleHueInteraction(touch.clientX);
  };

  const handleHueTouchMove = (e: React.TouchEvent) => {
    if (isDraggingHue) {
      const touch = e.touches[0];
      handleHueInteraction(touch.clientX);
    }
  };

  const handleHueTouchEnd = () => {
    setIsDraggingHue(false);
  };

  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newText = e.target.value;
    setInputValue(newText);
    if (/^#[0-9A-F]{6}$/i.test(newText)) {
      onChange(newText);
    }
  };

  const handleRgbChange = (component: 'r' | 'g' | 'b', val: number) => {
    const newRgb = { ...rgb, [component]: Math.max(0, Math.min(255, val)) };
    const newHex = rgbToHex(newRgb.r, newRgb.g, newRgb.b);
    onChange(newHex);
    setInputValue(newHex);
  };

  const t = translations || {} as I18nTranslations;

  return (
    <div ref={containerRef} className="w-full relative">
      <div className="flex items-center space-x-2">
        <div 
          className="w-10 h-10 border-2 border-black cursor-pointer"
          style={{ backgroundColor: value }}
          onClick={() => setShowPicker(!showPicker)}
        />
        <input
          type="text"
          value={inputValue}
          onChange={handleTextChange}
          placeholder="#RRGGBB"
          className="flex-1 border-2 border-black px-2 py-1 text-sm font-display"
        />
        <button
          onClick={onClose}
          className="px-3 py-1 border-2 border-black bg-green-500 text-white hover:bg-green-600 transition-colors font-bold"
        >
          ✓
        </button>
      </div>

      {/* Custom Color Picker Popup */}
      {showPicker && (
        <div className="absolute z-50 mt-2 bg-white border-4 border-black shadow-pop-lg p-4 w-[280px]">
          {/* Saturation/Lightness Box */}
          <div 
            ref={slBoxRef}
            className="w-full h-32 mb-3 cursor-crosshair border-2 border-black relative select-none touch-none"
            style={{
              background: `linear-gradient(to right, #fff 0%, hsl(${hue}, 100%, 50%) 100%),
                          linear-gradient(to top, #000 0%, transparent 100%)`,
              backgroundBlendMode: 'multiply'
            }}
            onMouseDown={handleSLMouseDown}
            onTouchStart={handleSLTouchStart}
            onTouchMove={handleSLTouchMove}
            onTouchEnd={handleSLTouchEnd}
          >
            <div 
              className="w-4 h-4 border-2 border-white rounded-full absolute -translate-x-1/2 -translate-y-1/2 shadow-md pointer-events-none"
              style={{
                left: `${saturation}%`,
                top: `${100 - lightness}%`,
                backgroundColor: value
              }}
            />
          </div>

          {/* Hue Slider */}
          <div 
            ref={hueSliderRef}
            className="w-full h-6 mb-3 cursor-pointer border-2 border-black relative select-none touch-none"
            style={{
              background: 'linear-gradient(to right, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100%)'
            }}
            onMouseDown={handleHueMouseDown}
            onTouchStart={handleHueTouchStart}
            onTouchMove={handleHueTouchMove}
            onTouchEnd={handleHueTouchEnd}
          >
            <div 
              className="w-4 h-full border-2 border-white absolute -translate-x-1/2 shadow-md pointer-events-none"
              style={{ left: `${hue / 360 * 100}%`, backgroundColor: `hsl(${hue}, 100%, 50%)` }}
            />
          </div>

          {/* RGB Inputs */}
          <div className="flex items-center space-x-2 mb-3">
            <div className="flex-1">
              <label className="text-xs font-bold block text-center">R</label>
              <input
                type="number"
                min="0"
                max="255"
                value={Math.round(rgb.r)}
                onChange={(e) => handleRgbChange('r', parseInt(e.target.value) || 0)}
                className="w-full border-2 border-black px-1 py-1 text-center text-sm font-display"
              />
            </div>
            <div className="flex-1">
              <label className="text-xs font-bold block text-center">G</label>
              <input
                type="number"
                min="0"
                max="255"
                value={Math.round(rgb.g)}
                onChange={(e) => handleRgbChange('g', parseInt(e.target.value) || 0)}
                className="w-full border-2 border-black px-1 py-1 text-center text-sm font-display"
              />
            </div>
            <div className="flex-1">
              <label className="text-xs font-bold block text-center">B</label>
              <input
                type="number"
                min="0"
                max="255"
                value={Math.round(rgb.b)}
                onChange={(e) => handleRgbChange('b', parseInt(e.target.value) || 0)}
                className="w-full border-2 border-black px-1 py-1 text-center text-sm font-display"
              />
            </div>
          </div>

          {/* Current Color Preview */}
          <div className="flex items-center space-x-2">
            <div 
              className="w-12 h-8 border-2 border-black"
              style={{ backgroundColor: value }}
            />
            <span className="text-sm font-display">{value.toUpperCase()}</span>
          </div>
        </div>
      )}
    </div>
  );
};
