# AMusic
Music through resource pack
## Features:
- Clientside, serverside caching
- Resourcepack merge (for versions before `1.20.3`)
- Optional sound repeat, repeat types: (`repeatone`, `repeatall`, `playone`, `playall`, `random`)
- Large number of supported versions `1.7.10` - `1.21.11`
- GeyserMC supported (single resourcepack compatible with java and bedrock, without sound data duplication)
- Volume control in `Voice` sound setting (only for `1.13+`)
- Web sound uploader with clientside transcoding

## Files and directories
- `./config.yml` - configuration file
- `./lang.yml` - localization file
- `./resourcepack.zip` - default parent resourcepack file (used for all resourcepacks merge if exsist)
- `./Music/` - music directory
- `./Music/<playlist_name>/` - playlist directory
- `./Music/<playlist_name>/<sound_name>` - sound
- `./Music/<playlist_name>.zip` - playlist specific parent resourcepack file (used for specific resourcepack merge if exsist)
- `./Packed/` - packed resourcepacks directory
- `./Packed/<uuid>.ampi` - packed resourcepack with info

## Commands:
- `/loadmusic @n <playlistname>` - update playlist
- `/loadmusic <playername> <playlistname>` - loads playlist(resourcepack) to player, update flag true if playlist not loaded before or used null target `@n`
- `/playmusic <playername> [soundname]` - starts sound "soundname" from playlist "playername", if no soundname, stop sound
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
- `amusic.uploadmusic` - start/finish/drop upload session
- `amusic.uploadmusic.token` - allows start/finish/drop session by token

### Selectors <playername>:
- `@n` - update playlist
- `@s` - self
<details>
<summary>Selector options</summary>
  
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
Available operations: `<=`, `<`, `>=`, `>`
Description: `Distance and position`

#### Selector `@a` arguments
##### closer, further, random
Args:
- `closer`
- `further`
- `random`

Format: `<arg><operation><int_value>`
Available operations: `=`
Description: `Limits player count`
</details>
### Commands for console without tab complete:

- `loadmusic @l` - get packed playlist(resourcepack) list
- `playmusic @l <playername>` - get loaded to player soundnames, currently playing sound name and time

## Dependencies

### GeyserMC (optional)
- Viaproxy 3.x.x does not support optional dependency, to use viaproxy amusic implementation without GeyserMC need to remove depends from `viaproxy.yml` inside jar

### FFmpeg (optional)
- [Size reduced ffmpeg 7.0.1 building arguments Linux](/FFMPEG_BUILD.md)

### FFmpeg.wasm
- [source](https://github.com/FatSaw/ffmpeg.wasm)


## BUILD:

1) Clone repository https://github.com/FatSaw/AMusic.git
2) Build project with: `mvn package`
