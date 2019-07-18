package com.owncloud.android;

/**
 * @author Guy Tavor (guy@scoompa.com)
 * @since Jul 17, 2019
 */
public class Dhamma {

  private static boolean isAdmin = false;
  // TODO: guy: integrate into UI to allow admin access.
  public static boolean isAdmin() { return isAdmin; }

  public static void setAdmin(boolean admin) {
    Dhamma.isAdmin = admin;
  }
}
