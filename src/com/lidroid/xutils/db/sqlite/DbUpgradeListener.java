package com.lidroid.xutils.db.sqlite;

public interface DbUpgradeListener {
    public void onUpgrade(IDatabase db, int oldVersion, int newVersion);
}