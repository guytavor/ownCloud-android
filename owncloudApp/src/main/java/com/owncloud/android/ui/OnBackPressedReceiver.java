package com.owncloud.android.ui;

/**
 * @author Guy Tavor (guy@scoompa.com)
 * @since Oct 11, 2019
 */
public interface OnBackPressedReceiver {
  /**
   * If you return true the back press will not be taken into account, otherwise the activity will act naturally
   *
   * @return true if your processing has priority if not false
   */
  boolean onBackPressedReceived();
}
