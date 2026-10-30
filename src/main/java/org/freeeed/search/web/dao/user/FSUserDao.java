/*
 *
 * Copyright SHMsoft, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.freeeed.search.web.dao.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.freeeed.search.web.model.User;

/**
 * 
 * Class FSUserDao.
 * 
 * Implements UserDao interface with File system storage.
 * 
 * @author ilazarov.
 *
 */
public class FSUserDao implements UserDao {
    private static final String USERS_FILE = "work/u.dat";

    /** Where the first-run admin password is left for the operator to read once. */
    private static final String INITIAL_PASSWORD_FILE = "work/initial-admin-password.txt";

    /** Lets the appliance provision a known admin password without a prompt. */
    private static final String ADMIN_PASSWORD_ENV = "FREEEED_ADMIN_PASSWORD";

    private static final String ADMIN_USERNAME = "admin";

    private static final Logger log = Logger.getLogger(FSUserDao.class);
    
    private Map<String, User> userCache;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    
    public void init() {
        log.info("Init FS User DAO...");
        userCache = new HashMap<String, User>();
        
        loadUsers();
        createAdminUser();
    }
    
    @Override
    public User findUser(String username) {
        lock.readLock().lock();
        
        try {
            return userCache.get(username);
        } finally {
            lock.readLock().unlock();
        }
        
    }

    @Override
    public void deleteUser(String username) {
        lock.writeLock().lock();
        
        try {
            userCache.remove(username);
            storeUsers();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void saveUser(User user) {
        lock.writeLock().lock();
        
        try {
            userCache.put(user.getUsername(), user);
            storeUsers();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public User login(String username, String password) {
        User user;
        String stored;

        lock.readLock().lock();
        try {
            log.debug("Log in user: " + username);

            user = userCache.get(username);
            stored = (user != null) ? user.getPassword() : null;
        } finally {
            lock.readLock().unlock();
        }

        // Verify against the stored hash. PasswordHash.verify also accepts a
        // legacy plaintext record, so an existing work/u.dat keeps working.
        if (user == null || !PasswordHash.verify(password, stored)) {
            return null;
        }

        // Successful login is the only moment the plaintext is in hand, so it
        // is where a legacy record can be migrated to a hash.
        if (!PasswordHash.isHashed(stored)) {
            upgradeStoredPassword(username, password);
        }

        return user;
    }

    /**
     * Replace a legacy plaintext password with a hash. Done after the read lock
     * is released, because a ReentrantReadWriteLock cannot upgrade in place.
     */
    private void upgradeStoredPassword(String username, String plaintext) {
        lock.writeLock().lock();

        try {
            User user = userCache.get(username);
            if (user != null && !PasswordHash.isHashed(user.getPassword())) {
                user.setPassword(PasswordHash.hash(plaintext));
                storeUsers();
                log.info("Migrated stored password for '" + username + "' from plaintext to a hash.");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public List<User> listUsers() {
        List<User> result = new ArrayList<User>();
        lock.readLock().lock();
        
        try {
            result.addAll(userCache.values());
        } finally {
            lock.readLock().unlock();
        }
        
        return result;
    }

    private void storeUsers() {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        
        try {
            File dir = new File(USERS_FILE);
            File parent = dir.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            
            fos = new FileOutputStream(dir);
            oos = new ObjectOutputStream(fos);
            
            oos.writeObject(userCache);
            
            oos.close();
            fos.close();
        } catch (Exception e) {
            log.error("Problem storing users from file system!", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error("Problem closing", e);
                }
            }
            
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    log.error("Problem closing", e);
                }
            }
        }
    }
    
    private void loadUsers() {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(USERS_FILE);
            ois = new ObjectInputStream(fis);
            
            @SuppressWarnings("unchecked")
            Map<String, User> data = (Map<String, User>) ois.readObject();
            if (data != null) {
                userCache = data;
            }
        } catch (java.io.FileNotFoundException e) {
            // First run: no users have been saved yet. Not an error -- the cache
            // starts empty and the file is created on the first save. (Logged loudly
            // as ERROR before, which made a normal fresh install look broken.)
            log.info("No stored users yet (" + USERS_FILE + "); starting with an empty user cache.");
        } catch (Exception e) {
            log.error("Problem loading users from file system!", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error("Problem closing", e);
                }
            }
            
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    log.error("Problem closing", e);
                }
            }
        }
    }
    
    private void createAdminUser() {
        if (userCache.containsKey(ADMIN_USERNAME)) {
            return;
        }

        // First run. Never seed a known password: the old default was
        // 'admin'/'admin', pre-filled in the login form and compared in
        // plaintext, so any fresh install was trivially accessible to anyone
        // who could reach the port (#68). The appliance supplies its own via
        // FREEEED_ADMIN_PASSWORD; everyone else gets a random one, written
        // once to work/initial-admin-password.txt for the operator.
        String fromEnv = System.getenv(ADMIN_PASSWORD_ENV);
        boolean generated = (fromEnv == null || fromEnv.trim().isEmpty());
        String password = generated ? PasswordHash.generatePassword() : fromEnv.trim();

        User adminUser = new User();
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Super Admin");
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setPassword(PasswordHash.hash(password));
        adminUser.setEmail("admin@example.com");
        
        adminUser.addRight(User.Right.APP_CONFIG);
        adminUser.addRight(User.Right.DOCUMENT_TAG);
        adminUser.addRight(User.Right.PROCESS);
        adminUser.addRight(User.Right.USERS_ADMIN);
        adminUser.addRight(User.Right.CASES);
        
        userCache.put(adminUser.getUsername(), adminUser);

        // Persist it. The old seeding path never saved, so the default account
        // was silently recreated on every startup and a changed password could
        // be undone by a restart.
        storeUsers();

        if (generated) {
            publishInitialPassword(password);
        } else {
            log.info("Admin password taken from " + ADMIN_PASSWORD_ENV + ".");
        }
    }

    /**
     * Write the generated first-run admin password where the operator can read
     * it, owner-only, and say so loudly in the log. This is the one time it
     * exists in plaintext; only the hash is kept in work/u.dat.
     */
    private void publishInitialPassword(String password) {
        File file = new File(INITIAL_PASSWORD_FILE);
        PrintWriter out = null;

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Create it empty and restrict it BEFORE the password goes in, so
            // there is no window where it is world-readable.
            if (!file.exists()) {
                file.createNewFile();
            }
            restrictToOwner(file);

            out = new PrintWriter(file, "UTF-8");
            out.println("FreeEed Review -- first-run administrator account");
            out.println();
            out.println("  username: " + ADMIN_USERNAME);
            out.println("  password: " + password);
            out.println();
            out.println("Sign in, change this password, then delete this file.");
            out.flush();

            log.warn("First-run admin password written to " + file.getAbsolutePath()
                    + " -- sign in, change it, then delete that file.");
        } catch (Exception e) {
            // The account still exists and is still usable; the operator just
            // has to read the password from the log instead. Say it plainly
            // rather than failing startup.
            log.error("Could not write " + INITIAL_PASSWORD_FILE
                    + "; first-run admin password is: " + password, e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /** Owner-only permissions, via POSIX where available and the File API elsewhere. */
    private void restrictToOwner(File file) {
        try {
            Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(file.toPath(), ownerOnly);
            return;
        } catch (UnsupportedOperationException e) {
            // Not a POSIX filesystem (Windows) -- fall through.
        } catch (Exception e) {
            log.debug("Could not set POSIX permissions on " + file, e);
        }

        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

}
