# readium-react-bridge

# Usage

import {NativeModules} from 'react-native';
```      
// XCode
NativeModules.RNReadiumReactBridge.navigateToReadium("file:///User/.../Documents/1.epub");

// Android
NativeModules.RNReadiumReactBridge.navigateToReadium("/storage/emulated/0/2.epub");
```      

# iOS

1. npm install --save readium-react-bridge

2. open the xCode Project .xcworkspace

3. Select the Project on xCode, Add Files to "Project"

Add "node_modules/readium-react-bridge/ios/RNReadiumReactBridge.xcodeproj" into the Project.

4. Project Setting/General/Embedded Binaries

Insert the RNReadiumReactBridgeBundle.bundle into Embedded Binaries.

"node_modules/readium-react-bridge/ios/*.framework"
When it's Choosing options for adding these files dialogs,
You need to check Destination : "Copy items if needed".

5. Project Setting/General/Linked Frameworks and Libraries

Insert the libRNReadiumReactBridge.a into Linked Frameworks and Libraries

Foundation.framework 

6. Project Setting/Build Phases/Copy Bundle Resources

Add the RNReadiumReactBridgeBundle.bundle there.

7. New file with "File.swift"

Add new file on the main project.

when the xCode ask the "Create Bridging Header", click on "Create Bridging Header" button.



8. Change Pods/DevelopmentPods/React-Core/Base/RCBridgeModule.h
```
    #Import <React/RCTDefines.h> => #Import "RCTDefines.h"
```

# Android
1.  Create a new react-native project (or use an existing one)  
2.  npm install --save readium-react-bridge
3.  In "android/build.gradle":
    *  set "minSdkVersion" to 21 (or greater)
	*  add the following into "allprojects.repositories":
 ```
            maven { url 'https://jitpack.io' }
            maven { url "https://s3.amazonaws.com/repo.commonsware.com" }
            maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
 ```        
4.  In "android/app/src/main/AndroidManifest.xml"
    *  add "tools" tag like this:
                  <manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools" package="com.test_app">     
    *  change tools:replace="android:allowBackup,android:name,android:theme" 
5. In "android\app\src\main\java\com\"ProjectName"\MainApplication.java":
  *  add the following getPackages function:
```java
     return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          new com.readium_react_bridge.ActivityStarterReactPackage());
```          