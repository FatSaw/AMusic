# AMusic
Music through resource pack
## Features:
Clientside, serverside caching

Addition to exsisting resourcepack (Place source resourcepack .zip (with same name as playlist) into /AMusic/Music directory)

5 repeat types (repeatone,repeatall,playone,playall,random)

## Config:

```
host: (String) #External server ip or hostname
port: (int) #Resourcepack file server port
processpack: (boolean) #If false, resourcepack packing disabled
cache:
 server: (boolean) #If true resourcepack cached on server
 client: (boolean) #If true resourcepack cached on client (Max 10), resets if host, port, player uuid changed)
strictdownloaderlist: (boolean) #If true, only connected players can access resourcepack server
tokensalt: (Base64 String) #Salt for token generator, replace it to random Base64, needs only when client cache enabled
encoder: 
 use: false #If true place ffmpeg('.exe'(windows)/'-osx'(mac)/''(linux)) (version 4.4.1) into plugin directory
 bitrate: 64000
 channels: 2
 samplingrate: 44100
 async: true
```
## Dependencies

Already build ffmpeg may be found there: [jave2 repository](https://github.com/a-schild/jave2)

FFMpeg sources: [ffmpeg-4.4.1.tar.gz](https://ffmpeg.org/releases/ffmpeg-4.4.1.tar.gz)

## BUILD:

1) Clone repository https://github.com/FatSaw/AMusic.git
2) Build project with: `mvn package`

## API:
```
AMusic api = AMusic.API(); //GET API INSTANCE
api.getPlaylists(); //список плейлистов
api.getPlaylistSoundnames(String playlistname); //получить список звуков в плейлисте
api.getPlaylistSoundnames(UUID playeruuid); //получить список звуков в плейлисте которые загружены игроку
api.getPlaylistSoundlengths(String playlistname); //получить список длин звуков (в секундах)
api.getPlaylistSoundlengths(UUID playeruuid); //получить список длин звуков (в секундах) которые загружены игроку
api.loadPack(UUID playeruuid, String playlistname, boolean update); //загрузить ресурспак игроку
api.getPackName(UUID playeruuid); //получить название загруженного  ресурспака
api.setRepeatMode(UUID playeruuid, RepeatType repeattype); //установить режим повтора
api.stopSound(UUID playeruuid); //остановить звук
api.playSound(UUID playeruuid, String name); //запустить звук
api.getPlayingSoundName(UUID playeruuid); //получить название звука который играет сейчас
api.getPlayingSoundSize(UUID playeruuid); //получить длину звука который играет сейчас (в секундах)
api.getPlayingSoundRemain(UUID playeruuid); //получить оставшиеся время звука который играет сейчас (в секундах)
```
