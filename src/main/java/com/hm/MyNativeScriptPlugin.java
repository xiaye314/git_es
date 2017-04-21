package com.hm;

import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.AbstractLongSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MyNativeScriptPlugin extends Plugin implements ScriptPlugin {
    public List<NativeScriptFactory> getNativeScripts() {
        return Collections.singletonList(new MyNativeScriptFactory());
    }

    public static class MyNativeScriptFactory implements NativeScriptFactory {
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            return new MyNativeScript();
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        @Override
        public String getName() {
            return "hmtest";
        }
    }


    public static class MyNativeScript extends AbstractLongSearchScript {

        @Override
        public long runAsLong() {
            Object id = source().get("id");
            System.out.println(source().get("diseaseName"));
            return Long.valueOf(id.toString());
        }
    }
}