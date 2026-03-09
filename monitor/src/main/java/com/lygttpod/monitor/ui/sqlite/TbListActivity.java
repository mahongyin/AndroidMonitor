package com.lygttpod.monitor.ui.sqlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.lygttpod.monitor.R;
import com.lygttpod.monitor.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TbListActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    private String mPath;
    private SQLiteDatabase mDatabase;
    private List<String> mTableNames;
    private String mTableName;

    private ListView mTables;
    private ArrayAdapter mAdapter;

    public static Intent buildIntent(@NonNull Context requireContext, @Nullable String dbPath) {
        Intent intent = new Intent(requireContext, TbListActivity.class);
        intent.setDataAndType(Uri.parse("file://" + dbPath), "application/x-sqlite3");
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tblist);
        mTables = findViewById(R.id.table);

        Uri uri = getIntent().getData();
        if (uri == null || uri.getScheme() == null || uri.getPath() == null) {
            Toast.makeText(this, getIntent().toString(), Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("TAG", "Uri: " + uri.toString());
        mPath = getFileForUri(uri).getPath();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            mPath = getFileForUri0(uri).getPath();
//        } else {
//            mPath = uri.getPath();
//        }
        main();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_exec) {
            View view = View.inflate(TbListActivity.this, R.layout.dialog_edit, null);
            EditText editText = view.findViewById(R.id.edit_text);
            editText.setHint(R.string.hint_sql);
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.action_exec_sql)
                    .setView(view)
                    .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                        try {
                            mDatabase.execSQL(editText.getText().toString());
                            Toast.makeText(this, R.string.message_success, Toast.LENGTH_SHORT).show();
                            refresh();
                        } catch (Exception e) {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .create()
                    .show();
            // 添加保存菜单项
        } else if (itemId == R.id.action_save) {
            if (mIsExternalAppDatabase) {
                saveDatabaseWithRoot();
            } else {
                Toast.makeText(this, "本地数据库自动保存", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_confirm)
                    .setMessage(R.string.message_irreversible)
                    .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                        mDatabase.execSQL("DROP TABLE \"" + mTableName + "\"");
                        refresh();
                    })
                    .create()
                    .show();
        }
        return true;
    }

