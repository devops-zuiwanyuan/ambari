/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.encryption;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CredentialProvider {
  public static final Pattern PASSWORD_ALIAS_PATTERN =
    Pattern.compile("\\$\\{alias=[\\w\\.]+\\}");

  protected char[] chars = { 'a', 'b', 'c', 'd', 'e', 'f', 'g',
    'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
    'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
    'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    '2', '3', '4', '5', '6', '7', '8', '9'};

  private CredentialStoreService keystoreService;
  static final Logger LOG = LoggerFactory.getLogger(CredentialProvider.class);

  public CredentialProvider(String masterKey, String masterKeyLocation,
              boolean isMasterKeyPersisted) throws AmbariException {
    MasterKeyService masterKeyService;
    if (masterKey != null) {
      masterKeyService = new MasterKeyServiceImpl(masterKey);
    } else {
      masterKeyService = new MasterKeyServiceImpl(masterKeyLocation,
        isMasterKeyPersisted);
    }
    if (!masterKeyService.isMasterKeyInitialized()) {
      throw new AmbariException("Master key initialization failed.");
    }
    String storeDir = masterKeyLocation.substring(0,
      masterKeyLocation.indexOf(Configuration.MASTER_KEY_FILENAME_DEFAULT));
    this.keystoreService = new CredentialStoreServiceImpl(storeDir);
    this.keystoreService.setMasterKeyService(masterKeyService);
  }

  public char[] getPasswordForAlias(String alias) throws AmbariException {
    if (isAliasString(alias))
      return keystoreService.getCredential(getAliasFromString(alias));
    return keystoreService.getCredential(alias);
  }

  public void generateAliasWithPassword(String alias) throws AmbariException {
    String passwordString = generatePassword(16);
    addAliasToCredentialStore(alias, passwordString);
  }

  public void addAliasToCredentialStore(String alias, String passwordString)
    throws AmbariException {
    if (alias == null || alias.isEmpty())
      throw new IllegalArgumentException("Alias cannot be null or empty.");
    if (passwordString == null || passwordString.isEmpty())
      throw new IllegalArgumentException("Empty or null password not allowed" +
        ".");
    keystoreService.addCredential(alias, passwordString);
  }

  private String generatePassword(int length) {
    StringBuffer sb = new StringBuffer();
    Random r = new Random();
    for (int i = 0; i < length; i++) {
      sb.append(chars[r.nextInt(chars.length)]);
    }
    return sb.toString();
  }

  public static boolean isAliasString(String aliasStr) {
    if (aliasStr == null || aliasStr.isEmpty())
      return false;
    Matcher matcher = PASSWORD_ALIAS_PATTERN.matcher(aliasStr);
    return matcher.matches();
  }

  private String getAliasFromString(String strPasswd) {
    return strPasswd.substring(strPasswd.indexOf("=") + 1,
      strPasswd.length() - 1);
  }

  protected CredentialStoreService getKeystoreService() {
    return keystoreService;
  }

  /**
   * Credential Store entry point
   * args[0] => Action (GET/PUT)
   * args[1] => Alias
   * args[2] => Payload (FilePath for GET/Password for PUT)
   * args[3] => Master Key (Empty)
   * @param args
   *
   */
  public static void main(String args[]) {
    if (args != null && args.length > 0) {
      String action = args[0];
      String alias = null;
      String masterKey = null;
      CredentialProvider credentialProvider = null;
      Configuration configuration = new Configuration();
      if (args.length > 1 && !args[1].isEmpty()) {
        alias = args[1];
      } else {
        LOG.error("No valid arguments provided.");
        System.exit(1);
      }
      // None - To avoid incorrectly assuming redirection as argument
      if (args.length > 3 && !args[3].isEmpty() && !args[3].equalsIgnoreCase
        ("None")) {
        masterKey = args[3];
        LOG.debug("Master key provided as an argument.");
      }
      try {
        credentialProvider = new CredentialProvider(masterKey,
          configuration.getMasterKeyLocation(),
          configuration.isMasterKeyPersisted());
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(1);
      }
      LOG.info("action => " + action + ", alias => " + alias);
      if (action.equalsIgnoreCase("PUT")) {
        String password = null;
        if (args.length > 2 && !args[2].isEmpty()) {
          password = args[2];
        }
        if (alias != null && !alias.isEmpty()
          && password != null && !password.isEmpty()) {
          try {
            credentialProvider.addAliasToCredentialStore(alias, password);
          } catch (AmbariException e) {
            e.printStackTrace();
          }
        } else {
          LOG.error("Alias and password are required arguments.");
          System.exit(1);
        }
      } else if (action.equalsIgnoreCase("GET")) {
        String writeFilePath = null;
        if (args.length > 2 && !args[2].isEmpty()) {
          writeFilePath = args[2];
        }
        if (alias != null && !alias.isEmpty() && writeFilePath != null &&
          !writeFilePath.isEmpty()) {
          String passwd = "";
          try {
            char[] retPasswd = credentialProvider.getPasswordForAlias(alias);
            if (retPasswd != null) {
              passwd = new String(retPasswd);
            }
          } catch (AmbariException e) {
            LOG.error("Error retrieving password for alias.");
            e.printStackTrace();
          }
          FileOutputStream fo = null;
          try {
            fo = new FileOutputStream(writeFilePath);
            fo.write(passwd.getBytes());
          } catch (FileNotFoundException fe) {
            fe.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            if (fo != null) {
              try {
                fo.close();
              } catch (IOException e) {
              }
            }
          }
        } else {
          LOG.error("Alias and file path are required arguments.");
        }
      } else if (action.equalsIgnoreCase("RESET")) {

      }
    } else {
      LOG.error("No arguments provided to " + "CredentialProvider");
      System.exit(1);
    }
    System.exit(0);
  }
}
