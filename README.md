# weex-image-crop-picker

[![JitPack](https://img.shields.io/jitpack/v/voids/weex-image-crop-picker.svg)](https://jitpack.io/#voids/weex-image-crop-picker)
[![CocoaPods](https://img.shields.io/cocoapods/v/WeexImageCropPicker.svg)](https://cocoapods.org/pods/WeexImageCropPicker)
[![license](https://img.shields.io/github/license/voids/weex-image-crop-picker.svg)]()

> 由于weex market里的[weex-image-picker](https://github.com/weexext/weex-image-picker)插件并无裁剪和多选功能，看到也是从[react-native-image-picker](https://github.com/react-community/react-native-image-picker)移植来的，并且该插件原作者推荐了功能比较多的 [react-native-image-crop-picker](https://github.com/ivpusic/react-native-image-crop-picker) ，于是产生了此项目。

## Android

- root目录的build.gradle增加jitpack的地址
  ```gradle
  allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
  }
  ```

- 在你的app目录的build.gradle增加一行依赖
  ```gradle
  dependencies {
    compile com.github.voids:weex-image-crop-picker:0.1.5
  }
  ```

- Application子类中注册module
  ```java
  import cn.dv4.weeximagecroppicker.ImageCropPickerModule;
  // 在WXSDKEngine.initialize之后注册module
  WXSDKEngine.registerModule("imageCropPicker", ImageCropPickerModule.class);
  ```

- 在你的WXSDKInstance所在的Activity中重载onActivityResult，否则接收不到返回结果
  ```java
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
          if (mWXSDKInstance != null) {
          mWXSDKInstance.onActivityResult(requestCode, resultCode, data);
      }
  }
  ```

- 如果需要使用camera，则需要手动在AndroidManifest.xml中添加一行权限
  ```xml
  <uses-permission android:name="android.permission.CAMERA"/>
  ```

## iOS

- 在Podfile增加一行依赖
  ```Podfile
  pod 'WeexImageCropPicker'
  ```

- 更新依赖
  ```shell
  pod install
  ```

- appdelegate中注册module
  ```objective-c
  #import <WeexImageCropPicker/ImageCropPicker.h>
  // 在[WXSDKEngine initSDKEnvironment] 之后注册module
  [WXSDKEngine registerModule:@"imageCropPicker" withClass:[ImageCropPicker class]];
  ```

- 请在info.plist中自行添加权限

## javascript

> 由于weex的扩展为callback，不支持promise，所以用法有些调整。
  ```javascript
  // example
  var ImageCropPicker = weex.requireModule('imageCropPicker')
  var options = {
      width: 300,
      height: 300,
      includeExif: true,
      mediaType: 'photo',
      cropping: true
  }

  export default {
      data: {
          result:""
      },
      methods: {
          gallery(e) {
              ImageCropPicker.openPicker(options, (response) => {
                  // 成功返回 {code:'E_SUCCESS', data:{...}}
                  this.result = JSON.stringify(response)
              })
          },
          camera(e) {
              ImageCropPicker.openCamera(options, (response) => {
                  // 失败返回 {code:'E_PERMISSION_MISSING', message:'...'}
                  this.result = JSON.stringify(response)
              })
          }
      }
  }
  ```
#### options

| Property                                |                   Type                   | Description                              |
| --------------------------------------- | :--------------------------------------: | :--------------------------------------- |
| cropping                                |           bool (default false)           | 是否开启图片剪裁                                 |
| width                                   |                  number                  | 剪裁后图片的宽度，`cropping`为true时有效              |
| height                                  |                  number                  | 剪裁后图片的高度，`cropping` 为true时有效             |
| multiple                                |           bool (default false)           | 是否开启多选，开启多选后裁剪功能无效                       |
| includeBase64                           |           bool (default false)           | 是否输出base64                               |
| includeExif                             |           bool (default false)           | 是否输出图片exif信息，如gps、光圈、快门速度等               |
| cropperActiveWidgetColor (android only) |       string (default `"#424242"`)       | When cropping image, determines ActiveWidget color. |
| cropperStatusBarColor (android only)    |        string (default `#424242`)        | When cropping image, determines the color of StatusBar. |
| cropperToolbarColor (android only)      |        string (default `#424242`)        | When cropping image, determines the color of Toolbar. |
| cropperToolbarTitle (android only)      |      string (default `Edit Photo`)       | When cropping image, determines the title of Toolbar. |
| cropperCircleOverlay                    |           bool (default false)           | 是否裁剪时开启遮罩                                |
| minFiles (ios only)                     |            number (default 1)            | Min number of files to select when using `multiple` option |
| maxFiles (ios only)                     |            number (default 5)            | Max number of files to select when using `multiple` option |
| waitAnimationEnd (ios only)             |           bool (default true)            | Promise will resolve/reject once ViewController `completion` block is called |
| smartAlbums (ios only)                  | array (default ['UserLibrary', 'PhotoStream', 'Panoramas', 'Videos', 'Bursts']) | List of smart albums to choose from      |
| useFrontCamera (ios only)               |           bool (default false)           | Whether to default to the front/'selfie' camera when opened |
| compressVideoPreset (ios only)          |      string (default MediumQuality)      | Choose which preset will be used for video compression |
| compressImageMaxWidth                   |          number (default none)           | 图片压缩指定最大宽度                               |
| compressImageMaxHeight                  |          number (default none)           | 图片压缩指定最大高度                               |
| compressImageQuality                    |            number (default 1)            | 图片压缩质量 (取值范围 0 — 1，1为最好质量)               |
| loadingLabelText (ios only)             | string (default "Processing assets...")  | Text displayed while photo is loading in picker |
| mediaType                               |           string (default any)           | 媒体选择类型: 'photo'=照片, 'video'=视频, 'any'=全部 |
| showsSelectedCount (ios only)           |           bool (default true)            | Whether to show the number of selected assets |
| showCropGuidelines (android only)       |           bool (default true)            | Whether to show the 3x3 grid on top of the image during cropping |
| hideBottomControls (android only)       |           bool (default false)           | Whether to display bottom controls       |
| enableRotationGesture (android only)    |           bool (default false)           | Whether to enable rotating the image by hand gesture |

> 参数均与[react-native-image-crop-picker](https://github.com/ivpusic/react-native-image-crop-picker) 文档中所列的参数保持一致
>
> 注：跟原插件的android部份一样，并未实现视频压缩，因为ffmpeg太慢并且需要licence
