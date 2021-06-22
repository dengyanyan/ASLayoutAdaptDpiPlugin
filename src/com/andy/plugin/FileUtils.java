package com.andy.plugin;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;

public class FileUtils {

    public static boolean isLayoutXmlFileOrDir(@Nullable VirtualFile file) {
        if (file == null) {
            return false;
        }
        String path = file.getPath();
        boolean isLayoutDir = false;
        boolean isLayoutXml = false;
        if (file.isDirectory()) {
            isLayoutDir = path.endsWith("/layout");
        } else {
            isLayoutXml = path.endsWith(".xml");
        }

        return isLayoutDir || isLayoutXml;
    }
}
