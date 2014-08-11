/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer;

import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.StringEscapeUtils;

import com.hippo.ehviewer.ehclient.EhClient;

public class ListUrls {
    
    public static final boolean NUM_CATEGORY = true;
    
    public static final int NORMAL = 0x0;
    public static final int UPLOADER = 0x1;
    public static final int TAG = 0x2;
    public static final int POPULAR = 0x3;
    
    // Search type
    public static final int MISC = 0x1;
    public static final int DOUJINSHI = 0x2;
    public static final int MANGA = 0x4;
    public static final int ARTIST_CG = 0x8;
    public static final int GAME_CG = 0x10;
    public static final int IMAGE_SET = 0x20;
    public static final int COSPLAY = 0x40;
    public static final int ASIAN_PORN = 0x80;
    public static final int NON_H = 0x100;
    public static final int WESTERN = 0x200;
    public static final int UNKNOWN = 0x400;
    
    public static final int ALL_TYPE = UNKNOWN - 1;
    //DOUJINSHI|MANGA|ARTIST_CG|GAME_CG|WESTERN|NON_H|IMAGE_SET|COSPLAY|ASIAN_PORN|MISC;
    
    // advance search type
    public static final int SNAME = 0x1;
    public static final int STAGS = 0x2;
    public static final int SDESC = 0x4;
    public static final int STORR = 0x8;
    public static final int STO = 0x10;
    public static final int STD1 = 0x20;
    public static final int STD2 = 0x40;
    public static final int SH = 0x80;
    
    public static final int DEFAULT_ADVANCE = SNAME | STAGS;
    public static final int DEFAULT_MIN_RATING = 2;
    
    private int mMode = NORMAL;
    
    private int page = 0;
    private int type = ALL_TYPE;
    
    private int advsearchType = -1;
    private int minRating = -1;
    
    private String search = null;
    
    private String mTag = null;
    
    public ListUrls() {}
    
    public ListUrls(int t) {
        type = t;
        page = 0;
    }
    
    public ListUrls(int t, int p) {
        type = t;
        page = p;
    }
    
    public ListUrls(int t, String s) {
        type = t;
        search = s;
    }
    
    public ListUrls(int t, String s, int p) {
        type = t;
        search = s;
        page = p;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public int getType() {
        return type;
    }
    
    public String getSearch() {
        return search;
    }
    
    public void setAdvance(boolean enabled) {
        if (enabled && advsearchType == -1)
            advsearchType = DEFAULT_ADVANCE;
        else if (!enabled)
            advsearchType = -1;
    }
    
    public void setAdvance(int advanceType) {
        advsearchType = advanceType;
    }
    
    public void setAdvance(int advanceType, int minRating) {
        advsearchType = advanceType;
        this.minRating = minRating;
    }
    
    public boolean isAdvance() {
        return advsearchType != -1;
    }
    
    public int getAdvanceType() {
        return advsearchType;
    }
    
    public boolean isMinRating() {
        return minRating != -1;
    }
    
    public int getMinRating() {
        return minRating;
    }
    
    public void setPage(int p) {
        page = p;
    }
    
    public int getPage() {
        return page;
    }
    
    public int getMode() {
        return mMode;
    }
    
    public void setMode(int mode) {
        mMode = mode;
    }
    
    public void setTag(String tag) {
        mMode = TAG;
        mTag = tag;
    }
    
    public String getTag() {
        return mTag;
    }
    
    public String getUrl() {
        StringBuilder url = new StringBuilder(EhClient.getUrlHeader());
        
        if (mMode == TAG) {
            // Add tag
            url.append("tag/");
            if (mTag != null) {
                String[] searchItems = mTag.split("\\s+");
                boolean firstItem = true;
                for (String searcheItem : searchItems) {
                    try {
                        searcheItem = java.net.URLEncoder.encode(searcheItem, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (firstItem)
                        firstItem = false;
                    else
                        url.append('+');
                    url.append(searcheItem);
                }
                
                // Add page
                url.append("/").append(page);
            } else
                url.append(page);
        } else if (mMode == POPULAR) {
            
        } else {
            
            // Add type
            if (NUM_CATEGORY) {
                url.append(type);
                url.append("?");
            } else {
                url.append("?");
                if((type & DOUJINSHI) == 0)
                    url.append("f_doujinshi=0&");
                else
                    url.append("f_doujinshi=1&");
                if((type & MANGA) == 0)
                    url.append("f_manga=0&");
                else
                    url.append("f_manga=1&");
                if((type & ARTIST_CG) == 0)
                    url.append("f_artistcg=0&");
                else
                    url.append("f_artistcg=1&");
                if((type & GAME_CG) == 0)
                    url.append("f_gamecg=0&");
                else
                    url.append("f_gamecg=1&");
                if((type & WESTERN) == 0)
                    url.append("f_western=0&");
                else
                    url.append("f_western=1&");
                if((type & NON_H) == 0)
                    url.append("f_non-h=0&");
                else
                    url.append("f_non-h=1&");
                if((type & IMAGE_SET) == 0)
                    url.append("f_imageset=0&");
                else
                    url.append("f_imageset=1&");
                if((type & COSPLAY) == 0)
                    url.append("f_cosplay=0&");
                else
                    url.append("f_cosplay=1&");
                if((type & ASIAN_PORN) == 0)
                    url.append("f_asianporn=0&");
                else
                    url.append("f_asianporn=1&");
                if((type & MISC) == 0)
                    url.append("f_misc=0&");
                else
                    url.append("f_misc=1&");
            }
            
            // Add search
            if (search != null) {
                url.append("f_search=");
                String[] searchItems = search.split("\\s+");
                boolean firstItem = true;
                for (String searcheItem : searchItems) {
                    try {
                        searcheItem = java.net.URLEncoder.encode(searcheItem, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (firstItem)
                        firstItem = false;
                    else
                        url.append('+');
                    url.append(searcheItem);
                }
                url.append("&");
            }
            
            // Add page
            url.append("page=").append(page).append("&");
            
            // Add foot
            url.append("f_apply=Apply+Filter");
            
            // Is advance search
            if (isAdvance()) {
                url.append("&advsearch=1");
                if((advsearchType & SNAME) != 0)
                    url.append("&f_sname=on");
                if((advsearchType & STAGS) != 0)
                    url.append("&f_stags=on");
                if((advsearchType & SDESC) != 0)
                    url.append("&f_sdesc=on");
                if((advsearchType & STORR) != 0)
                    url.append("&f_storr=on");
                if((advsearchType & STO) != 0)
                    url.append("&f_sto=on");
                if((advsearchType & STD1) != 0)
                    url.append("&f_sdt1=on");
                if((advsearchType & STD2) != 0)
                    url.append("&f_sdt2=on");
                if((advsearchType & SH) != 0)
                    url.append("&f_sh=on");
                
                // Set min star
                if (isMinRating())
                    url.append("&f_sr=on&f_srdd=").append(minRating);
            }
        }
        
        return url.toString();
    }
}