//    @Override
//    protected void onDestroy() {
//        if (mDatabase != null) {
//            mDatabase.close();
//        }
//        super.onDestroy();
//    }

    private void main() {
        try {
            openDatabase();
            mTableNames = getTableNames();
            initView();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void refresh() {
        mTableNames.clear();
        mTableNames.addAll(getTableNames());
        mAdapter.notifyDataSetChanged();
    }

    private void initView() {
        mTables.setAdapter(mAdapter = new ArrayAdapter<>(
                this, R.layout.item_table, R.id.table_name, mTableNames));
        mTables.setOnItemClickListener((parent, view, position, id) ->
                startActivity(new Intent(TbListActivity.this, TableActivity.class)
                        .putExtra(TableActivity.EXTRA_DATABASE_PATH, mPath)
                        .putExtra(TableActivity.EXTRA_TABLE_NAME, mTableNames.get(position))));
        mTables.setOnItemLongClickListener((parent, view, position, id) -> {
            mTableName = mTableNames.get(position);
            PopupMenu popup = new PopupMenu(TbListActivity.this, view);
            popup.getMenuInflater().inflate(R.menu.main_popup, popup.getMenu());
            popup.setOnMenuItemClickListener(this);
            popup.show();
            return true;
        });
    }

    private void openDatabase0() {
        String[] arr = mPath.split("/");
        setTitle("`" + arr[arr.length - 1].replaceAll("\\.db$", "") + "`");
        mDatabase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READWRITE, sqLiteDatabase -> {
        });
    }

    private String mOriginalPath;  // 添加：保存原始路径
    private boolean mIsExternalAppDatabase = false;  // 添加：标记是否是外部应用数据库

    private void openDatabase() {
        String[] arr = mPath.split("/");
        String dbName = arr[arr.length - 1];
        setTitle("`" + dbName.replaceAll("\\.db$", "") + "`");

        // 检查是否是其他应用的私有目录（需要 root 权限）
        if (mPath.startsWith("/data/data/") && !mPath.contains(getPackageName())) {
            // 保存原始路径
            mOriginalPath = mPath;
            mIsExternalAppDatabase = true;

            // 使用 root 权限复制到本地
            mPath = copyDatabaseToExt(mPath);
            if (mPath == null) {
                throw new RuntimeException("无法复制数据库文件，请确保设备已 root");
            }
        } else {
            mOriginalPath = null;
            mIsExternalAppDatabase = false;
        }

        mDatabase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READWRITE, sqLiteDatabase -> {
        });
    }

    /**
     * 复制到本地缓存目录
     */
    private String copyDatabaseToExt(String sourcePath) {
        try {
            // 目标路径：当前应用的缓存目录
            File cacheDir = getExternalFilesDir("db");
            File targetFile = new File(cacheDir, "copied_" + System.currentTimeMillis() + ".db");
            //将mPath复制到mOriginalPath
            boolean isSuccess = FileUtils.copyFileToAppDirectory(sourcePath, targetFile.getAbsolutePath());
            if (isSuccess && targetFile.exists()) {
                Toast.makeText(this, "已复制到临时文件", Toast.LENGTH_SHORT).show();
                return targetFile.getAbsolutePath();
            } else {
                throw new RuntimeException("复制后文件不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "复制失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    /**
     * 使用 root 权限将修改后的数据库保存回原位置
     */
    private boolean saveDatabaseWithRoot() {
        if (!mIsExternalAppDatabase || mOriginalPath == null) {
            Toast.makeText(this, "这是本地数据库，无需特殊保存", Toast.LENGTH_SHORT).show();
            return true;
        }

        try {
            if (mDatabase != null && mDatabase.isOpen()) {
                mDatabase.close();
            }

            // 确认对话框
            new AlertDialog.Builder(this)
                    .setTitle("确认保存")
                    .setMessage("确定要将修改保存到: " + mOriginalPath + " ？")
                    .setNegativeButton("取消", (dialog, which) -> {
                        // 重新打开数据库
                        reopenDatabase();
                    })
                    .setPositiveButton("确定", (dialog, which) -> {
                        performSaveDatabase();
                    })
                    .setCancelable(false)
                    .show();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * 执行实际的保存操作
     */
    private void performSaveDatabase() {
        new Thread(() -> {
            try {
                //File sourceFile = new File(mPath);
                //String cpCmd = String.format("cp \"%s\" \"%s\"", mPath, mOriginalPath);
                //将mPath复制到mOriginalPath
                boolean isSuccess = FileUtils.copyFileToAppDirectory(mPath, mOriginalPath);

                if (isSuccess) {
                    runOnUiThread(() -> {
                        Toast.makeText(TbListActivity.this, "保存成功！", Toast.LENGTH_SHORT).show();
                        reopenDatabase();
                    });
                } else {
                    throw new RuntimeException("保存失败: " + mPath);
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(TbListActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    reopenDatabase();
                });
            }
        }).start();
    }


    /**
     * 重新打开数据库
     */
    private void reopenDatabase() {
        try {
            if (mDatabase != null && !mDatabase.isOpen()) {
                mDatabase = SQLiteDatabase.openDatabase(mPath, null,
                        SQLiteDatabase.OPEN_READWRITE, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private List<String> getTableNames() {
        List<String> list = new ArrayList<>();
        if (mDatabase != null) {
            Cursor cursor = mDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name", null);
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
            cursor.close();
            Collections.sort(list, Collator.getInstance(Locale.getDefault()));
        }
        return list;
    }

    private File getFileForUri0(Uri uri) {
        String path = uri.getEncodedPath();

        assert path != null;
        final int splitIndex = path.indexOf('/', 1);
        path = Uri.decode(path.substring(splitIndex + 1));
        File file = new File("/storage", path);
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
        }

        return file;
    }

    private File getFileForUri(Uri uri) {
        return new File(getRealPathFromUri(this, uri));
    }

    /**
     * 根据URI获取文件的真实路径，适配不同Android版本
     */
    private String getRealPathFromUri(Context context, Uri uri) {
        String scheme = uri.getScheme();

        // 1. file:// scheme (直接获取路径)
        if ("file".equals(scheme)) {
            return uri.getPath();
        }

        // 2. content:// scheme (需要转换)
        if ("content".equals(scheme)) {
            // Android 10+ 使用 Document API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return getPathFromUri(context, uri);
            }
            // Android 4.4+ 使用传统方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return getPathFromUriBelowQ(context, uri);
            }
            // Android 4.3 及以下
            return getRealPathFromUriBelowKitKat(context, uri);
        }

        // 3. 其他 scheme (如自定义的 sqlitel://)
        return uri.toString();
    }

    /**
     * Android 10+ 获取文件路径
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getPathFromUri(Context context, Uri uri) {
        // 如果是文件类型URI，直接返回
        if (DocumentsContract.isDocumentUri(context, uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);

            // 下载文件
            if (isDownloadsDocument(uri)) {
                if (docId.startsWith("raw:")) {
                    return docId.substring(4);
                }
                // 尝试通过 content provider 查询
                return getDownloadFilePath(context, uri);
            }

            // 媒体文件
            if (isMediaDocument(uri)) {
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;

                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }

            // 外部存储文档
            if (isExternalStorageDocument(uri)) {
                final String[] split = docId.split(":");
                if (split.length >= 2) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
        }

        // 直接从 MediaStore 查询
        String path = getDataColumn(context, uri, null, null);
        if (path != null) {
            return path;
        }

        return uri.toString();
    }

    /**
     * Android 4.4 - Android 9 获取文件路径
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String getPathFromUriBelowQ(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);

            // 外部存储
            if (isExternalStorageDocument(uri)) {
                final String[] split = docId.split(":");
                if ("primary".equalsIgnoreCase(split[0])) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // 下载文件
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // 媒体文件
            else if (isMediaDocument(uri)) {
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (和 Gallery)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Google Photos 或其他相册
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Android 4.3 及以下获取文件路径
     */
    private String getRealPathFromUriBelowKitKat(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * 从数据库列获取数据
     */
    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            // 忽略异常
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 获取下载文件路径
     */
    private String getDownloadFilePath(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.DATA};
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Downloads.DATA);
                if (dataIndex != -1) {
                    return cursor.getString(dataIndex);
                }
            }
        } catch (Exception e) {
            // 忽略异常
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    // URI 类型判断工具方法
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @Override
    protected void onDestroy() {
        // 如果是外部应用的数据库，提示保存
        if (mIsExternalAppDatabase && mOriginalPath != null && mDatabase != null && mDatabase.isOpen()) {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("数据库已修改，是否保存到原位置？")
                    .setNegativeButton("不保存", (dialog, which) -> {
                        if (mDatabase != null) {
                            mDatabase.close();
                        }
                        finish();
                    })
                    .setPositiveButton("保存", (dialog, which) -> {
                        if (saveDatabaseWithRoot()) {
                            // saveDatabaseWithRoot 会处理后续逻辑
                        } else {
                            finish();
                        }
                    })
                    .setOnCancelListener(dialog -> {
                        if (mDatabase != null) {
                            mDatabase.close();
                        }
                        finish();
                    })
                    .show();
        } else {
            if (mDatabase != null) {
                mDatabase.close();
            }
            super.onDestroy();
        }
    }

}
