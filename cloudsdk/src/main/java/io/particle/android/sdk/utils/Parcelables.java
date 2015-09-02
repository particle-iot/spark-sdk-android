package io.particle.android.sdk.utils;


import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.util.ArrayMap;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

public class Parcelables {

    public static boolean readBoolean(Parcel parcel) {
        return (parcel.readInt() != 0);
    }


    public static void writeBoolean(Parcel parcel, boolean value) {
        parcel.writeInt(value ? 1 : 0);
    }


    public static List<String> readStringList(Parcel parcel) {
        List<String> sourceList = Lists.newArrayList();
        parcel.readStringList(sourceList);
        return sourceList;
    }


    public static Map<String, String> readStringMap(Parcel parcel) {
        Map<String, String> map = new ArrayMap<>();
        Bundle bundle = parcel.readBundle();
        for (String key : bundle.keySet()) {
            map.put(key, bundle.getString(key));
        }
        return map;
    }


    public static void writeStringMap(Parcel parcel, Map<String, String> stringMap) {
        Bundle b = new Bundle();
        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            b.putString(entry.getKey(), entry.getValue());
        }
        parcel.writeBundle(b);
    }


}
