package com.owncloud.android;

/**
 * @author Guy Tavor (guy@scoompa.com)
 * @since Jul 17, 2019
 */
public class Dhamma {

  private static boolean isAdmin = false;
  // TODO: guy: integrate into UI to allow admin access.
  public static boolean isAdmin() { return isAdmin; }

  /** @returns toggles admn mode and returns whether we are in admin mode after the toggle.*/
  public static boolean toggleAdmin() {
    return Dhamma.isAdmin = !isAdmin;
  }
}
