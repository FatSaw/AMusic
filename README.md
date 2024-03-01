# AMusic
Music through resource pack

CONFIGURATION:

```
host: (String) #Внешний ip сервера
port: (int) #Порт сервера отправки ресурспаков

processpack: (boolean) #Если false, упаковка ресурспаков запрещена

checkpackstatus: (boolean) #Проверка статуса ресурспака через (PlayerResourcePackStatusEvent)

packapplystatus: (boolean) #Более точное определение момента применения ресурспака (работает только до 1.14).
#Должен быть false если сервер на версии до 1.14 и используется заход с версий 1.14+

cache:
  server: (boolean) #Если true ресурспаки кешируются на сервере
  client: (boolean) #Если true ресурспаки кешируются у клиента (Максимум 10 ресурспаков, требует повторной загрузки если изменить host, port, uuid игрока)

strictdownloaderlist: (boolean) #Если true, присоединится к серверу скачивания ресурспака могут только подключеные игроки.

#Если заблокировано (закоментировано) то конвертер не используется (будут использованы звуки только .ogg).
#encoder: #В директории /plugins/AMusic/ должен быть файл ffmpeg(add '.exe' if windows, '-osx' if mac) (version 4.4.1)
#  bitrate: 64000
#  channels: 2
#  samplingrate: 44100
#  async: true
```
Already build ffmpeg may be found there: [jave2 repository](https://github.com/a-schild/jave2)

FFMpeg sources: [ffmpeg-4.4.1.tar.gz](https://ffmpeg.org/releases/ffmpeg-4.4.1.tar.gz)

BUILD:

1) Build craftbukkit 1.8.8, 1.9.4, 1.10.2, 1.11.2, 1.12.2, 1.13.2 via [BuildTools](https://www.spigotmc.org/wiki/buildtools/)
2) Clone repository https://github.com/FatSaw/AMusic.git
3) Build project with: `mvn package`

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
