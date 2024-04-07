# AMusic
Music through resource pack
## Features:
Clientside, serverside caching

Addition to exsisting resourcepack (Place source resourcepack .zip (with same name as playlist) into /AMusic/Music directory)

5 repeat types (repeatone,repeatall,playone,playall,random)

## Restrictions
Not allowed characters in playlist name: `@` `/` `\` `:` `;` `,` `[` `]` `(` `)` `{` `}` `<` `>` `,` `$` `&` `#` `*` `?` `|` `!` `%` `@` `^` `\t` `\b` `\n` `\r` `\f` `'` `"` `\0`

## Commands:

### How it works
loadmusic - convert(if enabled),pack,send playlist(resourcepack) to target player (if target @n not sends, update flag force true)

playmusic - start/stop sound from loaded resourcepack


`/loadmusic <playername> <playlistname> [update]` - loads playlist(resourcepack) to player, update flag always true if playlist not loaded before

`/playmusic <playername> [soundname]` - starts sound "soundname" from playlist "playername", if no soundname stop sound

`/repeat <playername> <repeat type>` - set repeat type

### Permissions
`amusic.loadmusic` - load playlist(resourcepack), allows `@s` usage

`amusic.playmusic` - start/stop sound, allows `@s` usage

`amusic.repeat` - set repeat, allows `@s` usage

`amusic.loadmusic.other` - load playlist(resourcepack) for other players

`amusic.playmusic.other` - start/stop sound for other players

`amusic.repeat.other` - set repeat for other players

`amusic.loadmusic.update` - reconvert(if enabled), repack, send playlist(resourcepack)

`amusic.loadmusic.nulltarget` - reconvert(if enabled), repack playlist(resourcepack), allows `@n` usage

### Placeholders <playername>:
`@n` - null target
`@s` - self

`loadmusic @s, @n`

`playmusic @s`

`repeat @s`
### Commands for console without tab complete:
`/loadmusic @l` - get packed playlist(resourcepack) list

`/playmusic @l <playername>` - get loaded to player soundnames, currently playing sound name and time

## Config:

```
host: (String) #External server ip or hostname
port: (int) #Resourcepack file server port
processpack: (boolean) #If false, resourcepack packing disabled
cache:
 server: (boolean) #If true resourcepack cached on server
 client: (boolean) #If true resourcepack cached on client (Max 10), resets if host, port, tokensalt, player uuid changed)
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
