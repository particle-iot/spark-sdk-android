package io.particle.android.sdk.utils;


import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.ArrayMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
public class Parcelables {

    public static boolean readBoolean(Parcel parcel) {
        return (parcel.readInt() != 0);
    }


    public static void writeBoolean(Parcel parcel, boolean value) {
        parcel.writeInt(value ? 1 : 0);
    }


    public static List<String> readStringList(Parcel parcel) {
        List<String> sourceList = new ArrayList<>();
        parcel.readStringList(sourceList);
        return sourceList;
    }


    public static Map<String, String> readStringMap(Parcel parcel) {
        Map<String, String> map = new ArrayMap<>();
        Bundle bundle = parcel.readBundle(Parcelables.class.getClassLoader());
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

    public static <T extends Parcelable> Map<String, T> readParcelableMap(Parcel parcel) {
        Map<String, T> map = new ArrayMap<>();
        Bundle bundle = parcel.readBundle(Parcelables.class.getClassLoader());
        for (String key : bundle.keySet()) {
            T parcelable = bundle.getParcelable(key);
            map.put(key, parcelable);
        }
        return map;
    }

    public static <T extends Parcelable> void writeParcelableMap(Parcel parcel, Map<String, T> map) {
        Bundle b = new Bundle();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            b.putParcelable(entry.getKey(), entry.getValue());
        }
        parcel.writeBundle(b);
    }

    public static <T extends Serializable> Map<String, T> readSerializableMap(Parcel parcel) {
        Map<String, T> map = new ArrayMap<>();
        Bundle bundle = parcel.readBundle(Parcelables.class.getClassLoader());
        for (String key : bundle.keySet()) {
            @SuppressWarnings("unchecked")
            T serializable = (T) bundle.getSerializable(key);
            map.put(key, serializable);
        }
        return map;
    }

    public static <T extends Serializable> void writeSerializableMap(Parcel parcel, Map<String, T> map) {
        Bundle b = new Bundle();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            b.putSerializable(entry.getKey(), entry.getValue());
        }
        parcel.writeBundle(b);
    }

}
