
The script LEAPstarter.bat is able to run the LEAP service when there is a problem for which it is not running.

It can be loaded within Java using something like:
Runtime.getRuntime().exec("cmd /c start build.bat");
with the proper URL.

Create a button in the application that allows the User to use this script simply pressing a button on the screen.