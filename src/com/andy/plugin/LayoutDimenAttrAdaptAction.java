package com.andy.plugin;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LayoutDimenAttrAdaptAction extends AnAction {
    public LayoutDimenAttrAdaptAction() {
        // Set the menu item name.
        super("Replace Layout HardCode Dimen Attributes");
    }

    @Override
    public void update(AnActionEvent event) {
        super.update(event);
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(event.getDataContext());
        event.getPresentation().setVisible(FileUtils.isLayoutXmlFileOrDir(file));
    }

    @Override
    public void actionPerformed(final AnActionEvent event) {
        final Project project = getEventProject(event);
        VirtualFile file = event.getData(LangDataKeys.VIRTUAL_FILE);
        if (file == null) {
            return;
        }

        execute(project, file);
        systemReformat(event);
    }

    private void systemReformat(final AnActionEvent event) {
        event.getActionManager().getAction(IdeActions.ACTION_EDITOR_REFORMAT).actionPerformed(event);
    }

    private synchronized void execute(Project project, final VirtualFile file) {
        VirtualFile[] files = file.getChildren();
        if (files.length > 0) {
            for (VirtualFile _file : files) {
                if (FileUtils.isLayoutXmlFileOrDir(_file)) {
                    execute(project, _file);
                }
            }
        } else {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                return;
            }

            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (!(psiFile instanceof XmlFile)) {
                return;
            }
            XmlFile xmlFile = (XmlFile) psiFile;

            XmlDocument doc = xmlFile.getDocument();
            if (doc == null) {
                return;
            }
            XmlTag tag = doc.getRootTag();
            if (tag == null) {
                return;
            }
            StringBuilder mLayoutContent = new StringBuilder();
            beginCreateLayoutTag(mLayoutContent);
            checkXmlTag(tag, mLayoutContent, "");
            endCreateLayoutTag(mLayoutContent);
            System.out.println("TAG execute :" + mLayoutContent);
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                public void run() {
                    try {
                        OutputStream outputStream = file.getOutputStream(null);
                        outputStream.write(mLayoutContent.toString().getBytes());
                        outputStream.flush();
                        outputStream.close();
                    } catch (Exception ioException) {
                        ioException.printStackTrace();
                    }
                }
            });
        }
    }

    private void beginCreateLayoutTag(StringBuilder content) {
        content.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    }

    private void endCreateLayoutTag(StringBuilder content) {
        content.append("");
    }

    private void checkXmlTag(XmlTag tag, StringBuilder content, String append) {
        String tagName = tag.getName();
        if (!tagName.isEmpty()) {
            XmlTag[] subTags = tag.getSubTags();
            content.append("\n")
                    .append(append)
                    .append("<")
                    .append(tagName)
                    .append("\n");
            checkXmlAttr(tag, content, append + "\t");
            if (subTags.length == 0) {
                content.append("/>\n");
            } else {
                content.append(">\n");
                for (XmlTag t : subTags) {
                    checkXmlTag(t, content, append + "\t");
                }
                content.append("\n");
                content.append(append)
                        .append("</")
                        .append(tagName)
                        .append(">\n");
            }
        }
    }

    private void checkXmlAttr(XmlTag tag, StringBuilder content, String append) {
        XmlAttribute[] attributes = tag.getAttributes();
        int length = attributes.length;
        XmlAttribute attribute;
        for (int i = 0; i < length; i++) {
            attribute = attributes[i];
            content.append(append)
                    .append(attribute.getName())
                    .append("=")
                    .append(adaptDimenAttr(attribute.getName(), attribute.getValue()));
            if (i != length - 1) {
                content.append("\n");
            }
        }
    }

    private String adaptDimenAttr(String name, String value) {
        Matcher matcher = Pattern.compile("[1-9][0-9]*(dp|px|sp|dip)").matcher(value);
        boolean isDeminValue = matcher.find();
        if (!isDeminValue) {
            return "\"" + value + "\"";
        }

        String[] split = value.split("(dp|px|sp|dip)");

        boolean isFontSize = name.contains("textSize");
        StringBuilder builder = new StringBuilder();
        if (isFontSize) {
            builder.append("@dimen/sp_");
        } else {
            builder.append("@dimen/dp_");
        }
        String str = split[0];
        str = str.replace("-", "m_").replace(".", "_").replace("+", "");
        builder.append(str);
        value = builder.toString();
        return "\"" + value + "\"";
    }

    public static void main(String[] args) {
        LayoutDimenAttrAdaptAction action = new LayoutDimenAttrAdaptAction();
        System.out.println(action.adaptDimenAttr("layout_width", "-10.5px"));
        System.out.println(action.adaptDimenAttr("layout_width", "100.5dp"));
        System.out.println(action.adaptDimenAttr("layout_width", "px"));
        System.out.println(action.adaptDimenAttr("layout_width", "dp_20"));
        System.out.println(action.adaptDimenAttr("layout_height", "10px"));
        System.out.println(action.adaptDimenAttr("layout_height", "-100dp"));
        System.out.println(action.adaptDimenAttr("layout_height", "10dip"));
        System.out.println(action.adaptDimenAttr("layout_height", "-100.2dip"));
        System.out.println(action.adaptDimenAttr("textSize", "+10px"));
        System.out.println(action.adaptDimenAttr("textSize", "100sp"));
    }

}
