package dev.jilaton.client.module.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rewrites components for local display only; packets and server state are never touched. */
public final class NickHiderPolicy {
    private NickHiderPolicy() {
    }

    public static String replace(String text, String realName, String replacement) {
        if (text == null || text.isEmpty() || realName == null || realName.isBlank()
                || replacement == null || realName.equals(replacement) || !text.contains(realName)) {
            return text;
        }
        if (text.equals(realName)) {
            return replacement;
        }
        Pattern boundary = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(realName) + "(?![A-Za-z0-9_])");
        return boundary.matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
    }

    public static Component replace(Component source, String realName, String replacement) {
        if (source == null || realName == null || realName.isBlank()
                || replacement == null || realName.equals(replacement)) {
            return source;
        }
        ComponentContents contents = source.getContents();
        ComponentContents rewritten = contents;
        if (contents instanceof PlainTextContents plain) {
            rewritten = PlainTextContents.create(replace(plain.text(), realName, replacement));
        } else if (contents instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] replacedArgs = new Object[args.length];
            for (int index = 0; index < args.length; index++) {
                replacedArgs[index] = args[index] instanceof Component component
                        ? replace(component, realName, replacement) : args[index];
            }
            rewritten = new TranslatableContents(translatable.getKey(), translatable.getFallback(), replacedArgs);
        }
        MutableComponent result = MutableComponent.create(rewritten).setStyle(source.getStyle());
        for (Component sibling : source.getSiblings()) {
            result.append(replace(sibling, realName, replacement));
        }
        return result;
    }
}
