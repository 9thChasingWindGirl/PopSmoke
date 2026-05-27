import { Preferences } from '@capacitor/preferences';
import { systemLogService } from './systemLogService';

interface VersionInfo {
  currentVersion: string;
  latestVersion: string;
  releaseUrl: string;
  releaseNotes: string;
  publishedAt: string;
  isForceUpdate: boolean;
}

interface GitHubRelease {
  tag_name: string;
  name: string;
  body: string;
  html_url: string;
  published_at: string;
  prerelease: boolean;
}

const VERSION_CHECK_KEY = 'last_version_check';
const VERSION_CHECK_INTERVAL = 24 * 60 * 60 * 1000; // 24小时
const GITHUB_API_URL = 'https://api.github.com/repos/your-username/PopSmoke/releases/latest';

function parseVersion(version: string): number[] {
  return version.replace('v', '').split('.').map(Number);
}

function compareVersions(v1: string, v2: string): number {
  const parts1 = parseVersion(v1);
  const parts2 = parseVersion(v2);
  
  for (let i = 0; i < Math.max(parts1.length, parts2.length); i++) {
    const num1 = parts1[i] || 0;
    const num2 = parts2[i] || 0;
    
    if (num1 > num2) return 1;
    if (num1 < num2) return -1;
  }
  
  return 0;
}

export async function getCurrentVersion(): Promise<string> {
  try {
    const response = await fetch('/metadata.json');
    const metadata = await response.json();
    return metadata.version || '0.0.0';
  } catch {
    return '0.0.0';
  }
}

async function shouldCheckForUpdate(): Promise<boolean> {
  try {
    const { value } = await Preferences.get({ key: VERSION_CHECK_KEY });
    if (!value) return true;
    
    const lastCheck = parseInt(value, 10);
    const now = Date.now();
    
    return (now - lastCheck) >= VERSION_CHECK_INTERVAL;
  } catch {
    return true;
  }
}

async function markVersionChecked(): Promise<void> {
  try {
    await Preferences.set({
      key: VERSION_CHECK_KEY,
      value: Date.now().toString()
    });
  } catch (error) {
    systemLogService.warn('ui', 'Failed to save version check timestamp');
  }
}

async function fetchLatestRelease(): Promise<GitHubRelease | null> {
  try {
    const response = await fetch(GITHUB_API_URL, {
      headers: {
        'Accept': 'application/vnd.github.v3+json'
      },
      signal: AbortSignal.timeout(10000)
    });

    if (!response.ok) {
      if (response.status === 404) {
        systemLogService.info('ui', 'No releases found in repository');
      } else if (response.status === 403) {
        systemLogService.warn('ui', 'GitHub API rate limited');
      } else {
        systemLogService.error('ui', `Failed to fetch release: ${response.status}` as any);
      }
      return null;
    }

    const data: GitHubRelease = await response.json();
    return data;
  } catch (error) {
    systemLogService.error('ui', 'Failed to fetch latest release', error as Error);
    return null;
  }
}

export async function checkForUpdate(): Promise<VersionInfo | null> {
  try {
    const shouldCheck = await shouldCheckForUpdate();
    if (!shouldCheck) {
      systemLogService.debug('ui', 'Skipping version check - checked recently');
      return null;
    }

    const currentVersion = await getCurrentVersion();
    const release = await fetchLatestRelease();
    
    if (!release || release.prerelease) {
      await markVersionChecked();
      return null;
    }

    const comparison = compareVersions(currentVersion, release.tag_name);
    
    if (comparison < 0) {
      const versionInfo: VersionInfo = {
        currentVersion,
        latestVersion: release.tag_name,
        releaseUrl: release.html_url,
        releaseNotes: release.body || '',
        publishedAt: release.published_at,
        isForceUpdate: false
      };

      systemLogService.info('ui', `New version available: ${release.tag_name}`);
      await markVersionChecked();
      
      return versionInfo;
    }

    systemLogService.debug('ui', `Application is up to date: ${currentVersion}`);
    await markVersionChecked();
    return null;
  } catch (error) {
    systemLogService.error('ui', 'Error checking for updates', error as Error);
    return null;
  }
}

export async function forceCheckForUpdate(): Promise<VersionInfo | null> {
  try {
    await Preferences.remove({ key: VERSION_CHECK_KEY });
    return checkForUpdate();
  } catch (error) {
    systemLogService.error('ui', 'Error in force update check', error as Error);
    return null;
  }
}
