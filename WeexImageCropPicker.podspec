#
Pod::Spec.new do |s|
  s.name             = 'WeexImageCropPicker'
  s.version          = '0.1.0'
  s.summary          = 'A weex plugin for image picker'
  s.description      = <<-DESC
                       WeexImageCropPicker is an image picker with support for camera, configurable compression, multiple images and cropping for weex framework.
                       DESC

  s.homepage         = 'https://github.com/voids/weex-image-crop-picker'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'voids' => 'qiudong26@gmail.com' }
  s.source           = { :git => 'https://github.com/voids/weex-image-crop-picker.git', :tag => s.version.to_s }

  s.platform     = :ios, "8.0"

  s.source_files = 'ios/*.{h,m}'

  s.requires_arc = true

  s.frameworks = "SomeFramework", "AnotherFramework"

  s.dependency "WeexSDK"
  s.dependency "WeexPluginLoader"
  s.dependency "QBImagePickerController", "3.4.0"
  s.dependency "RSKImageCropper", "1.6.3"
end
