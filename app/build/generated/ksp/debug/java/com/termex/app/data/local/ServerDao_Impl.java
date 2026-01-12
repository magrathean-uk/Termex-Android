package com.termex.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.termex.app.domain.AuthMode;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ServerDao_Impl implements ServerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ServerEntity> __insertionAdapterOfServerEntity;

  private final EntityDeletionOrUpdateAdapter<ServerEntity> __deletionAdapterOfServerEntity;

  private final EntityDeletionOrUpdateAdapter<ServerEntity> __updateAdapterOfServerEntity;

  public ServerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfServerEntity = new EntityInsertionAdapter<ServerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `servers` (`id`,`name`,`hostname`,`port`,`username`,`authMode`,`passwordKeychainID`,`keyId`,`workplaceId`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getHostname());
        statement.bindLong(4, entity.getPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, __AuthMode_enumToString(entity.getAuthMode()));
        if (entity.getPasswordKeychainID() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPasswordKeychainID());
        }
        if (entity.getKeyId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getKeyId());
        }
        if (entity.getWorkplaceId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getWorkplaceId());
        }
      }
    };
    this.__deletionAdapterOfServerEntity = new EntityDeletionOrUpdateAdapter<ServerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `servers` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfServerEntity = new EntityDeletionOrUpdateAdapter<ServerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `servers` SET `id` = ?,`name` = ?,`hostname` = ?,`port` = ?,`username` = ?,`authMode` = ?,`passwordKeychainID` = ?,`keyId` = ?,`workplaceId` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getHostname());
        statement.bindLong(4, entity.getPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, __AuthMode_enumToString(entity.getAuthMode()));
        if (entity.getPasswordKeychainID() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPasswordKeychainID());
        }
        if (entity.getKeyId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getKeyId());
        }
        if (entity.getWorkplaceId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getWorkplaceId());
        }
        statement.bindString(10, entity.getId());
      }
    };
  }

  @Override
  public Object insertServer(final ServerEntity server,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServerEntity.insert(server);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteServer(final ServerEntity server,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfServerEntity.handle(server);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateServer(final ServerEntity server,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfServerEntity.handle(server);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ServerEntity>> getAllServers() {
    final String _sql = "SELECT * FROM servers ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"servers"}, new Callable<List<ServerEntity>>() {
      @Override
      @NonNull
      public List<ServerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfHostname = CursorUtil.getColumnIndexOrThrow(_cursor, "hostname");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthMode = CursorUtil.getColumnIndexOrThrow(_cursor, "authMode");
          final int _cursorIndexOfPasswordKeychainID = CursorUtil.getColumnIndexOrThrow(_cursor, "passwordKeychainID");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfWorkplaceId = CursorUtil.getColumnIndexOrThrow(_cursor, "workplaceId");
          final List<ServerEntity> _result = new ArrayList<ServerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpHostname;
            _tmpHostname = _cursor.getString(_cursorIndexOfHostname);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final AuthMode _tmpAuthMode;
            _tmpAuthMode = __AuthMode_stringToEnum(_cursor.getString(_cursorIndexOfAuthMode));
            final String _tmpPasswordKeychainID;
            if (_cursor.isNull(_cursorIndexOfPasswordKeychainID)) {
              _tmpPasswordKeychainID = null;
            } else {
              _tmpPasswordKeychainID = _cursor.getString(_cursorIndexOfPasswordKeychainID);
            }
            final String _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getString(_cursorIndexOfKeyId);
            }
            final String _tmpWorkplaceId;
            if (_cursor.isNull(_cursorIndexOfWorkplaceId)) {
              _tmpWorkplaceId = null;
            } else {
              _tmpWorkplaceId = _cursor.getString(_cursorIndexOfWorkplaceId);
            }
            _item = new ServerEntity(_tmpId,_tmpName,_tmpHostname,_tmpPort,_tmpUsername,_tmpAuthMode,_tmpPasswordKeychainID,_tmpKeyId,_tmpWorkplaceId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getServerById(final String id,
      final Continuation<? super ServerEntity> $completion) {
    final String _sql = "SELECT * FROM servers WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ServerEntity>() {
      @Override
      @Nullable
      public ServerEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfHostname = CursorUtil.getColumnIndexOrThrow(_cursor, "hostname");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthMode = CursorUtil.getColumnIndexOrThrow(_cursor, "authMode");
          final int _cursorIndexOfPasswordKeychainID = CursorUtil.getColumnIndexOrThrow(_cursor, "passwordKeychainID");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfWorkplaceId = CursorUtil.getColumnIndexOrThrow(_cursor, "workplaceId");
          final ServerEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpHostname;
            _tmpHostname = _cursor.getString(_cursorIndexOfHostname);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final AuthMode _tmpAuthMode;
            _tmpAuthMode = __AuthMode_stringToEnum(_cursor.getString(_cursorIndexOfAuthMode));
            final String _tmpPasswordKeychainID;
            if (_cursor.isNull(_cursorIndexOfPasswordKeychainID)) {
              _tmpPasswordKeychainID = null;
            } else {
              _tmpPasswordKeychainID = _cursor.getString(_cursorIndexOfPasswordKeychainID);
            }
            final String _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getString(_cursorIndexOfKeyId);
            }
            final String _tmpWorkplaceId;
            if (_cursor.isNull(_cursorIndexOfWorkplaceId)) {
              _tmpWorkplaceId = null;
            } else {
              _tmpWorkplaceId = _cursor.getString(_cursorIndexOfWorkplaceId);
            }
            _result = new ServerEntity(_tmpId,_tmpName,_tmpHostname,_tmpPort,_tmpUsername,_tmpAuthMode,_tmpPasswordKeychainID,_tmpKeyId,_tmpWorkplaceId);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ServerEntity>> getServersByWorkplace(final String workplaceId) {
    final String _sql = "SELECT * FROM servers WHERE workplaceId = ? ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, workplaceId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"servers"}, new Callable<List<ServerEntity>>() {
      @Override
      @NonNull
      public List<ServerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfHostname = CursorUtil.getColumnIndexOrThrow(_cursor, "hostname");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthMode = CursorUtil.getColumnIndexOrThrow(_cursor, "authMode");
          final int _cursorIndexOfPasswordKeychainID = CursorUtil.getColumnIndexOrThrow(_cursor, "passwordKeychainID");
          final int _cursorIndexOfKeyId = CursorUtil.getColumnIndexOrThrow(_cursor, "keyId");
          final int _cursorIndexOfWorkplaceId = CursorUtil.getColumnIndexOrThrow(_cursor, "workplaceId");
          final List<ServerEntity> _result = new ArrayList<ServerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpHostname;
            _tmpHostname = _cursor.getString(_cursorIndexOfHostname);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final AuthMode _tmpAuthMode;
            _tmpAuthMode = __AuthMode_stringToEnum(_cursor.getString(_cursorIndexOfAuthMode));
            final String _tmpPasswordKeychainID;
            if (_cursor.isNull(_cursorIndexOfPasswordKeychainID)) {
              _tmpPasswordKeychainID = null;
            } else {
              _tmpPasswordKeychainID = _cursor.getString(_cursorIndexOfPasswordKeychainID);
            }
            final String _tmpKeyId;
            if (_cursor.isNull(_cursorIndexOfKeyId)) {
              _tmpKeyId = null;
            } else {
              _tmpKeyId = _cursor.getString(_cursorIndexOfKeyId);
            }
            final String _tmpWorkplaceId;
            if (_cursor.isNull(_cursorIndexOfWorkplaceId)) {
              _tmpWorkplaceId = null;
            } else {
              _tmpWorkplaceId = _cursor.getString(_cursorIndexOfWorkplaceId);
            }
            _item = new ServerEntity(_tmpId,_tmpName,_tmpHostname,_tmpPort,_tmpUsername,_tmpAuthMode,_tmpPasswordKeychainID,_tmpKeyId,_tmpWorkplaceId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __AuthMode_enumToString(@NonNull final AuthMode _value) {
    switch (_value) {
      case PASSWORD: return "PASSWORD";
      case KEY: return "KEY";
      case AUTO: return "AUTO";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private AuthMode __AuthMode_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "PASSWORD": return AuthMode.PASSWORD;
      case "KEY": return AuthMode.KEY;
      case "AUTO": return AuthMode.AUTO;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
