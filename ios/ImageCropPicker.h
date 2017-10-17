#ifndef RN_IMAGE_CROP_PICKER_h
#define RN_IMAGE_CROP_PICKER_h

#import <Foundation/Foundation.h>
#import <QBImagePickerController/QBImagePickerController.h>
#import <RSKImageCropper.h>
#import <math.h>
#import <WeexSDK/WXModuleProtocol.h>
#import "UIImage+Resize.h"
#import "Compression.h"

@interface ImageCropPicker : NSObject<
UIImagePickerControllerDelegate,
UINavigationControllerDelegate,
WXModuleProtocol,
QBImagePickerControllerDelegate,
RSKImageCropViewControllerDelegate,
RSKImageCropViewControllerDataSource>

typedef enum selectionMode {
    CAMERA,
    CROPPING,
    PICKER
} SelectionMode;

@property (nonatomic, strong) NSMutableDictionary *croppingFile;
@property (nonatomic, strong) NSDictionary *defaultOptions;
@property (nonatomic, strong) Compression *compression;
@property (nonatomic, retain) NSMutableDictionary *options;
@property (nonatomic, strong) WXModuleCallback callback;
@property SelectionMode currentSelectionMode;

@end

#endif
