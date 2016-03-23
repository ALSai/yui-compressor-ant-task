package com.simonbuckle.ant.tasks;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import com.yahoo.platform.yui.compressor.CssCompressor;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

public class CompressTask extends Task
{

    private List<FileSet> filesets = new ArrayList<FileSet>();
    private Mapper mapper;
    private int linebreak = -1;
    private boolean munge = true;
    private boolean preserveAllSemiColons = false;
    private boolean disableOptimizations = false;
    private boolean preserveUnknownHints = false;
    private boolean verbose = false;
    private String todir;
    private String encoding = "UTF-8";
    private String type;

    public void addFileset(FileSet fileset)
    {
        filesets.add(fileset);
    }

    public void addMapper(Mapper mapper)
    {
        this.mapper = mapper;
    }

    public void setPreserveUnknownHints(boolean preserveUnknownHints)
    {
        this.preserveUnknownHints = preserveUnknownHints;
    }
    
    public void setDisableOptimizations(boolean disableOptimizations)
    {
        this.disableOptimizations = disableOptimizations;
    }

    public void setLinebreak(int linebreak)
    {
        this.linebreak = linebreak;
    }

    public void setMunge(boolean munge)
    {
        this.munge = munge;
    }

    public void setPreserveAllSemiColons(boolean preserveAllSemiColons)
    {
        this.preserveAllSemiColons = preserveAllSemiColons;
    }

    public void setToDir(String todir)
    {
        this.todir = todir;
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    private void validateRequired() throws BuildException
    {
        StringBuilder errorString = new StringBuilder();

        if (mapper == null)
            errorString.append("Mapper property is required\n");
        if (todir == null || "".equals(todir))
            errorString.append("Output directory is not specified\n");
        if (type == null || "".equals(type))
            errorString.append("type is not specified\n");

        if (errorString.length() > 0)
        {
            throw new BuildException(errorString.toString());
        }
    }

    public void execute() throws BuildException
    {
        validateRequired();

        Iterator<FileSet> iter = filesets.listIterator();
        while (iter.hasNext())
        {
            FileSet fileset = iter.next();
            DirectoryScanner scanner = fileset.getDirectoryScanner(getProject());
            File dir = scanner.getBasedir();

            String[] files = scanner.getIncludedFiles();
            for (int i = 0; i < files.length; i++)
            {
                String[] output = mapper.getImplementation().mapFileName(files[i]);
                if (output != null)
                {
                    try
                    {
                        compress(new File(dir, files[i]), new File(todir, output[0]));
                    }
                    catch (IOException io)
                    {
                        log("Failed to compress file: " + files[i] + " : " + io.getMessage());
                    }
                }
            }
        }
    }

    private void compress(File source, File dest) throws IOException
    {
        Reader in = null;
        Writer out = null;
        try
        {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(source), encoding));
            log("Compressing: " + source.getName());

            if (type.equalsIgnoreCase("js"))
            {

                try
                {
                    JavaScriptCompressor compressor = new JavaScriptCompressor(in, new ErrorReporter()
                    {

                        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset)
                        {
                            if (line < 0)
                            {
                                System.err.println("\n[WARNING] " + message);
                            }
                            else
                            {
                                System.err.println("\n[WARNING] " + line + ':' + lineOffset + ':' + message);
                            }
                        }

                        public void error(String message, String sourceName, int line, String lineSource, int lineOffset)
                        {
                            if (line < 0)
                            {
                                System.err.println("\n[ERROR] " + message);
                            }
                            else
                            {
                                System.err.println("\n[ERROR] " + line + ':' + lineOffset + ':' + message);
                            }
                        }

                        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset)
                        {
                            error(message, sourceName, line, lineSource, lineOffset);
                            return new EvaluatorException(message);
                        }
                    });

                    // Close the input stream first, and then open the output
                    // stream,
                    // in case the output file should override the input file.
                    in.close();
                    in = null;

                    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), encoding));

                    compressor.compress(out, null, linebreak, munge, verbose, preserveAllSemiColons, disableOptimizations, preserveUnknownHints);
                }
                catch (EvaluatorException e)
                {
                    e.printStackTrace();
                    // Return a special error code used specifically by the web
                    // front-end.
                    System.exit(2);
                }

            }
            else if (type.equalsIgnoreCase("css"))
            {
                CssCompressor compressor = new CssCompressor(in);

                // Close the input stream first, and then open the output
                // stream,
                // in case the output file should override the input file.
                in.close();
                in = null;

                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), encoding));

                compressor.compress(out, linebreak);
            }

        }
        finally
        {

            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

        }
    }
}
