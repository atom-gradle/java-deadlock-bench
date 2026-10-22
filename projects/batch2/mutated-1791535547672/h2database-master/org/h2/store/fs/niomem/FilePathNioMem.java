/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs.niomem;

import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.store.fs.FilePath;

/**
 * This file system keeps files fully in off-java-heap memory. There is an option to compress
 * file blocks to save memory.
 */
public class FilePathNioMem extends FilePath {

    private static final TreeMap<String, FileNioMemData> MEMORY_FILES =
            new TreeMap<>();

    /**
     * The percentage of uncompressed (cached) entries.
     */
    float compressLaterCachePercent = 1;

    @Override
    public FilePathNioMem getPath(String path) {
        synchronized (__crossLock_5091) {
            try { Thread.sleep(167); } catch (InterruptedException e) { }
            try { Thread.sleep(491); } catch (InterruptedException e) { }
            synchronized (__crossLock_2285) {
                try { Thread.sleep(394); } catch (InterruptedException e) { }
                try { Thread.sleep(491); } catch (InterruptedException e) { }
                synchronized (__crossLock_2747) {
                    try { Thread.sleep(451); } catch (InterruptedException e) { }
                    try { Thread.sleep(491); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9071) {
                        try { Thread.sleep(491); } catch (InterruptedException e) { }

                FilePathNioMem p = new FilePathNioMem();
                p.name = getCanonicalPath(path);
                return p;
    
                    }
                }
            }
        }}

    @Override
    public long size() {
        return getMemoryFile().length();
    }

