/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.j2me;

import javax.microedition.amms.control.camera.ExposureControl;
import javax.microedition.amms.control.camera.FocusControl;
import javax.microedition.amms.control.camera.ZoomControl;
import javax.microedition.media.Controllable;
import javax.microedition.media.MediaException;

/**
 * <p>This odd class encapsulates all access to functionality exposed by JSR-234,
 * which provides access to things like focus and zoom. Not all phones support this though.
 * Normally we might handle loading of code like this via reflection but this is
 * not available to us in Java ME. So, we create two implementations of the same class --
 * this one, and another found under source root "src-basic". This one actually calls
 * JSR-234 methods. The other does nothing. The build script creates two build products then
 * one compiled with this class and one with other, to create both the JSR-234 version
 * and the "basic" non-JSR-234 version.</p>
 *
 * @author Sean Owen (srowen@google.com)
 */
public final class AdvancedMultimediaManager {

  private static final int NO_ZOOM = 100;
  private static final int MAX_ZOOM = 200;
  private static final long FOCUS_TIME_MS = 750L;
  private static final String DESIRED_METERING = "center-weighted";

  private AdvancedMultimediaManager() {
    // do nothing
  }

  static void setFocus(Controllable player) {
    FocusControl focusControl = (FocusControl)
        player.getControl("javax.microedition.amms.control.camera.FocusControl");
    if (focusControl != null) {
      try {
        if (focusControl.isMacroSupported() && !focusControl.getMacro()) {
          focusControl.setMacro(true);
        }
        if (focusControl.isAutoFocusSupported()) {
          focusControl.setFocus(FocusControl.AUTO);
          try {
            Thread.sleep(FOCUS_TIME_MS); // let it focus...
          } catch (InterruptedException ie) {
            // continue
          }
          focusControl.setFocus(FocusControl.AUTO_LOCK);
        }
      } catch (MediaException me) {
        // continue
      }
    }
  }

 public static void setZoom(Controllable player) {
    ZoomControl zoomControl = (ZoomControl) player.getControl("javax.microedition.amms.control.camera.ZoomControl");
    if (zoomControl != null) {
      // We zoom in if possible to encourage the viewer to take a snapshot from a greater distance.
      // This is a crude way of dealing with the fact that many phone cameras will not focus at a
      // very close range.
      int maxZoom = zoomControl.getMaxOpticalZoom();
      if (maxZoom > NO_ZOOM) {
        zoomControl.setOpticalZoom(maxZoom > MAX_ZOOM ? MAX_ZOOM : maxZoom);
      } else {
        int maxDigitalZoom = zoomControl.getMaxDigitalZoom();
        if (maxDigitalZoom > NO_ZOOM) {
          zoomControl.setDigitalZoom(maxDigitalZoom > MAX_ZOOM ? MAX_ZOOM : maxDigitalZoom);
        }
      }
    }
  }

 public static void setExposure(Controllable player) {
    ExposureControl exposureControl =
        (ExposureControl) player.getControl("javax.microedition.amms.control.camera.ExposureControl");
    if (exposureControl != null) {

      int[] supportedISOs = exposureControl.getSupportedISOs();
      if (supportedISOs != null && supportedISOs.length > 0) {
        int maxISO = Integer.MIN_VALUE;
        for (int i = 0; i < supportedISOs.length; i++) {
          if (supportedISOs[i] > maxISO) {
            maxISO = supportedISOs[i];
          }
        }
        try {
          exposureControl.setISO(maxISO);
        } catch (MediaException me) {
          // continue
        }
      }

      String[] supportedMeterings = exposureControl.getSupportedLightMeterings();
      if (supportedMeterings != null) {
        for (int i = 0; i < supportedMeterings.length; i++) {
          if (DESIRED_METERING.equals(supportedMeterings[i])) {
            exposureControl.setLightMetering(DESIRED_METERING);
            break;
          }
        }
      }

    }
  }

}