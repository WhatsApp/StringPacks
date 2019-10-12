/* Copyright (c) Facebook, Inc. and its affiliates. All rights reserved.
 *
 * This source code is licensed under the Apache 2.0 license found in
 * the LICENSE file in the root directory of this source tree.
 */

package com.whatsapp.stringpacks;

import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import androidx.annotation.RequiresApi;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParserException;

public class StringPackResources extends Resources {

  private final Resources baseResources;
  private final StringPacks stringPacks;

  public static StringPackResources wrap(Resources resources) {
    if (resources instanceof StringPackResources) {
      return (StringPackResources) resources;
    }

    return new StringPackResources(resources, StringPacks.getInstance());
  }

  StringPackResources(Resources res, StringPacks sp) {
    super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
    baseResources = res;
    stringPacks = sp;
  }

  Resources getBaseResources() {
    return baseResources;
  }

  // region Delegate to StringPacks

  @Override
  public CharSequence getText(int id) throws NotFoundException {
    // TODO: Support getText() in StringPacks.
    return stringPacks.getString(id);
  }

  @Override
  public CharSequence getText(int id, CharSequence def) {
    // TODO: Support getText() in StringPacks.
    String text = id != 0 ? stringPacks.getString(id) : null;
    return text != null ? text : def;
  }

  @Override
  public CharSequence getQuantityText(int id, int quantity) throws NotFoundException {
    // TODO: Support getQuantityText() in StringPacks.
    return stringPacks.getQuantityString(id, quantity);
  }

  @Override
  public String getString(int id) throws NotFoundException {
    return stringPacks.getString(id);
  }

  @Override
  public String getString(int id, Object... formatArgs) throws NotFoundException {
    return formatString(stringPacks.getString(id), formatArgs);
  }

  @Override
  public String getQuantityString(int id, int quantity) throws NotFoundException {
    return stringPacks.getQuantityString(id, quantity);
  }

  @Override
  public String getQuantityString(int id, int quantity, Object... formatArgs)
      throws NotFoundException {
    return formatString(stringPacks.getQuantityString(id, quantity), formatArgs);
  }

  // endregion

  private String formatString(String format, Object... formatArgs) {
    return String.format(
        StringPackUtils.getLocaleFromConfiguration(getConfiguration()), format, formatArgs);
  }

  // region Direct delegate to base resources.

  @Override
  public CharSequence[] getTextArray(int id) throws NotFoundException {
    return baseResources.getTextArray(id);
  }

  @Override
  public String[] getStringArray(int id) throws NotFoundException {
    return baseResources.getStringArray(id);
  }

  @Override
  public int[] getIntArray(int id) throws NotFoundException {
    return baseResources.getIntArray(id);
  }

  @Override
  public TypedArray obtainTypedArray(int id) throws NotFoundException {
    return baseResources.obtainTypedArray(id);
  }

  @Override
  public float getDimension(int id) throws NotFoundException {
    return baseResources.getDimension(id);
  }

  @Override
  public int getDimensionPixelOffset(int id) throws NotFoundException {
    return baseResources.getDimensionPixelOffset(id);
  }

  @Override
  public int getDimensionPixelSize(int id) throws NotFoundException {
    return baseResources.getDimensionPixelSize(id);
  }

  @Override
  public float getFraction(int id, int base, int pbase) {
    return baseResources.getFraction(id, base, pbase);
  }

  @Override
  public Drawable getDrawable(int id) throws NotFoundException {
    return baseResources.getDrawable(id);
  }

  @RequiresApi(21)
  @Override
  public Drawable getDrawable(int id, Theme theme) throws NotFoundException {
    return baseResources.getDrawable(id, theme);
  }

  @RequiresApi(15)
  @Override
  public Drawable getDrawableForDensity(int id, int density) throws NotFoundException {
    return baseResources.getDrawableForDensity(id, density);
  }

  @RequiresApi(21)
  @Override
  public Drawable getDrawableForDensity(int id, int density, Theme theme) {
    return baseResources.getDrawableForDensity(id, density, theme);
  }

  @Override
  public Movie getMovie(int id) throws NotFoundException {
    return baseResources.getMovie(id);
  }

  @Override
  public int getColor(int id) throws NotFoundException {
    return baseResources.getColor(id);
  }

  @Override
  public ColorStateList getColorStateList(int id) throws NotFoundException {
    return baseResources.getColorStateList(id);
  }

  @Override
  public boolean getBoolean(int id) throws NotFoundException {
    return baseResources.getBoolean(id);
  }

  @Override
  public int getInteger(int id) throws NotFoundException {
    return baseResources.getInteger(id);
  }

  @Override
  public XmlResourceParser getLayout(int id) throws NotFoundException {
    return baseResources.getLayout(id);
  }

  @Override
  public XmlResourceParser getAnimation(int id) throws NotFoundException {
    return baseResources.getAnimation(id);
  }

  @Override
  public XmlResourceParser getXml(int id) throws NotFoundException {
    return baseResources.getXml(id);
  }

  @Override
  public InputStream openRawResource(int id) throws NotFoundException {
    return baseResources.openRawResource(id);
  }

  @Override
  public InputStream openRawResource(int id, TypedValue value) throws NotFoundException {
    return baseResources.openRawResource(id, value);
  }

  @Override
  public AssetFileDescriptor openRawResourceFd(int id) throws NotFoundException {
    return baseResources.openRawResourceFd(id);
  }

  @Override
  public void getValue(int id, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
    baseResources.getValue(id, outValue, resolveRefs);
  }

  @RequiresApi(15)
  @Override
  public void getValueForDensity(int id, int density, TypedValue outValue, boolean resolveRefs)
      throws NotFoundException {
    baseResources.getValueForDensity(id, density, outValue, resolveRefs);
  }

  @Override
  public void getValue(String name, TypedValue outValue, boolean resolveRefs)
      throws NotFoundException {
    baseResources.getValue(name, outValue, resolveRefs);
  }

  @Override
  public TypedArray obtainAttributes(AttributeSet set, int[] attrs) {
    return baseResources.obtainAttributes(set, attrs);
  }

  @Override
  public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
    super.updateConfiguration(config, metrics);
    if (baseResources != null) { // called from super's constructor. So, need to check.
      baseResources.updateConfiguration(config, metrics);
    }
  }

  @Override
  public DisplayMetrics getDisplayMetrics() {
    return baseResources.getDisplayMetrics();
  }

  @Override
  public Configuration getConfiguration() {
    return baseResources.getConfiguration();
  }

  @Override
  public int getIdentifier(String name, String defType, String defPackage) {
    return baseResources.getIdentifier(name, defType, defPackage);
  }

  @Override
  public String getResourceName(int resid) throws NotFoundException {
    return baseResources.getResourceName(resid);
  }

  @Override
  public String getResourcePackageName(int resid) throws NotFoundException {
    return baseResources.getResourcePackageName(resid);
  }

  @Override
  public String getResourceTypeName(int resid) throws NotFoundException {
    return baseResources.getResourceTypeName(resid);
  }

  @Override
  public String getResourceEntryName(int resid) throws NotFoundException {
    return baseResources.getResourceEntryName(resid);
  }

  @Override
  public void parseBundleExtras(XmlResourceParser parser, Bundle outBundle)
      throws XmlPullParserException, IOException {
    baseResources.parseBundleExtras(parser, outBundle);
  }

  @Override
  public void parseBundleExtra(String tagName, AttributeSet attrs, Bundle outBundle)
      throws XmlPullParserException {
    baseResources.parseBundleExtra(tagName, attrs, outBundle);
  }

  // endregion
}
