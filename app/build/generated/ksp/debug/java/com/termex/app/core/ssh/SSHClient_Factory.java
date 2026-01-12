package com.termex.app.core.ssh;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class SSHClient_Factory implements Factory<SSHClient> {
  @Override
  public SSHClient get() {
    return newInstance();
  }

  public static SSHClient_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SSHClient newInstance() {
    return new SSHClient();
  }

  private static final class InstanceHolder {
    private static final SSHClient_Factory INSTANCE = new SSHClient_Factory();
  }
}
