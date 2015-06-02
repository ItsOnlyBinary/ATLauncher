/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.injector.binding;

import com.atlauncher.injector.Linker;

public final class SingletonBinding<T> implements Binding<T> {
    private static final Object UNINITIALIZED = new Object();

    private final Binding<T> delegate;
    private volatile Object instance = UNINITIALIZED;

    public SingletonBinding(Binding<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void link(Linker linker) {
        this.delegate.link(linker);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        if (instance == UNINITIALIZED) {
            synchronized (this) {
                if (instance == UNINITIALIZED) {
                    instance = this.delegate.get();
                }
            }
        }

        return (T) this.instance;
    }

    @Override
    public String toString() {
        return "SingletonBinding[delegate=" + this.delegate + "]";
    }
}