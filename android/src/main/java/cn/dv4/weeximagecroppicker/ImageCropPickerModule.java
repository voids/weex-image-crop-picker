package cn.dv4.weeximagecroppicker;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.*;
import java.util.*;

public class ImageCropPickerModule extends WXModule {
    private static final int IMAGE_PICKER_REQUEST = 61110;
    private static final int CAMERA_PICKER_REQUEST = 61111;
    private static final int CLEAN_REQUEST = 61112;
    private static final int CLEAN_SINGLE_REQUEST = 61113;

    private static final String E_SUCCESS = "E_SUCCESS";
    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
    private static final String E_PICKER_CANCELLED_KEY = "E_PICKER_CANCELLED";
    private static final String E_CALLBACK_ERROR = "E_CALLBACK_ERROR";
    private static final String E_FAILED_TO_SHOW_PICKER = "E_FAILED_TO_SHOW_PICKER";
    private static final String E_FAILED_TO_OPEN_CAMERA = "E_FAILED_TO_OPEN_CAMERA";
    private static final String E_NO_IMAGE_DATA_FOUND = "E_NO_IMAGE_DATA_FOUND";
    private static final String E_CAMERA_IS_NOT_AVAILABLE = "E_CAMERA_IS_NOT_AVAILABLE";
    private static final String E_CANNOT_LAUNCH_CAMERA = "E_CANNOT_LAUNCH_CAMERA";
    private static final String E_PERMISSIONS_MISSING = "E_PERMISSION_MISSING";
    private static final String E_ERROR_WHILE_CLEANING_FILES = "E_ERROR_WHILE_CLEANING_FILES";

    private String mediaType = "any";
    private boolean multiple = false;
    private boolean includeBase64 = false;
    private boolean includeExif = false;
    private boolean cropping = false;
    private boolean cropperCircleOverlay = false;
    private boolean freeStyleCropEnabled = false;
    private boolean showCropGuidelines = true;
    private boolean hideBottomControls = false;
    private boolean enableRotationGesture = false;
    private boolean disableCropperColorSetters = false;
    private String pathToDelete;
    private JSONObject options;


    //Grey 800
    private final String DEFAULT_TINT = "#424242";
    private String cropperActiveWidgetColor = DEFAULT_TINT;
    private String cropperStatusBarColor = DEFAULT_TINT;
    private String cropperToolbarColor = DEFAULT_TINT;
    private String cropperToolbarTitle = null;

    //Light Blue 500
    private final String DEFAULT_WIDGET_COLOR = "#03A9F4";
    private int width = 200;
    private int height = 200;

    private Uri mCameraCaptureURI;
    private String mCurrentPhotoPath;
    private ResponseHelper responseHelper;
    private Compression compression = new Compression();

    private String getTmpDir(Activity activity) {
        String tmpDir = activity.getCacheDir() + "/react-native-image-crop-picker";
        Boolean created = new File(tmpDir).mkdir();

        return tmpDir;
    }

    public String getName() {
        return "ImageCropPicker";
    }