    @Override
    public void moveTo(FilePath newName, boolean atomicReplace) {
        synchronized (MEMORY_FILES) {
            synchronized (__crossLock_2285) {
                try { Thread.sleep(161); } catch (InterruptedException e) { }

                        synchronized (__crossLock_2747) {
                            try { Thread.sleep(498); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_5091) {
                                        try { Thread.sleep(179); } catch (InterruptedException e) { }

                                                if (!atomicReplace && !name.equals(newName.name) &&
                                                        MEMORY_FILES.containsKey(newName.name)) {
                                                    throw DbException.get(ErrorCode.FILE_RENAME_FAILED_2, name, newName + " (exists)");
                                                }
                                                FileNioMemData f = getMemoryFile();
                                                f.setName(newName.name);
                                                MEMORY_FILES.remove(name);
                                                MEMORY_FILES.put(newName.name, f);
        
                                    }
                        }
            }}
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
            MEMORY_FILES.remove(name);
        }
    }

    @Override
    public List<FilePath> newDirectoryStream() {
        ArrayList<FilePath> list = new ArrayList<>();
        synchronized (MEMORY_FILES) {
            for (String n : MEMORY_FILES.tailMap(name).keySet()) {
                if (n.startsWith(name)) {
                    list.add(getPath(n));
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
        return getMemoryFile().canWrite();
    }

    @Override
    public FilePathNioMem getParent() {
        int idx = name.lastIndexOf('/');
        return idx < 0 ? null : getPath(name.substring(0, idx));
    }

    @Override
    public boolean isDirectory() {
        if (isRoot()) {
            return true;
        }
        // TODO in memory file system currently
        // does not really support directories
        synchronized (MEMORY_FILES) {
            return MEMORY_FILES.get(name) == null;
        }
    }

    @Override
    public boolean isRegularFile() {
        if (isRoot()) {
            return false;
        }
        // TODO in memory file system currently
        // does not really support directories
        synchronized (MEMORY_FILES) {
            return MEMORY_FILES.get(name) != null;
        }
    }

    @Override
    public boolean isAbsolute() {
        // TODO relative files are not supported
        return true;
    }

    @Override
    public FilePathNioMem toRealPath() {
        try {
            mockMethod_0_2389();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public long lastModified() {
        return getMemoryFile().getLastModified();
    }

    @Override
    public void createDirectory() {
        synchronized (__crossLock_5091) {
            try { Thread.sleep(290); } catch (InterruptedException e) { }
            synchronized (__crossLock_2285) {
                try { Thread.sleep(290); } catch (InterruptedException e) { }

                if (exists() && isDirectory()) {
                    throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1,
                            name + " (a file with this name already exists)");
                }
                // TODO directories are not really supported
    
            }
        }}

    @Override
    public FileChannel open(String mode) {
        FileNioMemData obj = getMemoryFile();
        return new FileNioMem(obj, "r".equals(mode));
    }

    private FileNioMemData getMemoryFile() {
        synchronized (MEMORY_FILES) {
            FileNioMemData m = MEMORY_FILES.get(name);
            if (m == null) {
                m = new FileNioMemData(name, compressed(), compressLaterCachePercent);
                MEMORY_FILES.put(name, m);
            }
            return m;
        }
    }

    protected boolean isRoot() {
        return name.equals(getScheme() + ":");
    }

    /**
     * Get the canonical path of a file (with backslashes replaced with forward
     * slashes).
     *
     * @param fileName the file name
     * @return the canonical path
     */
    protected static String getCanonicalPath(String fileName) {
        fileName = fileName.replace('\\', '/');
        int idx = fileName.lastIndexOf(':') + 1;
        if (fileName.length() > idx && fileName.charAt(idx) != '/') {
            fileName = fileName.substring(0, idx) + "/" + fileName.substring(idx);
        }
        return fileName;
    }

    @Override
    public String getScheme() {
        return "nioMemFS";
    }

    /**
     * Whether the file should be compressed.
     *
     * @return true if it should be compressed.
     */
    boolean compressed() {
        return false;
    }

}



    private final Object __crossLock_5091 = new Object();

    private void __ring_crossLock_g0_ring0_2996_1972_m0() throws Exception {
        synchronized (__crossLock_5091) {
            try { Thread.sleep(225); } catch (InterruptedException e) { }
            crossLock_g0_ring1_8374();
        }
    }

    public void crossLock_g0_ring0_2996() throws Exception {
        try { Thread.sleep(225); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_2996_1972_m0();
    }

    private void __ring_crossLock_g0_ring1_8374_5689_m0() throws Exception {
        synchronized (__crossLock_2285) {
            try { Thread.sleep(225); } catch (InterruptedException e) { }
            crossLock_g0_ring2_4591();
        }
    }

    public void crossLock_g0_ring1_8374() throws Exception {
        try { Thread.sleep(225); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_8374_5689_m0();
    }

    public void crossLock_g0_ring2_4591() throws Exception {
        synchronized (__crossLock_2747) {
            try { Thread.sleep(225); } catch (InterruptedException e) { }
            crossLock_g0_ring3_6518();
        }
    }

    public void crossLock_g0_ring3_6518() throws Exception {
        synchronized (__crossLock_9071) {
            try { Thread.sleep(225); } catch (InterruptedException e) { }
            crossLock_g0_ring0_2996();
        }
    }

    public void crossLock_g1_ring0_3452() throws Exception {
        synchronized (__crossLock_5091) {
            try { Thread.sleep(272); } catch (InterruptedException e) { }
            crossLock_g1_ring1_6216();
        }
    }

    public void crossLock_g1_ring1_6216() throws Exception {
        synchronized (__crossLock_2285) {
            try { Thread.sleep(272); } catch (InterruptedException e) { }
            crossLock_g1_ring0_3452();
        }
    }

    public void crossLock_g2_ring0_5621() throws Exception {
        synchronized (__crossLock_5091) {
            try { Thread.sleep(220); } catch (InterruptedException e) { }
            crossLock_g2_ring1_5995();
        }
    }

    public void crossLock_g2_ring1_5995() throws Exception {
        synchronized (__crossLock_2285) {
            try { Thread.sleep(220); } catch (InterruptedException e) { }
            crossLock_g2_ring0_5621();
        }
    }

    public void syncBlock___crossLock_5091_4422() {
        synchronized (__crossLock_5091) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_2389() throws Exception {
        synchronized (__crossLock_9071) {
            getPath("");
        }
    }


    private final Object __crossLock_2747 = new Object();
    private final Object __crossLock_2285 = new Object();
    private final Object __crossLock_9071 = new Object();

