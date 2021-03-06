/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import net.sf.saxon.TransformerFactoryImpl;
import radl.common.io.IO;
import radl.common.xml.Xml;
import radl.core.Log;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.cli.Cli;
import radl.core.xml.RadlFileAssembler;

/**
 * Generate documentation from a RADL file.
 */
public final class DocumentationGenerator implements Application {

  private static final String CLIENT_DOCUMENTATION_STYLESHEET = "/xslt/radl2html.xsl";
  private static final String CLIENT_DOCUMENTATION_DEFAULT_CSS = "/xslt/radl-default.css";
  private static final String CLIENT_DOCUMENTATION_FILE = "index.html";
  public static final String HIDE_LOCATION = "hide-location";

  public static void main(String[] args) {
    Cli.run(DocumentationGenerator.class, args);
  }

  /**
   * The following arguments are supported.<ul>
   * <li>[Required] Directory in which to generate the documentation</li>
   * <li>[Optional] The name of a configuration file</li>
   * <li>[Optional] The name of a css file</li>
   * <li>[Optional] The text 'hide-location' to indicate hide resource location in documentation</li>
   * <li>[Required] The names of the RADL files to generate documentation for</li>
   * </ul>
   */
  @Override
  public int run(Arguments arguments) {
    File docDir = parseDocDir(arguments);
    File configuration = parseConfigurationFile(arguments);
    String cssSource = parseCssFile(arguments);
    boolean hideLocation = parseHideLocation(arguments);
    return iterativelyHandleRadlFiles(arguments, docDir, configuration, cssSource, hideLocation) ? -1 : 0;
  }

  private File parseDocDir(Arguments arguments) {
    File docDir = arguments.file();
    docDir.mkdirs();
    return docDir;
  }

  private File parseConfigurationFile(Arguments arguments) {
    return getNextArgument(arguments, File.class, ".properties");
  }

  private String parseCssFile(Arguments arguments) {
    return getNextArgument(arguments, String.class, ".css");
  }

  private boolean parseHideLocation(Arguments arguments) {
    String hideLocation = getNextArgument(arguments, String.class, HIDE_LOCATION);
    return HIDE_LOCATION.equals(hideLocation);
  }

  private boolean iterativelyHandleRadlFiles(Arguments arguments, File docDir, File configuration, String cssSource, boolean hideLocation) {
    File radlFile = getNextArgument(arguments, File.class, ".radl", ".xml");
    if (radlFile == null) {
      Log.error("Missing RADL files");
      return true;
    }
    while (radlFile != null) {
      Log.info("-> Generating client documentation for " + radlFile.getName());
      try {
        generateClientDocumentation(radlFile, docDir, configuration, cssSource, hideLocation);
      } catch (RuntimeException e) {
        throw new RuntimeException("Error generating documentation for " + radlFile.getName(), e);
      }
      radlFile = getNextArgument(arguments, File.class, ".radl", ".xml");
    }
    return false;
  }

  private <T> T getNextArgument(Arguments arguments, Class<T> type, String... suffix) {
    if (!arguments.hasNext()) {
      return null;
    }
    String result = arguments.next();
    if (StringUtils.isNotEmpty(result) && !endsWith(result, suffix)) {
      arguments.prev();
      return null;
    }
    return type.cast(File.class.isAssignableFrom(type) ? new File(result) : result);
  }

  private boolean endsWith(String s, String[] suffix) {
    for (String suf : suffix) {
      if (s.endsWith(suf)) {
        return true;
      }
    }
    return false;
  }

  private void generateClientDocumentation(File radlFile, File docDir, File configuration, String cssSource, boolean hideLocation) {
    File serviceDir = getServiceDir(radlFile, docDir);
    File assembledRadl = RadlFileAssembler.assemble(radlFile, docDir);
    try {
      Document radlDocument = Xml.parse(assembledRadl);
      new StateDiagramGenerator().generateFrom(radlDocument, serviceDir, configuration);
      File localCssFile = normalizeCSSFile(docDir, cssSource);
      try {
        generateClientDocumentation(radlDocument, getIndexFile(serviceDir), localCssFile.toURI().toString(), hideLocation);
      } finally {
        IO.delete(localCssFile);
      }
    } finally {
      IO.delete(assembledRadl);
    }
  }

  private File getServiceDir(File radl, File docDir) {
    File result = new File(docDir, radl.getName().substring(0, radl.getName().lastIndexOf('.')));
    result.mkdirs();
    return result;
  }

  private File getIndexFile(File serviceDir) {
    return new File(serviceDir, CLIENT_DOCUMENTATION_FILE);
  }

  private void generateClientDocumentation(Document radl, File destination, String cssFile, boolean hideLocation) {
    try (InputStream stylesheet = getClass().getResourceAsStream(CLIENT_DOCUMENTATION_STYLESHEET)) {
      if (stylesheet == null) {
        throw new IllegalStateException("Missing stylesheet: " + CLIENT_DOCUMENTATION_STYLESHEET);
      }
      generateClientDocumentation(radl, stylesheet, cssFile, hideLocation, destination);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private File normalizeCSSFile(File docDir, String cssSource) {
    if (StringUtils.isNotBlank(cssSource)) {
      Log.info("Provided CSS URL is: " + cssSource);
    }
    try (InputStream input = StringUtils.isEmpty(cssSource) ?
          getClass().getResourceAsStream(CLIENT_DOCUMENTATION_DEFAULT_CSS) :
          new URL(cssSource).openStream()) {
      File localCss = new File(docDir, "radl-use.css");
      try (OutputStream output = new FileOutputStream(localCss)) {
        IO.copy(input, output);
      }
      return localCss;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void generateClientDocumentation(Document radl, InputStream stylesheet, String cssFile, boolean hideLocation, File destination)
      throws Exception {
    Transformer transformer = newTransformerFactory().newTransformer(new StreamSource(stylesheet));
    transformer.setParameter("dir", destination);
    transformer.setParameter("css-file", cssFile);
    transformer.setParameter(HIDE_LOCATION, hideLocation);
    try (OutputStream output = new FileOutputStream(destination)) {
      transformer.transform(new DOMSource(radl), new StreamResult(output));
    }
  }

  private TransformerFactory newTransformerFactory() {
    return new TransformerFactoryImpl();
  }

}
