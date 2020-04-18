package com.leobeliik.extremesoundmuffler.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.minecraft.util.ResourceLocation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

class JsonIO {

    static void save(File file, Set<ResourceLocation> list) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(new Gson().toJson(list));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Set<ResourceLocation> load(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(new JsonReader(reader), new TypeToken<Set<ResourceLocation>>() {}.getType());
        } catch (IOException e) {
            return new HashSet<>();
        }
    }
}