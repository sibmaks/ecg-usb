package xyz.dma.ecg_usb.collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by maksim.drobyshev on 05-Apr-21.
 */
public class FixedSizeList<T> implements List<T> {
    private final Lock lock = new ReentrantLock();
    private final int maxSize;
    private final T[] content;
    private int size;

    public FixedSizeList(int maxSize) {
        if(maxSize <= 0) {
            throw new IllegalArgumentException("Max size should be at least 1");
        }
        this.maxSize = maxSize;
        this.content = (T[]) new Object[maxSize];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(@Nullable @org.jetbrains.annotations.Nullable Object o) {
        T[] content = this.content;
        for (T t : content) {
            if (Objects.equals(o, t)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @NotNull
    @Override
    public Iterator<T> iterator() {
        T[] copy = (T[]) toArray();
        return new Iterator<T>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < copy.length;
            }

            @Override
            public T next() {
                return copy[index++];
            }
        };
    }

    @NonNull
    @NotNull
    @Override
    public Object[] toArray() {
        T[] content = this.content;
        T[] copy = (T[]) new Object[content.length];
        System.arraycopy(content, 0, copy, 0, copy.length);
        return copy;
    }

    @NonNull
    @NotNull
    @Override
    public <T1> T1[] toArray(@NonNull @NotNull T1[] a) {
        return (T1[]) toArray();
    }

    @Override
    public boolean add(T t) {
        lock.lock();
        try {
            if (size < maxSize) {
                content[size++] = t;
                return true;
            } else if(size == maxSize) {
                if (size >= 1) {
                    System.arraycopy(content, 1, content, 0, size - 1);
                }
                content[size - 1] = t;
                return true;
            } else {
                throw new IllegalStateException("Size more when max size on more when one element");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(@Nullable @org.jetbrains.annotations.Nullable Object o) {
        lock.lock();
        try {
            int index = indexOf(o);
            if(index < 0) {
                return false;
            }
            remove(index);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsAll(@NonNull @NotNull Collection<?> c) {
        for(Object o : c) {
            if(!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull @NotNull Collection<? extends T> c) {
        for(T o : c) {
            if(!add(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(int index, @NonNull @NotNull Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NonNull @NotNull Collection<?> c) {
        for(Object o : c) {
            if(!remove(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean retainAll(@NonNull @NotNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            size = 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T get(int index) {
        if(index < 0 || index > size) {
            throw new IllegalArgumentException("index is less when zero or more when size");
        }
        return content[index];
    }

    @Override
    public T set(int index, T element) {
        lock.lock();
        try {
            if (index < 0 || index > size) {
                throw new IllegalArgumentException("index is less when zero or more when size");
            }

            T old = content[index];
            content[index] = element;
            return old;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void add(int index, T element) {
        lock.lock();
        try {
            if (index < 0 || index > size) {
                throw new IllegalArgumentException("index is less when zero or more when size");
            }
            int leftBound = Math.max(0, index - 1);
            System.arraycopy(content, 1, content, 0, leftBound);
            content[index] = element;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T remove(int index) {
        lock.lock();
        try {
            if (index < 0 || index > size) {
                throw new IllegalArgumentException("index is less when zero or more when size");
            }
            T oldVal = content[index];
            int leftBound = Math.max(0, index - 1);
            int rightBound = Math.max(size, index + 1);
            System.arraycopy(content, 1, content, 0, leftBound);
            if (size - rightBound >= 0) {
                System.arraycopy(content, rightBound + 1, content, rightBound, size - rightBound);
            }
            size--;
            return oldVal;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int indexOf(@Nullable @org.jetbrains.annotations.Nullable Object o) {
        T[] content = this.content;
        for(int index = 0; index <= size; index++) {
            if(Objects.equals(o, content[index])) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(@Nullable @org.jetbrains.annotations.Nullable Object o) {
        T[] content = this.content;
        for(int index = size; index > 0; index--) {
            if(Objects.equals(o, content[index])) {
                return index;
            }
        }
        return -1;
    }

    @NonNull
    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex > size) {
            throw new IllegalArgumentException("fromIndex is less when zero or more when size");
        }
        if (toIndex < 0 || toIndex > size) {
            throw new IllegalArgumentException("toIndex is less when zero or more when size");
        }
        if(fromIndex >= toIndex) {
            throw new IllegalArgumentException("fromIndex must be greate when toIndex");
        }

        return new ArrayList<>(Arrays.asList(this.content).subList(fromIndex, toIndex));
    }
}
