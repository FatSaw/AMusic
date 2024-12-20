# AMusic
Music through resource pack
## Features:
- Clientside, serverside caching
- Addition to exsisting resourcepack (Place source resourcepack .zip (with same name as playlist) into /AMusic/Music directory)
- 5 repeat types (repeatone,repeatall,playone,playall,random)
- Web sound uploader with clientside convertation
- Large number of supported versions `1.7.10` - `1.21.1`
- Position and count selectors (bukkit only)

## Commands:
- `/loadmusic @n <playlistname>` - update playlist
- `/loadmusic <playername> <playlistname>` - loads playlist(resourcepack) to player, update flag true if playlist not loaded before or used null target `@n`
- `/playmusic <playername> [soundname]` - starts sound "soundname" from playlist "playername", if no soundname stop sound
- `/repeat <playername> <repeat type>` - set repeat type
- `/uploadmusic <start/finish> <playlist>/[token]` - upload sound

### Permissions
- `amusic.loadmusic` - load playlist(resourcepack), allows `@s` usage
- `amusic.playmusic` - start/stop sound, allows `@s` usage
- `amusic.repeat` - set repeat, allows `@s` usage
- `amusic.loadmusic.other` - load playlist(resourcepack) for other players
- `amusic.playmusic.other` - start/stop sound for other players
- `amusic.repeat.other` - set repeat for other players
- `amusic.loadmusic.update` - reconvert(if enabled), repack playlist(resourcepack), allows `@n` usage
- `amusic.uploadmusic` - start stop upload session
- `amusic.uploadmusic.token` - allows finish session by token

### Selectors <playername>:
- `@n` - update playlist
- `@s` - self

Argument format (`@p`, `@r`, `@a`): `[arg1,arg2,arg3,...]`
- `loadmusic` `@s`, `@p`, `@r`, `@a`, `@n`
- `playmusic` `@s`, `@p`, `@r`, `@a`
- `repeat` `@s`, `@p`, `@r`, `@a`

#### Selector `@p`, `@r`, `@a` arguments
##### distance and position
Args:
- `dist`
- `x`
- `y`
- `z`

Format: `<arg><operation><double_value>`
Avilable operations: `<=`, `<`, `>=`, `>`
Description: `Distance and postition`

#### Selector `@a` arguments
##### closer, further, random
Args:
- `closer`
- `further`
- `random`

Format: `<arg><operation><int_value>`
Avilable operations: `=`
Description: `Limits player count`

### Commands for console without tab complete:
`loadmusic @l` - get packed playlist(resourcepack) list

`playmusic @l <playername>` - get loaded to player soundnames, currently playing sound name and time

## Config:
```
uploaderserver:
 use: false
 host: http://127.0.0.1:25532/
 #ip: 127.0.0.1
 port: 25532
 backlog: 50
 strictaccess: true
 timeout: 600000
 limit: 262144000
server:
 host: http://127.0.0.1:25530/
 #ip: 127.0.0.1
 port: 25530
 backlog: 50
 strictaccess: true
 #waitacception: true
 tokensalt: PlaceHereARandomBase64StringIfClientCacheEnabled
resource:
 processpack: true
  cache:
   server: true
   client: true
 encoder:
  use: false
  ffmpegbinary: ffmpeg
  bitrate: 64000
  channels: 2
  samplingrate: 44100
  async: true
```
## Dependencies

### Ffmpeg
[Size reduced ffmpeg 7.0.1 building arguments Linux](/FFMPEG_BUILD.md)

### Bukkit
`...`
### Velocity
`Protocolise`

## BUILD:

1) Clone repository https://github.com/FatSaw/AMusic.git
2) Build project with: `mvn package`

## API:

### AMusic as plugin
Should be used only if AMusic used as plugin
```
AMusic api = AMusic.API(); //GET DEFAULT INSTANCE
```
### AMusic core
May be used to add amusic core into other plugin, or create multiple independent AMusic instances
```
AMusic api = new AMusic(ConfigOptions configoptions, SoundSource<?> source, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, ConcurrentHashMap<Object,InetAddress> playerips);
api.enable(); //starts threads
api.disable(); //stops threads
```
### For all operating modes
```
api.getPlaylists(); //get list of already packed playlists(resourcepacks)
api.getPlaylistSoundnames(String playlistname); //get list of sounds in playlist "playlistname"
api.getPlaylistSoundnames(UUID playeruuid); //get list of sounds loaded to player with uuid "playeruuid"
api.getPlaylistSoundlengths(String playlistname); //get list of sounds length in playlist "playlistname"
api.getPlaylistSoundlengths(UUID playeruuid); //get list of sounds length loaded to player with uuid "playeruuid"
api.loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport); //pack, convert(if enabled), send playlist(resourcepack) to player with uuid "playeruuid" (if playeruuid null not send)
api.getPackName(UUID playeruuid); //get loaded playlist(resourcepack) name, of player with uuid "playeruuid" 
api.setRepeatMode(UUID playeruuid, RepeatType repeattype); //set repeat mode "repeattype" to player with uuid "playeruuid"
api.stopSound(UUID playeruuid); //stop sound to player with uuid "playeruuid"
api.playSound(UUID playeruuid, String name); //start sound "name" to player with uuid "playeruuid"
api.getPlayingSoundName(UUID playeruuid); //get currently playing sound of player with uuid "playeruuid"
api.getPlayingSoundSize(UUID playeruuid); //get currently playing sound size of player with uuid "playeruuid"
api.getPlayingSoundRemain(UUID playeruuid); //get currently playing sound remaining time of player with uuid "playeruuid"
api.openUploadSession(String playlistname); //open upload session.
api.getUploadSessions(); //get upload sessions.
api.closeUploadSession(UUID token); //close upload session
```