    private void setConfiguration(final JSONObject options, final JSCallback callback) {
        mediaType = options.containsKey("mediaType") ? options.getString("mediaType") : mediaType;
        multiple = options.containsKey("multiple") && options.getBoolean("multiple");
        includeBase64 = options.containsKey("includeBase64") && options.getBoolean("includeBase64");
        includeExif = options.containsKey("includeExif") && options.getBoolean("includeExif");
        width = options.containsKey("width") ? options.getInteger("width") : width;
        height = options.containsKey("height") ? options.getInteger("height") : height;
        cropping = options.containsKey("cropping") ? options.getBoolean("cropping") : cropping;
        cropperActiveWidgetColor = options.containsKey("cropperActiveWidgetColor") ? options.getString("cropperActiveWidgetColor") : cropperActiveWidgetColor;
        cropperStatusBarColor = options.containsKey("cropperStatusBarColor") ? options.getString("cropperStatusBarColor") : cropperStatusBarColor;
        cropperToolbarColor = options.containsKey("cropperToolbarColor") ? options.getString("cropperToolbarColor") : cropperToolbarColor;
        cropperToolbarTitle = options.containsKey("cropperToolbarTitle") ? options.getString("cropperToolbarTitle") : null;
        cropperCircleOverlay = options.containsKey("cropperCircleOverlay") ? options.getBoolean("cropperCircleOverlay") : cropperCircleOverlay;
        freeStyleCropEnabled = options.containsKey("freeStyleCropEnabled") ? options.getBoolean("freeStyleCropEnabled") : freeStyleCropEnabled;
        showCropGuidelines = options.containsKey("showCropGuidelines") ? options.getBoolean("showCropGuidelines") : showCropGuidelines;
        hideBottomControls = options.containsKey("hideBottomControls") ? options.getBoolean("hideBottomControls") : hideBottomControls;
        enableRotationGesture = options.containsKey("enableRotationGesture") ? options.getBoolean("enableRotationGesture") : enableRotationGesture;
        disableCropperColorSetters = options.containsKey("disableCropperColorSetters") ? options.getBoolean("disableCropperColorSetters") : disableCropperColorSetters;
        this.options = options;
        this.responseHelper = new ResponseHelper(callback);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                responseHelper.invoke(E_PERMISSIONS_MISSING, "Required permission missing");
                return;
            }
        }
        switch (requestCode) {
            case CAMERA_PICKER_REQUEST:
                initiateCamera();
                break;
            case IMAGE_PICKER_REQUEST:
                initiatePicker();
                break;
            case CLEAN_REQUEST:
                initiateClean();
                break;
            case CLEAN_SINGLE_REQUEST:
                initiateCleanSingle();
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean permissionsCheck(final Activity activity, final List<String> requiredPermissions, int requestCode) {
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            int status = ActivityCompat.checkSelfPermission(activity, permission);
            if (status != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, missingPermissions.toArray(new String[missingPermissions.size()]), requestCode);
            return false;
        }
        return true;
    }

    @JSMethod
    public void clean(final JSCallback callback) {
        setConfiguration(new JSONObject(), callback);
        final Activity activity = getCurrentActivity();
        if (!permissionsCheck(activity, Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE), CLEAN_REQUEST)) {
            return;
        }
        initiateClean();
    }

    private void initiateClean() {
        try {
            final Activity activity = getCurrentActivity();
            final ImageCropPickerModule module = this;
            File file = new File(module.getTmpDir(activity));
            if (!file.exists()) throw new Exception("File does not exist");
            module.deleteRecursive(file);
            responseHelper.invoke(E_SUCCESS);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseHelper.invoke(E_ERROR_WHILE_CLEANING_FILES, ex.getMessage());
        }
    }

    @JSMethod
    public void cleanSingle(final String pathToDelete, final JSCallback callback) {
        this.pathToDelete = pathToDelete;
        setConfiguration(new JSONObject(), callback);
        if (pathToDelete == null) {
            responseHelper.invoke(E_ERROR_WHILE_CLEANING_FILES, "Cannot cleanup empty path");
            return;
        }
        final Activity activity = getCurrentActivity();
        if (!permissionsCheck(activity, Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE), CLEAN_SINGLE_REQUEST)) {
            return;
        }
        initiateCleanSingle();
    }

    private void initiateCleanSingle() {
        try {
            String path = pathToDelete;
            final String filePrefix = "file://";
            if (path.startsWith(filePrefix)) {
                path = path.substring(filePrefix.length());
            }
            File file = new File(path);
            if (!file.exists()) throw new Exception("File does not exist. Path: " + path);
            final ImageCropPickerModule module = this;
            module.deleteRecursive(file);
            responseHelper.invoke(E_SUCCESS);
        } catch (Exception ex) {
            ex.printStackTrace();
            responseHelper.invoke(E_ERROR_WHILE_CLEANING_FILES, ex.getMessage());
        }
    }

    @JSMethod
    public void openCamera(final JSONObject options, final JSCallback callback) {
        setConfiguration(options, callback);
        final Activity activity = getCurrentActivity();
        if (!permissionsCheck(activity, Arrays.asList(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_PICKER_REQUEST)) {
            return;
        }
        initiateCamera();
    }

    private void initiateCamera() {
        try {
            final Activity activity = getCurrentActivity();
            if (!isCameraAvailable(activity)) {
                responseHelper.invoke(E_CAMERA_IS_NOT_AVAILABLE, "Camera not available");
                return;
            }
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File imageFile = createImageFile();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mCameraCaptureURI = Uri.fromFile(imageFile);
            } else {
                mCameraCaptureURI = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", imageFile);
            }
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraCaptureURI);
            if (cameraIntent.resolveActivity(activity.getPackageManager()) == null) {
                responseHelper.invoke(E_CANNOT_LAUNCH_CAMERA, "Cannot launch camera");
                return;
            }
            activity.startActivityForResult(cameraIntent, CAMERA_PICKER_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            responseHelper.invoke(E_FAILED_TO_OPEN_CAMERA, e.getMessage());
        }
    }

    @JSMethod
    public void openPicker(final JSONObject options, final JSCallback callback) {
        final Activity activity = getCurrentActivity();
        setConfiguration(options, callback);
        if (!permissionsCheck(activity, Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE), IMAGE_PICKER_REQUEST)) {
            return;
        }
        initiatePicker();
    }

    private void initiatePicker() {
        try {
            final Activity activity = getCurrentActivity();
            final Intent galleryIntent = new Intent(Intent.ACTION_PICK);

            if (cropping || mediaType.equals("photo")) {
                galleryIntent.setType("image/*");
            } else if (mediaType.equals("video")) {
                galleryIntent.setType("video/*");
            } else {
                galleryIntent.setType("*/*");
                String[] mimetypes = {"image/*", "video/*"};
                galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            }

            galleryIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

            final Intent chooserIntent = Intent.createChooser(galleryIntent, "Choose");
            activity.startActivityForResult(chooserIntent, IMAGE_PICKER_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            responseHelper.invoke(E_FAILED_TO_SHOW_PICKER, e.getMessage());
        }
    }

    @JSMethod
    public void openCropper(final JSONObject options, final JSCallback callback) {
        final Activity activity = getCurrentActivity();
        setConfiguration(options, callback);
        Uri uri = Uri.parse(options.getString("path"));
        startCropping(activity, uri);
    }

    private String getBase64StringFromFile(String absoluteFilePath) {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(new File(absoluteFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }

    private JSONObject getSelection(Activity activity, Uri uri, boolean isCamera) throws Exception {
        String path = resolveRealPath(activity, uri, isCamera);
        if (path == null || path.isEmpty()) {
            throw new Exception("Cannot resolve asset path.");
        }
        try {
            String mime = getMimeType(path);
            if (mime != null && mime.startsWith("video/")) {
                return getVideo(activity, path, mime);
            }
            return getImage(activity, path);
        } catch (Exception e) {
            throw e;
        }
    }

    private Bitmap validateVideo(String path) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        Bitmap bmp = retriever.getFrameAtTime();

        if (bmp == null) {
            throw new Exception("Cannot retrieve video data");
        }

        return bmp;
    }

    private JSONObject getVideo(final Activity activity, final String path, final String mime) throws Exception {
        validateVideo(path);
        final String compressedVideoPath = getTmpDir(activity) + "/" + UUID.randomUUID().toString() + ".mp4";
        String videoPath = compression.compressVideo(activity, options, path, compressedVideoPath);
        try {
            Bitmap bmp = validateVideo(videoPath);
            long modificationDate = new File(videoPath).lastModified();
            JSONObject video = new JSONObject();
            video.put("width", bmp.getWidth());
            video.put("height", bmp.getHeight());
            video.put("mime", mime);
            video.put("size", (int) new File(videoPath).length());
            video.put("path", "file://" + videoPath);
            video.put("modificationDate", String.valueOf(modificationDate));
            return video;
        } catch (Exception e) {
            throw e;
        }
    }

    private String resolveRealPath(Activity activity, Uri uri, boolean isCamera) {
        String path;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            path = RealPathUtil.getRealPathFromURI(activity, uri);
        } else {
            if (isCamera) {
                Uri imageUri = Uri.parse(mCurrentPhotoPath);
                path = imageUri.getPath();
            } else {
                path = RealPathUtil.getRealPathFromURI(activity, uri);
            }
        }

        return path;
    }

    private BitmapFactory.Options validateImage(String path) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;

        BitmapFactory.decodeFile(path, options);

        if (options.outMimeType == null || options.outWidth == 0 || options.outHeight == 0) {
            throw new Exception("Invalid image selected");
        }

        return options;
    }

    private JSONObject getImage(final Activity activity, String path) throws Exception {
        JSONObject image = new JSONObject();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            throw new Exception("Cannot select remote files");
        }
        Log.d("ImageCropPicker", path);
        validateImage(path);
        // if compression options are provided image will be compressed. If none options is provided,
        // then original image will be returned
        File compressedImage = compression.compressImage(activity, options, path);
        String compressedImagePath = compressedImage.getPath();
        BitmapFactory.Options options = validateImage(compressedImagePath);
        long modificationDate = new File(path).lastModified();

        image.put("path", "file://" + compressedImagePath);
        image.put("width", options.outWidth);
        image.put("height", options.outHeight);
        image.put("mime", options.outMimeType);
        image.put("size", (int) new File(compressedImagePath).length());
        image.put("modificationDate", String.valueOf(modificationDate));

        if (includeBase64) {
            image.put("data", getBase64StringFromFile(compressedImagePath));
        }

        if (includeExif) {
            try {
                JSONObject exif = ExifExtractor.extract(path);
                image.put("exif", exif);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return image;
    }

    private void configureCropperColors(UCrop.Options options) {
        int activeWidgetColor = Color.parseColor(cropperActiveWidgetColor);
        int toolbarColor = Color.parseColor(cropperToolbarColor);
        int statusBarColor = Color.parseColor(cropperStatusBarColor);
        options.setToolbarColor(toolbarColor);
        options.setStatusBarColor(statusBarColor);
        if (activeWidgetColor == Color.parseColor(DEFAULT_TINT)) {
            /*
            Default tint is grey => use a more flashy color that stands out more as the call to action
            Here we use 'Light Blue 500' from https://material.google.com/style/color.html#color-color-palette
            */
            options.setActiveWidgetColor(Color.parseColor(DEFAULT_WIDGET_COLOR));
        } else {
            //If they pass a custom tint color in, we use this for everything
            options.setActiveWidgetColor(activeWidgetColor);
        }
    }

    private void startCropping(Activity activity, Uri uri) {
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(100);
        options.setCircleDimmedLayer(cropperCircleOverlay);
        options.setFreeStyleCropEnabled(freeStyleCropEnabled);
        options.setShowCropGrid(showCropGuidelines);
        options.setHideBottomControls(hideBottomControls);
        if (cropperToolbarTitle != null) {
            options.setToolbarTitle(cropperToolbarTitle);
        }
        if (enableRotationGesture) {
            // UCropActivity.ALL = enable both rotation & scaling
            options.setAllowedGestures(
                    UCropActivity.ALL, // When 'scale'-tab active
                    UCropActivity.ALL, // When 'rotate'-tab active
                    UCropActivity.ALL  // When 'aspect ratio'-tab active
            );
        }
        if (!disableCropperColorSetters) {
            configureCropperColors(options);
        }

        UCrop.of(uri, Uri.fromFile(new File(this.getTmpDir(activity), UUID.randomUUID().toString() + ".jpg")))
                .withMaxResultSize(width, height)
                .withAspectRatio(width, height)
                .withOptions(options)
                .start(activity);
    }

    private void imagePickerResult(Activity activity, final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            responseHelper.invoke(E_PICKER_CANCELLED_KEY, "User canceled");
        } else if (resultCode == Activity.RESULT_OK) {
            if (multiple) {
                ClipData clipData = data.getClipData();
                try {
                    // only one image selected
                    if (clipData == null) {
                        responseHelper.invoke(E_SUCCESS, getSelection(activity, data.getData(), false));
                    } else {
                        JSONArray list = new JSONArray();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            list.add(getSelection(activity, clipData.getItemAt(i).getUri(), false));
                        }
                        responseHelper.invoke(E_SUCCESS, list);
                    }
                } catch (Exception ex) {
                    responseHelper.invoke(E_NO_IMAGE_DATA_FOUND, ex.getMessage());
                }

            } else {
                Uri uri = data.getData();
                if (uri == null) {
                    responseHelper.invoke(E_NO_IMAGE_DATA_FOUND, "Cannot resolve image url");
                    return;
                }
                if (cropping) {
                    startCropping(activity, uri);
                } else {
                    try {
                        responseHelper.invoke(E_SUCCESS, getSelection(activity, uri, false));
                    } catch (Exception ex) {
                        responseHelper.invoke(E_NO_IMAGE_DATA_FOUND, ex.getMessage());
                    }
                }
            }
        }
    }

    private void cameraPickerResult(Activity activity, final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            responseHelper.invoke(E_PICKER_CANCELLED_KEY, "User canceled");
        } else if (resultCode == Activity.RESULT_OK) {
            Uri uri = mCameraCaptureURI;

            if (uri == null) {
                responseHelper.invoke(E_NO_IMAGE_DATA_FOUND, "Cannot resolve image url");
                return;
            }

            if (cropping) {
                UCrop.Options options = new UCrop.Options();
                options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                startCropping(activity, uri);
            } else {
                try {
                    responseHelper.invoke(E_SUCCESS, getSelection(activity, uri, true));
                } catch (Exception ex) {
                    responseHelper.invoke(E_NO_IMAGE_DATA_FOUND, ex.getMessage());
                }
            }
        }
    }

    private void croppingResult(Activity activity, final int requestCode, final int resultCode, final Intent data) {
        if (data != null) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                try {
                    JSONObject result = getSelection(activity, resultUri, false);
                    result.put("cropRect", ImageCropPickerModule.getCroppedRectMap(data));
                    responseHelper.invoke(E_SUCCESS, result);
                } catch (Exception ex) {
                    responseHelper.invoke(E_NO_IMAGE_DATA_FOUND, ex.getMessage());
                }
            } else {
                responseHelper.invoke(E_NO_IMAGE_DATA_FOUND, "Cannot find image data");
            }
        } else {
            responseHelper.invoke(E_PICKER_CANCELLED_KEY, "User canceled");
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        responseHelper.cleanResponse();
        final Activity activity = getCurrentActivity();
        if (requestCode == IMAGE_PICKER_REQUEST) {
            imagePickerResult(activity, requestCode, resultCode, data);
        } else if (requestCode == CAMERA_PICKER_REQUEST) {
            cameraPickerResult(activity, requestCode, resultCode, data);
        } else if (requestCode == UCrop.REQUEST_CROP) {
            croppingResult(activity, requestCode, resultCode, data);
        }
        super.onActivityResult(resultCode, resultCode, data);
    }

    private boolean isCameraAvailable(Activity activity) {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) || activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private File createImageFile() throws IOException {
        String imageFileName = "image-" + UUID.randomUUID().toString();
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (!path.exists() && !path.isDirectory()) {
            path.mkdirs();
        }
        File image = File.createTempFile(imageFileName, ".jpg", path);
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private static JSONObject getCroppedRectMap(Intent data) {
        final int DEFAULT_VALUE = -1;
        final JSONObject map = new JSONObject();

        map.put("x", data.getIntExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, DEFAULT_VALUE));
        map.put("y", data.getIntExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, DEFAULT_VALUE));
        map.put("width", data.getIntExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, DEFAULT_VALUE));
        map.put("height", data.getIntExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, DEFAULT_VALUE));

        return map;
    }

    @NonNull
    private Activity getCurrentActivity() {
        return (Activity) mWXSDKInstance.getContext();
    }

    @Override
    public void onActivityCreate() {
        super.onActivityCreate();
    }
}