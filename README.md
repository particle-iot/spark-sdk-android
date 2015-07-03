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

## WARNING: This documentation is currently under heavy construction - some info here is not updated/correct 

<!---
Android Cloud SDK
=======
-->

The Particle Android Cloud SDK enables Android apps to interact with Particle-powered connected products via the Particle Cloud. It’s an easy-to-use wrapper for Particle REST API. The Cloud SDK will allow you to:

- Manage user sessions for the Particle Cloud (access tokens, encrypted session management)
- Claim/Unclaim devices for a user account
- Get a list of instances of user's Particle devices
- Read variables from devices
- Invoke functions on devices
- Publish events from the mobile app and subscribe to events coming from devices *(Coming Soon)*

All cloud operations should take place asynchronously and use the analgesic AsyncTask wrapper for making Particle cloud API calls and for reporting results allowing you to build  responsive apps for your Particle products and projects.
Android Cloud SDK is implemented as an open-source maven dependency published in the standard [JCenter repository](https://bintray.com/particle/android/cloud-sdk/) for easy integration with Android Studio. See [Installation](#android-cloud-sdk-installation) section for more details.

**Rebranding notice**

Spark has been recently rebranded as Particle.
Code currently refers to `SparkCloud` and `SparkDevice`, this will eventually be replaced with `ParticleCloud` and `ParticleDevice`. This should not bother or affect your code.

**Beta notice**

This SDK is still under development and is currently released as Beta, although tested, bugs and issues may be present, some code might require cleanups.

## Getting Started

- Perform the installation step described under the **Installation** section below for integrating in your own project
- You can also 'git pull' or [Download Particle Android Cloud SDK](https://github.com/spark/spark-sdk-android/archive/master.zip) and try out the included Android example app under module 'example_app'.
- Be sure to check [Usage](#android-cloud-sdk-usage) before you begin for some code examples

## Usage

Cloud SDK usage involves two basic classes: first is `SparkCloud` which is a singleton object that enables all basic cloud operations such as user authentication, device listing, claiming etc. Second class is `SparkDevice` which is an instance represnting a claimed device in the current user session. Each object enables device-specific operation such as: getting its info, invoking functions and reading variables from it.

### Utilities & wrappers

Provided utility classes are:

- Async.executeAsync
- Toaster
- Py
- EZ

(WIP)explain usages for the essential utils we provide

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
(WIP)what here? javadoc generated reference link?

For additional reference check out the [Reference in Cocoadocs website](http://cocoadocs.org/docsets/Spark-SDK/) for full coverage of `SparkDevice` and `SparkCloud` functions and member variables. In addition you can consult the javadoc style comments in `SparkCloud.h` and `SparkDevice.h` for each public method. If Particle Android Cloud SDK is integrated in your XCode project you should be able to press `Esc` to get an auto-complete hints for each cloud and device method.

## Installation

Particle Android Cloud SDK is available through as a [JCenter repository](https://bintray.com/particle/android/cloud-sdk/). JCenter is the default dependency repository for Android Studio. To install the Android Cloud SDK in your project, add the following to your app module gradle file:

(WIP)what do we do with versioning here? We don't want to update the documentation each time we update version in the cloud SDK, on the other hand we don't want to break users apps when something changes

```gradle
dependencies {
    compile 'io.particle:cloudsdk:0.1.2'
}
```

make sure your _main project_ gradle file contains (that's the default):

```gradle
allprojects {
    repositories {
        jcenter()
    }
}
```

then sync and rebuild your module. 

## Communication

- If you **need help**, use [Our community website](http://community.particle.io), use the `Mobile` category for dicussion/troubleshooting Android apps using the Particle Android Cloud SDK.
- If you are certain you **found a bug**, _and can provide steps to reliably reproduce it_, open an issue, label it as `bug`.
- If you **have a feature request**, open an issue with an `enhancement` label on it
- If you **want to contribute**, submit a pull request, be sure to check out spark.github.io for our contribution guidelines, and please sign the [CLA](https://docs.google.com/a/particle.io/forms/d/1_2P-vRKGUFg5bmpcKLHO_qNZWGi5HKYnfrrkd-sbZoA/viewform).


## Maintainers

- Jens Knutsen [Github](https://github.com/jensck/) | [Twitter]()(WIP)update Jens twitter page
- Ido Kleinman [Github](https://www.github.com/idokleinman) | [Twitter](https://www.twitter.com/idokleinman)

## License

Particle Android Cloud SDK is available under the Apache License 2.0. See the LICENSE file for more info.

