# Tutorial : Authentication

#### Documentation

A detailed documentation of this tutorial is available [here](https://docs.sightcall.com/GD/03_android_SDK/09_Tutorials/01_authentication.html).

#### Configuration

Every tutorial has a configuration file located here:  
[`app/src/main/java/net/rtccloud/tutorial/Config.java`](app/src/main/java/net/rtccloud/tutorial/Config.java)

You **must** set these three values in order to run the tutorial:

```java
/**
 * Application identifier, available on the portal website.
 */
public static final String APP_ID = "";

/**
 * URL of the token server.
 * It must contain the pattern `%s`.
 * This pattern will automatically be replaced with the provided uid.
 * Example: `https://token-server.com?uid=%s`
 */
public static final String AUTH_URL = "";

/**
 * Credentials to use if the token server is secured with an htaccess dotfile.
 * Leave it empty if the token server is publicly available.
 * It should be this format: `login:pwd`
 */
public static final String AUTH_PWD = "";
```
