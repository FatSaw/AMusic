# AMusic
Music through resource pack

API:
```
AMusic.getPlaylists(); //список плейлистов
AMusic.getPlaylistSoundnames(String playlistname); //получить список звуков в плейлисте
AMusic.getPlaylistSoundnames(UUID playeruuid); //получить список звуков в плейлисте которые загружены игроку
AMusic.getPlaylistSoundlengths(String playlistname); //получить список длин звуков (в секундах)
AMusic.getPlaylistSoundlengths(UUID playeruuid); //получить список длин звуков (в секундах) которые загружены игроку
AMusic.loadPack(UUID playeruuid, String playlistname, boolean update); //загрузить ресурспак игроку
AMusic.getPackName(UUID playeruuid); //получить название загруженного  ресурспака
AMusic.setRepeatMode(UUID playeruuid, RepeatType repeattype); //установить режим повтора
AMusic.stopSound(UUID playeruuid); //остановить звук
AMusic.playSound(UUID playeruuid, String name); //запустить звук
AMusic.getPlayingSoundName(UUID playeruuid); //получить название звука который играет сейчас
AMusic.getPlayingSoundSize(UUID playeruuid); //получить длину звука который играет сейчас (в секундах)
AMusic.getPlayingSoundRemain(UUID playeruuid); //получить оставшиеся время звука который играет сейчас (в секундах)
```
