export const isLightColor = (color: string): boolean => {
  const hex = color.replace('#', '');
  // Handle 3-character hex codes (e.g., #FFF)
  const expandedHex = hex.length === 3 
    ? hex.split('').map(c => c + c).join('') 
    : hex;
  const r = parseInt(expandedHex.slice(0, 2), 16);
  const g = parseInt(expandedHex.slice(2, 4), 16);
  const b = parseInt(expandedHex.slice(4, 6), 16);
  const brightness = (r * 299 + g * 587 + b * 114) / 1000;
  return brightness > 128;
};