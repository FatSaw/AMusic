# AMusic
Music through resource pack
## Features:
Clientside, serverside caching

Addition to exsisting resourcepack (Place source resourcepack .zip (with same name as playlist) into /AMusic/Music directory)

5 repeat types (repeatone,repeatall,playone,playall,random)

## Commands:

### How it works
loadmusic - convert(if enabled),pack,send playlist(resourcepack) to target player (if target @n not sends, update flag force true)

playmusic - start/stop sound from loaded resourcepack


`/loadmusic <playername> <playlistname>` - loads playlist(resourcepack) to player, update flag true if playlist not loaded before or used null target `@n`

`/playmusic <playername> [soundname]` - starts sound "soundname" from playlist "playername", if no soundname stop sound

`/repeat <playername> <repeat type>` - set repeat type

### Permissions
`amusic.loadmusic` - load playlist(resourcepack), allows `@s` usage

`amusic.playmusic` - start/stop sound, allows `@s` usage

`amusic.repeat` - set repeat, allows `@s` usage

`amusic.loadmusic.other` - load playlist(resourcepack) for other players

`amusic.playmusic.other` - start/stop sound for other players

`amusic.repeat.other` - set repeat for other players

`amusic.loadmusic.update` - reconvert(if enabled), repack playlist(resourcepack), allows `@n` usage

### Placeholders <playername>:
`@n` - null target
`@s` - self

`loadmusic @s, @n`

`playmusic @s`

`repeat @s`
### Commands for console without tab complete:
`loadmusic @l` - get packed playlist(resourcepack) list

`playmusic @l <playername>` - get loaded to player soundnames, currently playing sound name and time

## Config:

```
server:
 host: 127.0.0.1:25530 #External server ip or hostname
 #ip: 127.0.0.1
 port: 25530 #Resourcepack file server port
 #backlog: 50 #Maximum length of the queue of incoming connections
 strictdownloaderlist: true
 #waitacception: true #ResourcePack Status accepted need to access server (if blocked using recomended value 'true', if 1.7.10 'false')
 tokensalt: PlaceHereARandomBase64StringIfClientCacheEnabled
resource:
 processpack: true #If false, resourcepack packing disabled
  cache:
   server: true #If true resourcepack cached on server
   client: true #If true resourcepack cached on client (Max 10), resets if host, port, tokensalt, player uuid changed)
 encoder:
  use: false
  ffmpegbinary: ffmpeg #Path to ffmpeg binary
  bitrate: 64000
  channels: 2
  samplingrate: 44100
  async: true
```
## Dependencies

### Ffmpeg
[Size reduced ffmpeg 7.0.1 building arguments Linux](/FFMPEG_BUILD.md)

### Bukkit
`PlaceholderAPI` (optional)
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
AMusic api = new AMusic(ConfigOptions configoptions, Data data, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, ConcurrentHashMap<Object,InetAddress> playerips);
api.enable(); //starts positiontracker and resourceserver threads
api.disable(); //stops positiontracker and resourceserver threads
```
### For all operating modes
```
api.getPlaylists(); //get list of already packed playlists(resourcepacks)
api.getPlaylistSoundnames(String playlistname); //get list of sounds in playlist "playlistname"
api.getPlaylistSoundnames(UUID playeruuid); //get list of sounds loaded to player with uuid "playeruuid"
api.getPlaylistSoundlengths(String playlistname); //get list of sounds length in playlist "playlistname"
api.getPlaylistSoundlengths(UUID playeruuid); //get list of sounds length loaded to player with uuid "playeruuid"
api.loadPack(UUID playeruuid, String playlistname, boolean update); //pack, convert(if enabled), send playlist(resourcepack) to player with uuid "playeruuid" (if playeruuid null not send)
api.getPackName(UUID playeruuid); //get loaded playlist(resourcepack) name, of player with uuid "playeruuid" 
api.setRepeatMode(UUID playeruuid, RepeatType repeattype); //set repeat mode "repeattype" to player with uuid "playeruuid"
api.stopSound(UUID playeruuid); //stop sound to player with uuid "playeruuid"
api.playSound(UUID playeruuid, String name); //start sound "name" to player with uuid "playeruuid"
api.getPlayingSoundName(UUID playeruuid); //get currently playing sound of player with uuid "playeruuid"
api.getPlayingSoundSize(UUID playeruuid); //get currently playing sound size of player with uuid "playeruuid"
api.getPlayingSoundRemain(UUID playeruuid); //get currently playing sound remaining time of player with uuid "playeruuid"
```
