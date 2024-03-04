# AMusic
Music through resource pack

CONFIGURATION:

```
host: (String) #Внешний ip сервера
port: (int) #Порт сервера отправки ресурспаков

processpack: (boolean) #Если false, упаковка ресурспаков запрещена

cache:
 server: (boolean) #Если true ресурспаки кешируются на сервере
 client: (boolean) #Если true ресурспаки кешируются у клиента (Максимум 10 ресурспаков, требует повторной загрузки если изменить host, port, uuid игрока)

strictdownloaderlist: (boolean) #Если true, присоединится к серверу скачивания ресурспака могут только подключеные игроки.

encoder: #В директории /plugins/AMusic/ должен быть файл ffmpeg(add '.exe' if windows, '-osx' if mac) (version 4.4.1)
 use: false #Если false то конвертер не используется (разрешены только .ogg).
 bitrate: 64000
 channels: 2
 samplingrate: 44100
 async: true
```
Already build ffmpeg may be found there: [jave2 repository](https://github.com/a-schild/jave2)

FFMpeg sources: [ffmpeg-4.4.1.tar.gz](https://ffmpeg.org/releases/ffmpeg-4.4.1.tar.gz)

BUILD:

1) Clone repository https://github.com/FatSaw/AMusic.git
2) Build project with: `mvn package`

API:
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
