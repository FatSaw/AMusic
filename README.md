# AMusic
Music through resource pack

API:
```
AMusic.getPlaylists(); //список плейлистов
AMusic.getPlaylistSoundnames(String playlistname); //получить список звуков в плейлисте
AMusic.getPlaylistSoundnames(Player player); //получить список звуков в плейлисте которые загружены игроку
AMusic.getPlaylistSoundlengths(String playlistname); //получить список длин звуков (в секундах)
AMusic.getPlaylistSoundlengths(Player player); //получить список длин звуков (в секундах) которые загружены игроку
AMusic.loadPack(Player player, String playlistname, boolean update); //загрузить ресурспак игроку
AMusic.setRepeatMode(Player player,RepeatType repeattype); //установить режим повтора
AMusic.stopSound(Player player); //остановить звук
AMusic.playSound(Player player,String name); //запустить звук
AMusic.getPlayingSoundName(Player player); //получить название звука который играет сейчас
AMusic.getPlayingSoundSize(Player player); //получить длину звука который играет сейчас (в секундах)
AMusic.getPlayingSoundRemain(Player player); //получить оставшиеся время звука который играет сейчас (в секундах)
```
