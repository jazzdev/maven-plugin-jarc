/**
 * Copyright (C) 2012-2013, Markus Sprunck - original tlocc plugin
 * Copyright (C) 2014, JD Brennan - Jarc compiler plugin
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - The name of its contributor may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Jarc Maven Plugin based on https://github.com/SPM2OT/maven-plugin-tlocc
 */

package jarc;

import java.io.*;
import java.util.*;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import jarc.*;

/**
 * Goal compile .arc files
 * 
 * @goal jaclyn
 * 
 * @phase compile
 */
public class MavenPlugin extends AbstractMojo
{
    /** List with all files to be compiled */
    private final List<String> files = new ArrayList<String>();

    /**
     * Location of the target directory.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private final File outputDirectory = new File("");

    /**
     * Project's source directory as specified in the POM.
     * 
     * @parameter expression="${project.build.sourceDirectory}"
     * @readonly
     * @required
     */
    private final File sourceDirectory = new File("");

    @Override
    public void execute() throws MojoExecutionException
    {

        if (!ensureTargetDirectoryExists()) {
            getLog().error("Could not create target directory");
            return;
        }

        if (!sourceDirectory.exists()) {
            getLog().error("Base directory \"" + sourceDirectory + "\" is not valid.");
            return;
        }

        // sourceDirectory is src/main/java - use parent to search src/main/webapp also
        fillListWithAllFilesRecursiveTask(sourceDirectory.getParentFile(), files);

        String libdir = sourceDirectory.getParentFile() + "/webapp/WEB-INF/lib";

        Env _env = new Env();
        _env.setThreadLocal(Symbol.intern("*in*"), Symbol.NIL);
        _env.setThreadLocal(Symbol.intern("*out*"), new JarcWriter(System.out));

        try
        {
            Symbol loadPathSymbol = Symbol.intern("load-path*");
            Cons loadPath = (Cons)_env.get(loadPathSymbol);
            loadPath = (Cons)_env.set(loadPathSymbol, Jarc.append(loadPath, libdir));
            for (final String filePath : files) {
                File file = new File(filePath);
                String filename = file.getName();
                String filedir = file.getParentFile().getPath();
                String targetPath = outputDirectory.getPath()
                    + "/" + filename.substring(0, filename.length()-4) + ".class";
                if (!loadPath.toList().contains(filedir))
                {
                    loadPath = (Cons)_env.set(loadPathSymbol, Jarc.append(loadPath, filedir));
                }
                Jarc.apply(Symbol.intern("add-lib-dir"), Jarc.list(libdir), _env);
                Jarc.apply(Symbol.intern("compile"), Jarc.list(filePath, targetPath), _env);
            }
        }
        catch (Exception ex)
        {
            getLog().error(ex.toString());
        }
    }

    private boolean ensureTargetDirectoryExists() {
        if (outputDirectory.exists()) {
            return true;
        }
        return outputDirectory.mkdirs();
    }

    public void fillListWithAllFilesRecursiveTask(final File root, final List<String> files) {
        if (root.isFile() && root.getName().endsWith(".arc")) {
            files.add(root.getPath());
            return;
        }
        File[] fileList = root.listFiles();
        if (fileList == null) {
            return;
        }
        for (final File file : fileList) {
            fillListWithAllFilesRecursiveTask(file, files);
        }
    }

}
