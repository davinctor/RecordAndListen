# Record and listen

Write a single screen app that allows a user to record and then to listen to the recording.

Should be:
  - A button to record with mic icon. By pressing on it should be started the voice recording. The recording should start with 500 ms delay while the user holds the record button. The record should stop when the user releases the button.
  - While recording in progress should be shown the panel with record duration
  - There should be a list of all recordings. While recording is playing there should be shown recording playing duration as progress bar in the list item.
  - There shouldn't be allowed to record and play simultaneously.
  - There shouldn't be allowed to play several recordings simultaneously.

As bonus:
  - Implement screen using [Mosby MVI]
  - Use as much as possible RxJava 2.0
  - Remove silence from the recording

[Mosby MVI]: <http://hannesdorfmann.com/android/model-view-intent>