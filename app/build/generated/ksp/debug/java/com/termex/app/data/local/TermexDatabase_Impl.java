package com.termex.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TermexDatabase_Impl extends TermexDatabase {
  private volatile ServerDao _serverDao;

  private volatile WorkplaceDao _workplaceDao;

  private volatile SnippetDao _snippetDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `servers` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `hostname` TEXT NOT NULL, `port` INTEGER NOT NULL, `username` TEXT NOT NULL, `authMode` TEXT NOT NULL, `passwordKeychainID` TEXT, `keyId` TEXT, `workplaceId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workplaces` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `snippets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `command` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '028b042ce1d5d803956dbf559af20d6e')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `servers`");
        db.execSQL("DROP TABLE IF EXISTS `workplaces`");
        db.execSQL("DROP TABLE IF EXISTS `snippets`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsServers = new HashMap<String, TableInfo.Column>(9);
        _columnsServers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("hostname", new TableInfo.Column("hostname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("port", new TableInfo.Column("port", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("authMode", new TableInfo.Column("authMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("passwordKeychainID", new TableInfo.Column("passwordKeychainID", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("keyId", new TableInfo.Column("keyId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("workplaceId", new TableInfo.Column("workplaceId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServers = new TableInfo("servers", _columnsServers, _foreignKeysServers, _indicesServers);
        final TableInfo _existingServers = TableInfo.read(db, "servers");
        if (!_infoServers.equals(_existingServers)) {
          return new RoomOpenHelper.ValidationResult(false, "servers(com.termex.app.data.local.ServerEntity).\n"
                  + " Expected:\n" + _infoServers + "\n"
                  + " Found:\n" + _existingServers);
        }
        final HashMap<String, TableInfo.Column> _columnsWorkplaces = new HashMap<String, TableInfo.Column>(2);
        _columnsWorkplaces.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkplaces.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkplaces = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWorkplaces = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWorkplaces = new TableInfo("workplaces", _columnsWorkplaces, _foreignKeysWorkplaces, _indicesWorkplaces);
        final TableInfo _existingWorkplaces = TableInfo.read(db, "workplaces");
        if (!_infoWorkplaces.equals(_existingWorkplaces)) {
          return new RoomOpenHelper.ValidationResult(false, "workplaces(com.termex.app.data.local.WorkplaceEntity).\n"
                  + " Expected:\n" + _infoWorkplaces + "\n"
                  + " Found:\n" + _existingWorkplaces);
        }
        final HashMap<String, TableInfo.Column> _columnsSnippets = new HashMap<String, TableInfo.Column>(4);
        _columnsSnippets.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnippets.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnippets.put("command", new TableInfo.Column("command", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnippets.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSnippets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSnippets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSnippets = new TableInfo("snippets", _columnsSnippets, _foreignKeysSnippets, _indicesSnippets);
        final TableInfo _existingSnippets = TableInfo.read(db, "snippets");
        if (!_infoSnippets.equals(_existingSnippets)) {
          return new RoomOpenHelper.ValidationResult(false, "snippets(com.termex.app.data.local.SnippetEntity).\n"
                  + " Expected:\n" + _infoSnippets + "\n"
                  + " Found:\n" + _existingSnippets);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "028b042ce1d5d803956dbf559af20d6e", "1f2c8d00bdd05d875b08a43d02d8fc40");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "servers","workplaces","snippets");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `servers`");
      _db.execSQL("DELETE FROM `workplaces`");
      _db.execSQL("DELETE FROM `snippets`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ServerDao.class, ServerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WorkplaceDao.class, WorkplaceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SnippetDao.class, SnippetDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ServerDao serverDao() {
    if (_serverDao != null) {
      return _serverDao;
    } else {
      synchronized(this) {
        if(_serverDao == null) {
          _serverDao = new ServerDao_Impl(this);
        }
        return _serverDao;
      }
    }
  }

  @Override
  public WorkplaceDao workplaceDao() {
    if (_workplaceDao != null) {
      return _workplaceDao;
    } else {
      synchronized(this) {
        if(_workplaceDao == null) {
          _workplaceDao = new WorkplaceDao_Impl(this);
        }
        return _workplaceDao;
      }
    }
  }

  @Override
  public SnippetDao snippetDao() {
    if (_snippetDao != null) {
      return _snippetDao;
    } else {
      synchronized(this) {
        if(_snippetDao == null) {
          _snippetDao = new SnippetDao_Impl(this);
        }
        return _snippetDao;
      }
    }
  }
}
