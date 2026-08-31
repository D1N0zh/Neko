package com.neko.finance;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int SAVE_FILE_REQUEST = 1002;
    private static final int ATTACHMENT_PICK_REQUEST = 1003;
    private static final long MAX_ATTACHMENT_BYTES = 20L * 1024L * 1024L;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private byte[] pendingSaveContent;
    private String pendingAttachmentMovementId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        webView.setOnApplyWindowInsetsListener((view, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets safeArea = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                left = safeArea.left;
                top = safeArea.top;
                right = safeArea.right;
                bottom = safeArea.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left, top, right, bottom);
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? WindowInsets.CONSUMED
                    : insets.consumeSystemWindowInsets();
        });
        setContentView(webView);
        webView.requestApplyInsets();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.addJavascriptInterface(new AppInfoBridge(), "NekoApp");

        webView.clearCache(true);
        webView.clearHistory();
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams
            ) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent;
                try {
                    intent = fileChooserParams.createIntent();
                } catch (Exception e) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                }

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html?v=signed111-ui-icon-polish");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private class AppInfoBridge {
        @JavascriptInterface
        public String getVersionName() { return BuildConfig.VERSION_NAME; }

        @JavascriptInterface
        public int getVersionCode() { return BuildConfig.VERSION_CODE; }

        @JavascriptInterface
        public String getBuildRevision() { return BuildConfig.BUILD_REVISION; }

        @JavascriptInterface
        @SuppressWarnings("deprecation")
        public void setDarkMode(boolean dark) {
            runOnUiThread(() -> {
                int background = Color.parseColor(dark ? "#11141B" : "#F6F7F9");
                getWindow().setStatusBarColor(background);
                getWindow().setNavigationBarColor(background);
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = dark ? flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags = dark ? flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                decor.setSystemUiVisibility(flags);
            });
        }

        @JavascriptInterface
        public void saveTextFile(String fileName, String mimeType, String content) {
            pendingSaveContent = content.getBytes(StandardCharsets.UTF_8);
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, fileName);
                startActivityForResult(intent, SAVE_FILE_REQUEST);
            });
        }

        @JavascriptInterface
        public void chooseAttachments(String movementId) {
            if (!isSafePathPart(movementId)) return;
            pendingAttachmentMovementId = movementId;
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try { startActivityForResult(intent, ATTACHMENT_PICK_REQUEST); }
                catch (Exception ignored) {
                    pendingAttachmentMovementId = null;
                    notifyAttachmentsChanged(movementId, 0, 1);
                }
            });
        }

        @JavascriptInterface
        public String listAttachments(String movementId) {
            JSONArray result = new JSONArray();
            File directory = attachmentDirectory(movementId, false);
            if (directory == null || !directory.isDirectory()) return result.toString();
            File[] files = directory.listFiles(File::isFile);
            if (files == null) return result.toString();
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (File file : files) {
                try {
                    JSONObject item = new JSONObject();
                    item.put("id", file.getName());
                    item.put("name", attachmentDisplayName(file.getName()));
                    item.put("size", file.length());
                    item.put("mime", attachmentMimeType(file.getName()));
                    result.put(item);
                } catch (Exception ignored) { }
            }
            return result.toString();
        }

        @JavascriptInterface
        public String getAttachmentCounts() {
            JSONObject result = new JSONObject();
            File root = new File(getFilesDir(), "attachments");
            File[] directories = root.listFiles(File::isDirectory);
            if (directories == null) return result.toString();
            for (File directory : directories) {
                File[] files = directory.listFiles(File::isFile);
                if (files != null && files.length > 0) {
                    try { result.put(directory.getName(), files.length); } catch (Exception ignored) { }
                }
            }
            return result.toString();
        }

        @JavascriptInterface
        public boolean removeAttachment(String movementId, String attachmentId) {
            File file = attachmentFile(movementId, attachmentId);
            return file != null && file.isFile() && file.delete();
        }

        @JavascriptInterface
        public void deleteAllAttachments() {
            deleteRecursively(new File(getFilesDir(), "attachments"));
        }

        @JavascriptInterface
        public void openAttachment(String movementId, String attachmentId) {
            File file = attachmentFile(movementId, attachmentId);
            if (file == null || !file.isFile()) return;
            runOnUiThread(() -> {
                Uri uri = AttachmentProvider.uriForFile(MainActivity.this, movementId, attachmentId);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, attachmentMimeType(attachmentId));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { startActivity(intent); }
                catch (Exception ignored) {
                    webView.evaluateJavascript("window.onNekoAttachmentOpenFailed&&window.onNekoAttachmentOpenFailed()", null);
                }
            });
        }
    }

    private boolean isSafePathPart(String value) {
        return value != null && !value.isEmpty() && value.matches("[A-Za-z0-9_.-]{1,160}");
    }

    private File attachmentDirectory(String movementId, boolean create) {
        if (!isSafePathPart(movementId)) return null;
        File root = new File(getFilesDir(), "attachments");
        File directory = new File(root, movementId);
        try {
            String rootPath = root.getCanonicalPath() + File.separator;
            if (!directory.getCanonicalPath().startsWith(rootPath)) return null;
        } catch (Exception e) { return null; }
        if (create && !directory.exists() && !directory.mkdirs()) return null;
        return directory;
    }

    private File attachmentFile(String movementId, String attachmentId) {
        if (!isSafePathPart(movementId) || attachmentId == null || !attachmentId.matches("[A-Za-z0-9._-]{1,220}")) return null;
        File directory = attachmentDirectory(movementId, false);
        if (directory == null) return null;
        File file = new File(directory, attachmentId);
        try {
            if (!file.getCanonicalPath().startsWith(directory.getCanonicalPath() + File.separator)) return null;
        } catch (Exception e) { return null; }
        return file;
    }

    private String attachmentDisplayName(String storedName) {
        int separator = storedName.indexOf("__");
        return separator >= 0 && separator + 2 < storedName.length() ? storedName.substring(separator + 2) : storedName;
    }

    private boolean deleteRecursively(File target) {
        if (target == null || !target.exists()) return true;
        File[] children = target.listFiles();
        if (children != null) for (File child : children) if (!deleteRecursively(child)) return false;
        return target.delete();
    }

    private String attachmentMimeType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".heic")) return "image/heic";
        if (lower.endsWith(".heif")) return "image/heif";
        String guessed = URLConnection.guessContentTypeFromName(name);
        return guessed == null ? "application/octet-stream" : guessed;
    }

    private String extensionForMime(String mimeType) {
        if ("application/pdf".equals(mimeType)) return ".pdf";
        if ("image/jpeg".equals(mimeType)) return ".jpg";
        if ("image/png".equals(mimeType)) return ".png";
        if ("image/webp".equals(mimeType)) return ".webp";
        if ("image/gif".equals(mimeType)) return ".gif";
        if ("image/heic".equals(mimeType)) return ".heic";
        if ("image/heif".equals(mimeType)) return ".heif";
        return "";
    }

    private String displayNameForUri(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        } catch (Exception ignored) { }
        if (name == null || name.trim().isEmpty()) name = "allegato";
        name = name.replaceAll("[^A-Za-z0-9._-]", "_").trim();
        if (name.length() > 100) name = name.substring(name.length() - 100);
        return name.isEmpty() ? "allegato" : name;
    }

    private boolean copyAttachment(Uri uri, String movementId) {
        File directory = attachmentDirectory(movementId, true);
        if (directory == null) return false;
        String displayName = displayNameForUri(uri);
        String mimeType;
        try { mimeType = getContentResolver().getType(uri); }
        catch (Exception e) { return false; }
        if (mimeType == null) mimeType = attachmentMimeType(displayName);
        if (!(mimeType.startsWith("image/") || "application/pdf".equals(mimeType))) return false;
        if (!displayName.contains(".")) displayName += extensionForMime(mimeType);
        File target = new File(directory, UUID.randomUUID().toString().replace("-", "") + "__" + displayName);
        long copied = 0;
        InputStream openedInput;
        try { openedInput = getContentResolver().openInputStream(uri); }
        catch (Exception e) { return false; }
        if (openedInput == null) return false;
        try (InputStream input = openedInput; FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                copied += read;
                if (copied > MAX_ATTACHMENT_BYTES) throw new IllegalStateException("Attachment too large");
                output.write(buffer, 0, read);
            }
            if (copied > 0) return true;
            target.delete();
            return false;
        } catch (Exception e) {
            if (target.exists()) target.delete();
            return false;
        }
    }

    private void notifyAttachmentsChanged(String movementId, int added, int failed) {
        if (webView == null) return;
        String script = "window.onNekoAttachmentsChanged&&window.onNekoAttachmentsChanged(" +
                JSONObject.quote(movementId) + "," + added + "," + failed + ")";
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(script, null); });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ATTACHMENT_PICK_REQUEST) {
            String movementId = pendingAttachmentMovementId;
            pendingAttachmentMovementId = null;
            List<Uri> selectedUris = new ArrayList<>();
            if (resultCode == RESULT_OK && data != null && movementId != null) {
                if (data.getClipData() != null) {
                    int count = Math.min(10, data.getClipData().getItemCount());
                    for (int i = 0; i < count; i++) {
                        selectedUris.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    selectedUris.add(data.getData());
                }
            }
            if (movementId == null || selectedUris.isEmpty()) {
                notifyAttachmentsChanged(movementId == null ? "" : movementId, 0, 0);
                return;
            }
            new Thread(() -> {
                int added = 0;
                int failed = 0;
                for (Uri uri : selectedUris) {
                    if (copyAttachment(uri, movementId)) added++; else failed++;
                }
                notifyAttachmentsChanged(movementId, added, failed);
            }, "neko-attachment-copy").start();
            return;
        }
        if (requestCode == SAVE_FILE_REQUEST) {
            boolean saved = false;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingSaveContent != null) {
                try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
                    if (output != null) {
                        output.write(pendingSaveContent);
                        saved = true;
                    }
                } catch (Exception ignored) {
                    saved = false;
                }
            }
            pendingSaveContent = null;
            if (webView != null) {
                webView.evaluateJavascript("window.onNekoFileSaved&&window.onNekoFileSaved(" + saved + ")", null);
            }
            return;
        }
        if (requestCode == FILE_CHOOSER_REQUEST) {
            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
            }

            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }
        webView.evaluateJavascript(
                "window.nekoHandleAndroidBack ? window.nekoHandleAndroidBack() : 'exit'",
                result -> {
                    if ("\"exit\"".equals(result)) {
                        MainActivity.super.onBackPressed();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
