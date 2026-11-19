'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('hifz', {
  bootstrap: () => ipcRenderer.invoke('bootstrap'),
  saveState: state => ipcRenderer.invoke('state:save', state),
  audioSources: payload => ipcRenderer.invoke('audio:sources', payload),
  listAudio: () => ipcRenderer.invoke('audio:list'),
  downloadAudio: verse => ipcRenderer.invoke('audio:download', verse),
  removeAudio: key => ipcRenderer.invoke('audio:remove', key),
  onAudioProgress: callback => {
    const handler = (_event, value) => callback(value);
    ipcRenderer.on('audio:progress', handler);
    return () => ipcRenderer.removeListener('audio:progress', handler);
  },
  exportBackup: state => ipcRenderer.invoke('backup:export', state),
  importBackup: () => ipcRenderer.invoke('backup:import'),
  openDataFolder: () => ipcRenderer.invoke('system:open-data'),
  openExternal: url => ipcRenderer.invoke('system:open-external', url)
});
