package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class IdeHelper {

    public static void openUrl(String url) {
        if(java.awt.Desktop.isDesktopSupported() ) {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

            if(desktop.isSupported(java.awt.Desktop.Action.BROWSE) ) {
                try {
                    java.net.URI uri = new java.net.URI(url);
                    desktop.browse(uri);
                } catch (URISyntaxException ignored) {
                } catch (IOException ignored) {

                }
            }
        }
    }


    @Nullable
    public static VirtualFile createFile(@Nullable VirtualFile root, String fileNameWithPath) {

        if(root == null) {
            return null;
        }

        String path = fileNameWithPath.substring(0, fileNameWithPath.lastIndexOf("/"));

        try {
            VfsUtil.createDirectoryIfMissing(root, path);
        } catch (IOException e) {
            return null;
        }

        VirtualFile twigDirectory = VfsUtil.findRelativeFile(root, path.split("/"));
        if(twigDirectory == null || !twigDirectory.exists()) {
            return null;
        }

        File f = new File(twigDirectory.getCanonicalPath() + "/" + fileNameWithPath.substring(fileNameWithPath.lastIndexOf("/") + 1));
        if(!f.exists()){
            try {
                if(!f.createNewFile()) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }

        VirtualFile virtualFile = VfsUtil.findFileByIoFile(f, true);
        if(virtualFile == null) {
            return null;
        }

        return virtualFile;
    }

    public static RunnableCreateAndOpenFile getRunnableCreateAndOpenFile(Project project, VirtualFile rootVirtualFile, String fileName) {
        return new RunnableCreateAndOpenFile(project, rootVirtualFile, fileName);
    }

    public static class RunnableCreateAndOpenFile implements Runnable {

        VirtualFile rootVirtualFile;
        String fileName;
        Project project;

        RunnableCreateAndOpenFile(Project project, VirtualFile rootVirtualFile, String fileName) {
            this.project = project;
            this.rootVirtualFile = rootVirtualFile;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            VirtualFile virtualFile = createFile(rootVirtualFile, fileName);
            if(virtualFile != null) {
                new OpenFileDescriptor(project, virtualFile, 0).navigate(true);
            }
        }
    }

}
