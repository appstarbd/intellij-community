// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model;

import com.intellij.openapi.application.Application;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <h3>Example 1</h3>
 * {@linkplain com.intellij.psi.SmartPsiElementPointer Smart pointers} might be used to restore the element across different read actions.
 * <p>
 * Elements are expected to stay valid within a single {@link Application#runReadAction read action}.
 * It's highly advised to split long read actions into several short ones, but this also means
 * that some {@link Application#runWriteAction write action} might be run in between these short read actions,
 * which could potentially change the model of the element (reference model, PSI model, framework model or whatever model).
 * </p>
 *
 * <h3>Example 2</h3>
 * Pointers might be used to avoid hard references to the element to save the memory.
 * In this case the pointer stores minimal needed information to be able to restore the element when requested.
 *
 * @param <T> type of underlying element
 */
@Experimental
public interface Pointer<T> {

  /**
   * @return value or {@code null} if the value was invalidated or cannot be restored
   */
  @Nullable T dereference();

  boolean equals(Object o);

  int hashCode();

  /**
   * Creates a pointer which holds strong reference to the {@code value}.
   * The pointer always dereferences into the passed {@code value}.
   * Hard pointers should be used only for values that cannot be invalidated.
   */
  @Contract(value = "_ -> new", pure = true)
  static <T> @NotNull Pointer<T> hardPointer(@NotNull T value) {
    return new HardPointer<>(value);
  }

  /**
   * Creates a pointer which uses {@code underlyingPointer} value to restore its value with {@code restoration} function.
   * <p/>
   * Equality of {@code restoration} function is unreliable, because it might be a lambda.
   * The {@code key} must be passed to check for equality instead,
   * where two equal keys mean the same restoration logic will be applied.
   */
  @Contract(value = "_, _, _ -> new", pure = true)
  static <T, U> @NotNull Pointer<T> delegatingPointer(@NotNull Pointer<? extends U> underlyingPointer,
                                                      @NotNull Object key,
                                                      @NotNull Function<? super U, ? extends T> restoration) {
    return new DelegatingPointer.ByValue<>(underlyingPointer, key, restoration);
  }

  /**
   * Creates same pointer as {@link #delegatingPointer}, which additionally passes itself
   * into the {@code restoration} function to allow caching the pointer in the restored value.
   */
  @Contract(value = "_, _, _ -> new", pure = true)
  static <T, U> @NotNull Pointer<T> uroborosPointer(@NotNull Pointer<? extends U> underlyingPointer,
                                                    @NotNull Object key,
                                                    @NotNull BiFunction<? super U, ? super Pointer<T>, ? extends T> restoration) {
    return new DelegatingPointer.ByValueAndPointer<>(underlyingPointer, key, restoration);
  }
}

final class HardPointer<T> implements Pointer<T> {

  private final T myValue;

  HardPointer(@NotNull T value) {
    myValue = value;
  }

  @NotNull
  @Override
  public T dereference() {
    return myValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HardPointer<?> pointer = (HardPointer<?>)o;
    return Objects.equals(myValue, pointer.myValue);
  }

  @Override
  public int hashCode() {
    return myValue.hashCode();
  }
}
