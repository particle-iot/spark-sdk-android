<!---
for docs only:
---
word: android
title: Android SDK
order: 12
shared: true
---
-->

<p align="center" >
<img src="http://oi60.tinypic.com/116jd51.jpg" alt="Particle" title="Particle">
</p>

<!---
(WIP Update link once we have CI in place)
[![Build Status](https://travis-ci.org/AFNetworking/AFNetworking.svg)](https://travis-ci.org/Spark-SDK/Spark-SDK)
-->

# Particle (_formerly Spark_) Android Cloud SDK

## WARNING: This documentation is currently under heavy construction; some info here is not yet updated/corrected.

<!---
Android Cloud SDK
=======
-->

The Particle Android Cloud SDK enables Android apps to interact with Particle-powered connected products via the Particle Cloud. It’s an easy-to-use wrapper for Particle REST API. The Cloud SDK will allow you to:

- Get a list of instances of user's Particle devices
- Read variables from devices
- Invoke functions on devices
- Manage access tokens for the Particle Cloud
- Claim & unclaim devices for a user account
- *(Coming Soon)* Publish events from the mobile app and subscribe to events coming from devices


**Rebranding notice**

Spark has been recently rebranded as Particle!  Classes like `SparkCloud` and `SparkDevice` this will soon be replaced with `ParticleCloud` and `ParticleDevice`, _et al._


**Beta notice**

This SDK is still under development and is currently in beta.  Although it is tested and mostly API-stable, bugs and other issues may be present, and the API may be subject to change prior to leaving beta.  (See rebranding notice above.)


## Getting Started

The SDK is available as a gradle/maven dependency via [JCenter](https://bintray.com/particle/android/cloud-sdk/).  See [Installation](#android-cloud-sdk-installation) section for more details.  (spoiler: just add `compile 'io.particle:cloudsdk:0.1.2'` to your `build.gradle`)

You can also [download the SDK as a zip](https://github.com/spark/spark-sdk-android/archive/master.zip).

For some usage examples, check out [Usage](#android-cloud-sdk-usage) below, or play with the `example_app` module included here in the repo.

## Usage

**NOTE:** all SDK methods are intentionally implemented as synchronous, blocking calls, including network calls.  This blocking API avoids nested callbacks and other complexity, making it easy to write a series of synchronous calls while on a non-UI thread.  To spare developers some of the awkwardness of making async calls and returning results back to the UI thread, we have supplied the `Async` and `ApiWork` set of convenience classes, a purpose-built wrapper around `AsyncTask` for use with these APIs.  (See [Extras](#android-cloud-sdk-usage-extras) for more info on this.)


Cloud SDK usage mostly involves two main classes:

1. `SparkCloud`, a singleton which enables all basic cloud operations such as user authentication, retrieving a device list, claiming, etc.
2. `SparkDevice`, which represents an instance of a claimed device in the current user session. (WIP: clearly explain "session" or don't mention it at all)  Each instance enables device-specific operations: invoking functions, reading variables, and getting basic info about the device (name, version info, etc).

### Extras

(WIP) further explain usages for the utils?  Give examples?

The SDK also ships with a handful of helpful utility classes:

- Async.executeAsync: as mentioned above, this is a purpose-built wrapper around AsyncTask.  (WIP: improve docs here or, preferably, improve class-level javadoc and just refer people to that.)
- Toaster: Another boilerplate-killer util.  `Toast.makeToast(blah blah blah)` is absurd, when all you really wanted was an ultra-lightweight way to say "put this string on the screen for a sec".  `Toaster` makes this dream come true.
- EZ: Misc. shortcuts for killing boilerplate code, like Toaster, but which have no simple taxonomic classification
- Py: There's nothing Particle or Android specific about this, but it's worth calling out.  This class brings a little Python joy to your Java, like easy collection constructors and a Python-like _truthiness_ check (called `truthy()`).  Quick quiz: unless you have special, specific requirements, like thread safety or immutability, which `List` implementation do you use Every. Single. Time.?  It's `ArrayList`, isn't it?  `Py.java` embraces this fact and gives you `Py.list()`.  For further Pythonic happiness, use it like a static import, so you can just do `list()` the way you could in Python.  Also included here is `truthy()`, which, if you know Python, does exactly what you think.


Here are few examples for the most common use cases to get your started:

#### Logging in to Particle cloud
You don't need to worry about access tokens, SDK takes care of that for you

```java
Async.executeAsync(SparkCloud.get(myView.getContext()), new Async.ApiWork<SparkCloud, Void>() {
        public void callApi (SparkCloud sparkCloud) throws SparkCloudException, IOException {
            sparkCloud.logIn("ido@particle.io","userpass");
        }

        @Override
        public void onSuccess(Void aVoid) {
            Toaster.l(myActivity.this, "Logged in");
            // start new activity..
        }

        @Override
        public void onFailure(SparkCloudException e) {
            e.printStackTrace();
            Log.d("info", e.getBestMessage());
            Toaster.l(myActicity.this, "Wrong credentials or no internet connectivity, please try again");
        }
});

```
---


#### Get a list of all devices

List the devices that belong to currently logged in user and find a specific device by name:


```java
Async.executeAsync(SparkCloud.get(v.getContext()), new Async.ApiWork<SparkCloud, List<SparkDevice>>() {
        SparkDevice myDevice;
        public List<SparkDevice> callApi (SparkCloud sparkCloud) throws SparkCloudException, IOException {
            return sparkCloud.getDevices();
        }

        @Override
        public void onSuccess(List<SparkDevice> devices) {
            for (int i = 0; i < devices.size(); i++) {
                SparkDevice device = devices.get(i);
                if (device.getName() == "myDevice") {
                    myDevice = device;
                }
            }
        }

        @Override
        public void onFailure(SparkCloudException e) {
            e.printStackTrace();
            Log.d("info", e.getBestMessage());
            Toaster.l(myActicity.this, "Wrong credentials or no internet connectivity, please try again");
        }
});

```
---

#### Read a variable from a Particle device (Core/Photon)
Assuming here that `myPhoton` is an active instance of `SparkDevice` class which represents a device claimed to current user:

(WIP)fix example

```java
[myPhoton getVariable:@"temperature" completion:^(id result, NSError *error) {
    if (!error) {
        NSNumber *temperatureReading = (NSNumber *)result;
        NSLog(@"Room temperature is %f degrees",temperatureReading.floatValue);
    }
    else {
        NSLog(@"Failed reading temperature from Photon device");
    }
}];
```
---

#### Call a function on a Particle device (Core/Photon)
Invoke a function on the device and pass a list of parameters to it, `resultCode` on the completion block will represent the returned result code of the function on the device

(WIP)fix example

```java
[myPhoton callFunction:@"digitalwrite" withArguments:@[@"D7",@1] completion:^(NSNumber *resultCode, NSError *error) {
    if (!error)
    {
        NSLog(@"LED on D7 successfully turned on");
    }
}];
```
---

#### List device exposed functions and variables
Functions is just a list of names, variables is a dictionary in which keys are variable names and values are variable types:

(WIP)fix example

```java
NSDictionary *myDeviceVariables = myPhoton.variables;
NSLog(@"MyDevice first Variable is called %@ and is from type %@", myDeviceVariables.allKeys[0], myDeviceVariables.allValues[0]);

NSArray *myDeviceFunctions = myPhoton.functions;
NSLog(@"MyDevice first Function is called %@", myDeviceFunctions[0]);
```
---


#### Get an instance of a device
Get a device instance by its ID:

(WIP)fix example

```java
__block SparkDevice *myOtherDevice;
NSString *deviceID = @"53fa73265066544b16208184";
[[SparkCloud sharedInstance] getDevice:deviceID completion:^(SparkDevice *device, NSError *error) {
    if (!error)
        myOtherDevice = device;
}];
```
---

#### Rename a device
you can simply set the `.name` property or use -rename() method if you need a completion block to be called (for example updating a UI after renaming was done):

(WIP)fix example

```java
myPhoton.name = @"myNewDeviceName";
```

_or_
```java
[myPhoton rename:@"myNewDeviecName" completion:^(NSError *error) {
    if (!error)
        NSLog(@"Device renamed successfully");
}];
```
---


#### Logout
Also clears user session and access token

(WIP)fix example

```java
[[SparkCloud sharedInstance] logout];
```
---

### Additional reference
(WIP) include link to generated javadoc, or (better yet) find a better way to browse javadoc for an installed lib within Android Studio, and recommend using that

For additional reference check out the [Reference in Cocoadocs website](http://cocoadocs.org/docsets/Spark-SDK/) for full coverage of `SparkDevice` and `SparkCloud` functions and member variables. In addition you can consult the javadoc style comments in `SparkCloud.h` and `SparkDevice.h` for each public method. If Particle Android Cloud SDK is integrated in your XCode project you should be able to press `Esc` to get an auto-complete hints for each cloud and device method.

## Installation

The SDK is available through [JCenter](https://bintray.com/particle/android/cloud-sdk/).  To install the Android Cloud SDK in your project, add the following to your app module gradle file:

(WIP)what do we do with versioning here? We don't want to update the documentation each time we update version in the cloud SDK, on the other hand we don't want to break users apps when something changes

```gradle
dependencies {
    compile 'io.particle:cloudsdk:0.1.2'
}
```

and make sure your _main project_ gradle file contains the following (it's probably already there, since it's the default):

```gradle
allprojects {
    repositories {
        jcenter()
    }
}
```

then sync and rebuild your module. 

## Communication

- If you **need help**, head to [our community website](http://community.particle.io), under the `Mobile` category for dicussion/troubleshooting around Android apps using the Particle Android Cloud SDK.
- If you are certain you **found a bug**, _and can provide steps to reliably reproduce it_, open an issue, label it as `bug`.
- If you **have a feature request**, open an issue with an `enhancement` label on it
- If you **want to contribute**, submit a pull request, be sure to check out spark.github.io for our contribution guidelines, and please sign the [CLA](https://docs.google.com/a/particle.io/forms/d/1_2P-vRKGUFg5bmpcKLHO_qNZWGi5HKYnfrrkd-sbZoA/viewform).


## Maintainers

- Jens Knutson [Github](https://github.com/jensck/) | [Google+](https://google.com/+JensKnutson)
- Ido Kleinman [Github](https://www.github.com/idokleinman) | [Twitter](https://www.twitter.com/idokleinman)

## License

Particle Android Cloud SDK is available under the Apache License 2.0. See the LICENSE file for more info.
