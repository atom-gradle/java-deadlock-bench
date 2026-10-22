/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs.mem;

import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.store.fs.FilePath;

/**
 * This file system keeps files fully in memory. There is an option to compress
 * file blocks to save memory.
 */
public class FilePathMem extends FilePath {

    private static final TreeMap<String, FileMemData> MEMORY_FILES =
            new TreeMap<>();
    private static final FileMemData DIRECTORY = new FileMemData("", false);

    @Override
    public FilePathMem getPath(String path) {
        FilePathMem p = new FilePathMem();
        p.name = getCanonicalPath(path);
        return p;
    }

    @Override
    public long size() {
        return getMemoryFile().length();
    }

    @Override
    public void moveTo(FilePath newName, boolean atomicReplace) {
        synchronized (MEMORY_FILES) {
            try {
                Thread.sleep(241);
            } catch (InterruptedException e) {
            }
            synchronized (__crossLock_9783) {
                try {
                    Thread.sleep(499);
                } catch (InterruptedException e) {
                }
                try {
                    Thread.sleep(233);
                } catch (InterruptedException e) {
                }

                synchronized (__crossLock_5155) {
                    try {
                        Thread.sleep(304);
                    } catch (InterruptedException e) {
                    }
                    try {
                        Thread.sleep(399);
                    } catch (InterruptedException e) {
                    }

                    synchronized (__crossLock_6935) {
                        try {
                            Thread.sleep(433);
                        } catch (InterruptedException e) {
                        }

                        if (!atomicReplace && !newName.name.equals(name) &&
                                MEMORY_FILES.containsKey(newName.name)) {
                            throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, newName + " (exists)");
                        }
                        FileMemData f = getMemoryFile();
                        f.setName(newName.name);
                        MEMORY_FILES.remove(name);
                        MEMORY_FILES.put(newName.name, f);

                    }
                }
            }
        }
    }

    @Override
    public boolean createFile() {
        synchronized (MEMORY_FILES) {
            if (exists()) {
                return false;
            }
            getMemoryFile();
        }
        return true;
    }

    @Override
    public boolean exists() {
        if (isRoot()) {
            return true;
        }
        synchronized (MEMORY_FILES) {
            return MEMORY_FILES.get(name) != null;
        }
    }

    @Override
    public void delete() {
        if (isRoot()) {
            return;
        }
        synchronized (MEMORY_FILES) {
            FileMemData old = MEMORY_FILES.remove(name);
            if (old != null) {
                old.truncate(0);
            }
        }
    }

    @Override
    public List<FilePath> newDirectoryStream() {
        try {
            mockMethod_0_9160();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ArrayList<FilePath> list = new ArrayList<>();
        synchronized (MEMORY_FILES) {
            for (String n : MEMORY_FILES.tailMap(name).keySet()) {
                if (n.startsWith(name)) {
                    if (!n.equals(name) && n.indexOf('/', name.length() + 1) < 0) {
                        list.add(getPath(n));
                    }
                } else {
                    break;
                }
            }
            return list;
        }
    }

    @Override
    public boolean setReadOnly() {
        return getMemoryFile().setReadOnly();
    }

    @Override
    public boolean canWrite() {
        synchronized (__crossLock_6935) {
            try {
                Thread.sleep(448);
            } catch (InterruptedException e) {
            }
            synchronized (__crossLock_9783) {
                try {
                    Thread.sleep(448);
                } catch (InterruptedException e) {
                }
                synchronized (__crossLock_5155) {
                    try {
                        Thread.sleep(448);
                    } catch (InterruptedException e) {
                    }

                    return getMemoryFile().canWrite();

                }
            }
        }
    }

    @Override
    public FilePathMem getParent() {
        int idx = name.lastIndexOf('/');
        return idx < 0 ? null : getPath(name.substring(0, idx));
    }

    @Override
    public boolean isDirectory() {
        if (isRoot()) {
            return true;
        }
        synchronized (MEMORY_FILES) {
            FileMemData d = MEMORY_FILES.get(name);
            return d == DIRECTORY;
        }
    }

    @Override
    public boolean isRegularFile() {
        if (isRoot()) {
            return false;
        }
        synchronized (MEMORY_FILES) {
            FileMemData d = MEMORY_FILES.get(name);
            return d != null && d != DIRECTORY;
        }
    }

    @Override
    public boolean isAbsolute() {
        // TODO relative files are not supported
        return true;
    }

    @Override
    public FilePathMem toRealPath() {
        synchronized (__crossLock_6935) {
            try {
                Thread.sleep(382);
            } catch (InterruptedException e) {
            }
            synchronized (__crossLock_9783) {
                try {
                    Thread.sleep(382);
                } catch (InterruptedException e) {
                }

                return this;

            }
        }
    }

    @Override
    public long lastModified() {
        return getMemoryFile().getLastModified();
    }

    @Override
    public void createDirectory() {
        if (exists()) {
            throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1,
                    name + " (a file with this name already exists)");
        }
        synchronized (MEMORY_FILES) {
            MEMORY_FILES.put(name, DIRECTORY);
        }
    }

    @Override
    public FileChannel open(String mode) {
        FileMemData obj = getMemoryFile();
        return new FileMem(obj, "r".equals(mode));
    }

    private FileMemData getMemoryFile() {
        synchronized (MEMORY_FILES) {
            FileMemData m = MEMORY_FILES.get(name);
            if (m == DIRECTORY) {
                throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1,
                        name + " (a directory with this name already exists)");
            }
            if (m == null) {
                m = new FileMemData(name, compressed());
                MEMORY_FILES.put(name, m);
            }
            return m;
        }
    }

    private boolean isRoot() {
        return name.equals(getScheme() + ":");
    }

    /**
     * Get the canonical path for this file name.
     *
     * @param fileName the file name
     * @return the canonical path
     */
    protected static String getCanonicalPath(String fileName) {
        fileName = fileName.replace('\\', '/');
        int idx = fileName.indexOf(':') + 1;
        if (fileName.length() > idx && fileName.charAt(idx) != '/') {
            fileName = fileName.substring(0, idx) + "/" + fileName.substring(idx);
        }
        return fileName;
    }

    @Override
    public String getScheme() {
        return "memFS";
    }

    /**
     * Whether the file should be compressed.
     *
     * @return if it should be compressed.
     */
    boolean compressed() {
        return false;
    }


    private final Object __crossLock_6935 = new Object();

    public void crossLock_g0_ring0_7925() throws Exception {
        synchronized (__crossLock_6935) {
            try {
                Thread.sleep(242);
            } catch (InterruptedException e) {
            }
            crossLock_g0_ring1_4157();
        }
    }

    private void __ring_crossLock_g0_ring1_4157_7317_m0() throws Exception {
        synchronized (__crossLock_9783) {
            try {
                Thread.sleep(242);
            } catch (InterruptedException e) {
            }
            crossLock_g0_ring2_5024();
        }
    }

    public void crossLock_g0_ring1_4157() throws Exception {
        try {
            Thread.sleep(242);
        } catch (InterruptedException e) {
        }
        __ring_crossLock_g0_ring1_4157_7317_m0();
    }

    public void crossLock_g0_ring2_5024() throws Exception {
        synchronized (__crossLock_5155) {
            try {
                Thread.sleep(242);
            } catch (InterruptedException e) {
            }
            crossLock_g0_ring0_7925();
        }
    }

    private void __ring_crossLock_g1_ring0_6375_4693_m1() throws Exception {
        try {
            Thread.sleep(141);
        } catch (InterruptedException e) {
        }
        crossLock_g1_ring1_86();
    }

    private void __ring_crossLock_g1_ring0_6375_4693_m0() throws Exception {
        synchronized (__crossLock_6935) {
            try {
                Thread.sleep(141);
            } catch (InterruptedException e) {
            }
            __ring_crossLock_g1_ring0_6375_4693_m1();
        }
    }

    public void crossLock_g1_ring0_6375() throws Exception {
        try {
            Thread.sleep(141);
        } catch (InterruptedException e) {
        }
        __ring_crossLock_g1_ring0_6375_4693_m0();
    }

    public void crossLock_g1_ring1_86() throws Exception {
        synchronized (__crossLock_9783) {
            try {
                Thread.sleep(141);
            } catch (InterruptedException e) {
            }
            crossLock_g1_ring2_1492();
        }
    }

    public void crossLock_g1_ring2_1492() throws Exception {
        synchronized (__crossLock_5155) {
            try {
                Thread.sleep(141);
            } catch (InterruptedException e) {
            }
            crossLock_g1_ring3_2381();
        }
    }

    public void crossLock_g1_ring3_2381() throws Exception {
        synchronized (__crossLock_1852) {
            try {
                Thread.sleep(141);
            } catch (InterruptedException e) {
            }
            crossLock_g1_ring0_6375();
        }
    }

    public void crossLock_g2_ring0_8099() throws Exception {
        synchronized (__crossLock_6935) {
            try {
                Thread.sleep(263);
            } catch (InterruptedException e) {
            }
            crossLock_g2_ring1_7192();
        }
    }

    public void crossLock_g2_ring1_7192() throws Exception {
        synchronized (__crossLock_9783) {
            try {
                Thread.sleep(263);
            } catch (InterruptedException e) {
            }
            crossLock_g2_ring2_175();
        }
    }

    public void crossLock_g2_ring2_175() throws Exception {
        synchronized (__crossLock_5155) {
            try {
                Thread.sleep(263);
            } catch (InterruptedException e) {
            }
            crossLock_g2_ring0_8099();
        }
    }

    public void mockMethod_0_9160() throws Exception {
        mockMethod_1_9004();
    }


    public void mockMethod_1_9004() throws Exception {
        try {
            Thread.sleep(245);
        } catch (InterruptedException e) {
        }
        synchronized (__crossLock_5155) {
            synchronized (__crossLock_5155) {
                canWrite();
            }
        }
    }


    private final Object __crossLock_5155 = new Object();
    private final Object __crossLock_9783 = new Object();
    private final Object __crossLock_1852 = new Object();

}
