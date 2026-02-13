# AMusic
Music through resource pack
## Features:
- Clientside, serverside caching
- Addition to exsisting resourcepack
- 5 repeat types (repeatone,repeatall,playone,playall,random)
- Web sound uploader with clientside convertation
- Large number of supported versions `1.7.10` - `1.21.5`
- Position and count selectors (bukkit only)
- Volume control option `Voice` only `1.13+`

## Files and directories
- `./config.yml` - configuration file
- `./lang.yml` - localisation file
- `./resourcepack.zip` - default parent resourcepack file
- `./Music/` - music directory
- `./Music/<playlist_name>/` - playlist directory
- `./Music/<playlist_name>/<sound_name>` - converted sound
- `./Music/<playlist_name>/<sound_name>.<!ogg>` - not converted sound (Ignored if serverside encoder disabled)
- `./Music/<playlist_name>.zip` - playlist specific parent resourcepack file
- `./Packed/` - packed resourcepacks directory
- `./Packed/<uuid>.ampi` - packed resourcepack with info

## Commands:
- `/loadmusic @n <playlistname>` - update playlist
- `/loadmusic <playername> <playlistname>` - loads playlist(resourcepack) to player, update flag true if playlist not loaded before or used null target `@n`
- `/playmusic <playername> [soundname]` - starts sound "soundname" from playlist "playername", if no soundname stop sound
- `/repeat <playername> <repeat type>` - set repeat type
- `/uploadmusic <start/finish/drop> <playlist>/[token]/[token]` - upload sound

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

## Dependencies

### Ffmpeg
[Size reduced ffmpeg 7.0.1 building arguments Linux](/FFMPEG_BUILD.md)

### Bukkit
`...`
### Velocity
`Protocolize`

## BUILD:

1) Clone repository https://github.com/FatSaw/AMusic.git
2) Build project with: `mvn package`

## API:

### AMusic core
May be used to add amusic core into other plugin, or create multiple independent AMusic instances
```
AMusic api = new AMusic(ConfigOptions configoptions, SoundSource<?> source, PackSender packsender, SoundStarter soundstarter, SoundStopper soundstopper, ConcurrentHashMap<Object,InetAddress> playerips);
api.enable(); //starts threads
api.disable(); //stops threads
```
### For all operating modes
```
api.logout(UUID playeruuid); //Handle logout.
api.getPlaylists(boolean packed, boolean useCache, Consumer<String[]> resultConsumer); //get playlists(resourcepacks)
api.getPlaylistSoundnames(String playlistname, boolean packed, boolean useCache, Consumer<String[]> resultConsumer); //get list of sounds in playlist "playlistname"
api.getPlaylistSoundnames(UUID playeruuid, boolean useCache, Consumer<String[]> resultConsumer); //get list of sounds loaded to player with uuid "playeruuid"
api.getPlaylistSoundlengths(String playlistname, boolean useCache, Consumer<short[]> resultConsumer); //get list of sounds length in playlist "playlistname"
api.getPlaylistSoundlengths(UUID playeruuid, boolean useCache, Consumer<short[]> resultConsumer); //get list of sounds length loaded to player with uuid "playeruuid"
api.loadPack(UUID[] playeruuid, String name, boolean update, StatusReport statusreport); //pack, convert(if enabled), send playlist(resourcepack) to player with uuid "playeruuid" (if playeruuid null not send)
api.getPackName(UUID playeruuid, Consumer<String> resultConsumer); //get loaded playlist(resourcepack) name, of player with uuid "playeruuid" 
api.setRepeatMode(UUID playeruuid, RepeatType repeattype); //set repeat mode "repeattype" to player with uuid "playeruuid"
api.stopSound(UUID playeruuid); //stop sound to player with uuid "playeruuid"
api.playSound(UUID playeruuid, String name); //start sound "name" to player with uuid "playeruuid"
api.getPlayingSoundName(UUID playeruuid, Consumer<String> resultConsumer); //get currently playing sound of player with uuid "playeruuid"
api.getPlayingSoundSize(UUID playeruuid, Consumer<Short> resultConsumer); //get currently playing sound size of player with uuid "playeruuid"
api.getPlayingSoundRemain(UUID playeruuid, Consumer<Short> resultConsumer); //get currently playing sound remaining time of player with uuid "playeruuid"
api.openUploadSession(String playlistname, Consumer<UUID> resultConsumer); //open upload session.
api.getUploadSessions(Consumer<UUID[]> resultConsumer); //get upload sessions.
api.closeUploadSession(UUID token, boolean save, Consumer<Boolean> resultConsumer); //close upload session
api.closeUploadSession(UUID token, boolean save); //close upload session
```
