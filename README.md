# weex-image-crop-picker

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
    compile com.github.voids:weex-image-crop-picker:0.1.0
  }
  ```

- application子类中注册module
  ```java
  import cn.dv4.weeximagecroppicker.ImageCropPickerModule;
  // 在WXSDKEngine.initialize之后注册module
  WXSDKEngine.registerModule("imageCropPicker", ImageCropPickerModule.class);
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
                // 失败返回 {cdoe:'E_PERMISSION_MISSING', message:'...'}
                this.result = JSON.stringify(response)
            })
        }
    }
}
```

> 参数均与[react-native-image-crop-picker](https://github.com/ivpusic/react-native-image-crop-picker) 文档中所列的参数保持一致
>
> 注：跟原插件的android部份一样，并未实现视频压缩，因为ffmpeg太慢并且需要licence
>
>    个人非objC程序员或java程序员，能力有限，欢迎大家提交pr
