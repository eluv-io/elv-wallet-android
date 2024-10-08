# Eluvio Media Wallet for AndroidTV Releases

## Version 2.0
* Overhauled Wallet app. Uses the new Media Property API.

## Version 1.0.4
* Updated Compose-TV libraries. This solves a bug when scrolling up quickly in MyItems - the tab row would get selected before list was done scrolling.

## Version 1.0.3
### Improvements / Changes
* Deeplink support!
* Better loading/empty states for MyMedia tab.
* Added Live Video Support
* Fix redeemable offer image aspect ratio. 
* Fixed scrolling issue in NFTDetail when description is just long enough to prevent scrolling to top.

## Version 1.0.2
### Improvements / Changes
* Redeemable offers support more states, hides when needed, and shows the correct message/timestamps.
* Video player will notify errors and close itself.
* More informative User-Agent header.
* D-Pad navigation improvements.
* Bump Media3 to 1.1.1.

### Bug Fixes
* Pause audio from other apps when starting to play video.
* Disable dry_run for offer redemption. ([#2][i2])
* Empty section names won't break parsing anymore.
* Background audio from other apps wasn't paused when playing a video.

## Version 1.0.1
* Unlock all collectibles until we have a better way to figure out the true lock state.

## Version 1.0 ##
First release of the Eluvio Media Wallet for AndroidTV.

[i2]: https://github.com/eluv-io/elv-wallet-android/issues/2
