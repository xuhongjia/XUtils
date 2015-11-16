package com.lidroid.xutils.db.sqlite;

import android.content.Context;
import android.text.TextUtils;


//******************************************** config ******************************************************

public class DaoConfig {
    private Context context;
    private String dbName = "xUtils.db"; // default db name
    private int dbVersion = 1;
    private DbUpgradeListener dbUpgradeListener;

    private String dbDir;

    public DaoConfig(Context context) {
        this.context = context.getApplicationContext();
    }

    public Context getContext() {
        return context;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        if (!TextUtils.isEmpty(dbName)) {
            this.dbName = dbName;
        }
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public DbUpgradeListener getDbUpgradeListener() {
        return dbUpgradeListener;
    }

    public void setDbUpgradeListener(DbUpgradeListener dbUpgradeListener) {
        this.dbUpgradeListener = dbUpgradeListener;
    }

    public String getDbDir() {
        return dbDir;
    }

    /**
     * set database dir
     *
     * @param dbDir If dbDir is null or empty, use the app default db dir.
     */
    public void setDbDir(String dbDir) {
        this.dbDir = dbDir;
    }
}