'use strict';

const { app, BrowserWindow, dialog, ipcMain, shell, session } = require('electron');
const fs = require('node:fs');
const path = require('node:path');
const { loadCorpus } = require('./core/content-loader');
const { JsonStore, normalizeState } = require('./core/store');
const { AudioManager } = require('./core/audio-manager');

let mainWindow;
let corpus;
let store;
let audioManager;

function contentDirectory() {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'content')
    : path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'content', 'tanzil-uthmani-1.1');
}

function fontDirectory() {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'fonts')
    : path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'font');
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1050,
    minHeight: 700,
    backgroundColor: '#071611',
    title: 'Hifz Sniper Desktop',
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));
  mainWindow.once('ready-to-show', () => mainWindow.show());
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https:\/\//.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
}

function registerIpc() {
  ipcMain.handle('bootstrap', () => ({
    corpus,
    state: store.load(),
    downloads: audioManager.list(),
    appVersion: app.getVersion(),
    platform: process.platform,
    fontUrls: {
      regular: `file:///${path.join(fontDirectory(), 'amiri_regular.ttf').replace(/\\/g, '/')}`,
      bold: `file:///${path.join(fontDirectory(), 'amiri_bold.ttf').replace(/\\/g, '/')}`,
      quran: `file:///${path.join(fontDirectory(), 'amiri_quran.ttf').replace(/\\/g, '/')}`
    }
  }));

  ipcMain.handle('state:save', (_event, value) => store.save(value));
  ipcMain.handle('audio:sources', (_event, { key, primaryUrl }) => audioManager.candidates(key, primaryUrl));
  ipcMain.handle('audio:list', () => audioManager.list());
  ipcMain.handle('audio:remove', (_event, key) => audioManager.remove(key));
  ipcMain.handle('audio:download', async (event, verse) => {
    const result = await audioManager.download(verse, progress => {
      if (!event.sender.isDestroyed()) event.sender.send('audio:progress', progress);
    });
    return { ...result, downloads: audioManager.list() };
  });

  ipcMain.handle('backup:export', async (_event, state) => {
    const result = await dialog.showSaveDialog(mainWindow, {
      title: 'Exporter la progression Hifz-Sniper',
      defaultPath: `hifz-sniper-desktop-backup-${new Date().toISOString().slice(0, 10)}.json`,
      filters: [{ name: 'Sauvegarde JSON', extensions: ['json'] }]
    });
    if (result.canceled || !result.filePath) return { canceled: true };
    const payload = {
      format: 'hifz-sniper-desktop-backup',
      exportedAt: new Date().toISOString(),
      appVersion: app.getVersion(),
      state: normalizeState(state)
    };
    fs.writeFileSync(result.filePath, JSON.stringify(payload, null, 2), 'utf8');
    return { canceled: false, filePath: result.filePath };
  });

  ipcMain.handle('backup:import', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
      title: 'Restaurer une progression Hifz-Sniper',
      properties: ['openFile'],
      filters: [{ name: 'Sauvegarde JSON', extensions: ['json'] }]
    });
    if (result.canceled || !result.filePaths[0]) return { canceled: true };
    const raw = fs.readFileSync(result.filePaths[0], 'utf8');
    if (raw.length > 20 * 1024 * 1024) throw new Error('Sauvegarde trop volumineuse');
    const payload = JSON.parse(raw);
    if (payload?.format !== 'hifz-sniper-desktop-backup' || !payload.state) {
      throw new Error('Format de sauvegarde non reconnu');
    }
    const state = store.save(payload.state);
    return { canceled: false, state };
  });

  ipcMain.handle('system:open-data', () => shell.openPath(app.getPath('userData')));
  ipcMain.handle('system:open-external', (_event, url) => {
    if (!/^https:\/\//.test(url)) throw new Error('Adresse externe refusée');
    return shell.openExternal(url);
  });
}

app.whenReady().then(() => {
  app.setAppUserModelId('com.hifzsniper.desktop');
  store = new JsonStore(path.join(app.getPath('userData'), 'hifz-data.json'));
  audioManager = new AudioManager(path.join(app.getPath('userData'), 'audio'));
  corpus = loadCorpus(contentDirectory());

  session.defaultSession.setPermissionRequestHandler((_webContents, permission, callback, details) => {
    callback(permission === 'media' && Array.isArray(details.mediaTypes) && details.mediaTypes.includes('audio'));
  });
  registerIpc();
  createWindow();
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
