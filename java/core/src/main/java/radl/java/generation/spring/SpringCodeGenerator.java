/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.core.code.MediaType;
import radl.core.code.Property;
import radl.core.code.PropertyGroup;
import radl.core.code.PropertyGroups;
import radl.core.code.RadlCode;
import radl.core.code.RadlCode.ResourceMethod;
import radl.core.generation.CodeGenerator;
import radl.java.code.Java;
import radl.java.code.JavaCode;


/**
 * Generates Java code for the Spring framework from a RADL document.
 */
public class SpringCodeGenerator implements CodeGenerator {

  private static final String DTO_SUFFIX = "Dto";
  private static final String UNKNOWN_TYPE = "Object";
  private static final String NO_TYPE = "void";
  private static final String DEFAULT_HEADER = "Generated from RADL.";
  private static final String IMPL_PACKAGE = "impl";
  private static final String API_PACKAGE = "api";
  private static final String BILLBOARD_URL = "BILLBOARD";
  private static final String CONSTANT_PREFIX_URL = "URL_";
  private static final String DEFAULT_MEDIA_TYPE = "application/";
  private static final String MEDIA_TYPE_CONSTANT_PREFIX = "MEDIA_TYPE_";
  private static final String DEFAULT_MEDIA_TYPE_CONSTANT = MEDIA_TYPE_CONSTANT_PREFIX + "DEFAULT";
  private static final String API_TYPE = "Api";
  private static final String URIS_TYPE = "Uris";
  private static final String DEFAULT_STATUS_CODE = "400";
  private static final String ERROR_DTO_TYPE = "ErrorDto";
  private static final String IDENTIFIABLE_TYPE = "Identifiable";
  private static final Map<String, String> HTTP_STATUSES = new HashMap<String, String>();
  private static final Collection<String> FRAMEWORK_HANDLED_STATUSES = Arrays.asList("405", "406");
  private static final String SEMANTIC_ANNOTATION_PACKAGE = "de.escalon.hypermedia.hydra.mapping";
  private static final String SEMANTIC_ANNOTATION = "Expose";
  private static final String CONTROLLER_HELPER_NAME = "helper";

  private final String packagePrefix;
  private final Map<String, Constant> errorConstants = new TreeMap<String, Constant>();
  private final Map<String, Constant> linkRelationConstants = new TreeMap<String, Constant>();
  private final Map<String, Constant> mediaTypeConstants = new TreeMap<String, Constant>();
  private final Map<String, Constant> uriConstants = new TreeMap<String, Constant>();
  private final String header;
  private MediaType defaultMediaType;

  public SpringCodeGenerator(String packagePrefix) {
    this(packagePrefix, null);
  }

  public SpringCodeGenerator(String packagePrefix, String header) {
    this.packagePrefix = packagePrefix;
    this.header = header == null || header.trim().isEmpty() ? DEFAULT_HEADER : header;
    initHttpStatuses();
  }

  private void initHttpStatuses() {
    HTTP_STATUSES.put("400", "BAD_REQUEST");
    HTTP_STATUSES.put("401", "UNAUTHORIZED");
    HTTP_STATUSES.put("402", "PAYMENT_REQUIRED");
    HTTP_STATUSES.put("403", "FORBIDDEN");
    HTTP_STATUSES.put("404", "NOT_FOUND");
    HTTP_STATUSES.put("405", "METHOD_NOT_ALLOWED");
    HTTP_STATUSES.put("406", "NOT_ACCEPTABLE");
    HTTP_STATUSES.put("407", "PROXY_AUTHENTICATION_REQUIRED");
    HTTP_STATUSES.put("408", "REQUEST_TIMEOUT");
    HTTP_STATUSES.put("409", "CONFLICT");
    HTTP_STATUSES.put("410", "GONE");
    HTTP_STATUSES.put("411", "LENGTH_REQUIRED");
    HTTP_STATUSES.put("412", "PRECONDITION_FAILED");
    HTTP_STATUSES.put("413", "PAYLOAD_TOO_LARGE");
    HTTP_STATUSES.put("414", "URI_TOO_LONG");
    HTTP_STATUSES.put("415", "UNSUPPORTED_MEDIA_TYPE");
    HTTP_STATUSES.put("416", "REQUESTED_RANGE_NOT_SATISFIABLE");
    HTTP_STATUSES.put("417", "EXPECTATION_FAILED");
    HTTP_STATUSES.put("422", "UNPROCESSABLE_ENTITY");
    HTTP_STATUSES.put("423", "LOCKED");
    HTTP_STATUSES.put("424", "FAILED_DEPENDENCY");
    HTTP_STATUSES.put("426", "UPGRADE_REQUIRED");
    HTTP_STATUSES.put("428", "PRECONDITION_REQUIRED");
    HTTP_STATUSES.put("429", "TOO_MANY_REQUESTS");
    HTTP_STATUSES.put("431", "REQUEST_HEADER_FIELDS_TOO_LARGE");
    HTTP_STATUSES.put("500", "INTERNAL_SERVER_ERROR");
    HTTP_STATUSES.put("501", "NOT_IMPLEMENTED");
    HTTP_STATUSES.put("502", "BAD_GATEWAY");
    HTTP_STATUSES.put("503", "SERVICE_UNAVAILABLE");
    HTTP_STATUSES.put("504", "GATEWAY_TIMEOUT");
    HTTP_STATUSES.put("505", "HTTP_VERSION_NOT_SUPPORTED");
    HTTP_STATUSES.put("506", "VARIANT_ALSO_NEGOTIATES");
    HTTP_STATUSES.put("507", "INSUFFICIENT_STORAGE");
    HTTP_STATUSES.put("508", "LOOP_DETECTED");
    HTTP_STATUSES.put("509", "BANDWIDTH_LIMIT_EXCEEDED");
    HTTP_STATUSES.put("510", "NOT_EXTENDED");
    HTTP_STATUSES.put("511", "NETWORK_AUTHENTICATION_REQUIRED");
  }

  @Override
  public Iterable<Code> generateFrom(Document radl) {
    Collection<Code> result = new ArrayList<Code>();
    try {
      generate(new RadlCode(radl), result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private void generate(RadlCode radl, Collection<Code> result) throws Exception {
    defaultMediaType = radl.defaultMediaType();
    boolean hasHyperMediaTypes = radl.hasHyperMediaTypes();
    addLinkRelationConstants(radl);
    generateSourcesForPropertyGroups(radl.propertyGroups(), hasHyperMediaTypes, result);
    generateSourcesForResources(radl, hasHyperMediaTypes, result);
    generateSourcesForErrors(radl, result);
  }

  private void generateSourcesForPropertyGroups(PropertyGroups propertyGroups, final boolean hasHyperMediaTypes,
      final Collection<Code> sources) throws Exception {
    if (propertyGroups == null) {
      return;
    }
    for (String propertyGroup : propertyGroups.names()) {
      addDtosFor(propertyGroups.item(propertyGroup), hasHyperMediaTypes, sources);
    }
  }

  protected String addDtosFor(PropertyGroup propertyGroup, boolean hasHyperMediaTypes, Collection<Code> sources)
      throws Exception {
    final JavaCode code = new JavaCode();
    addPackage(propertyGroup.name(), code);
    code.add("");
    String superType;
    if (hasHyperMediaTypes) {
      code.add("import org.springframework.hateoas.ResourceSupport;");
      code.add("");
      superType = "extends ResourceSupport ";
    } else {
      superType = "";
    }
    addSemanticAnnotationImport(propertyGroup, code);
    addDtoImports(propertyGroup, code);
    code.add("");
    addSemanticAnnotation(propertyGroup, "", code);
    String result = getDtoClass(propertyGroup.name());
    code.add("public class %s %s{", result, superType);
    code.add("");
    addDtoFields(hasHyperMediaTypes, propertyGroup, code, sources);
    code.add("");
    code.add("}");
    sources.add(code);
    return result;
  }

  private String getDtoClass(String name) {
    return Java.toIdentifier(name) + DTO_SUFFIX;
  }

  private void addDtoImports(PropertyGroups propertyGroup, final Code code) throws Exception {
    boolean added = false;
    for (String name : propertyGroup.names()) {
      String ref = propertyGroup.item(name).reference();
      if (ref.isEmpty()) {
        ref = name;
      }
      if (!added) {
        added = true;
        code.add("");
      }
      code.add("import %s.%s.%s;", packagePrefix, toPackage(ref), getDtoClass(ref));
    }
    code.add("");
  }

  private void addSemanticAnnotationImport(PropertyGroup propertyGroup, Code code) throws Exception {
    if (defaultMediaType.isSemanticMediaType() && propertyGroup.hasSemantics()) {
      code.add("import %s.%s;", SEMANTIC_ANNOTATION_PACKAGE, SEMANTIC_ANNOTATION);
      code.add("");
    }
  }

  private void addSemanticAnnotation(Property property, String indent, final Code result) {
    if (defaultMediaType.isSemanticMediaType()) {
      String uri = property.uri();
      if (!uri.isEmpty()) {
        result.add("%s@%s(\"%s\")", indent, SEMANTIC_ANNOTATION, Java.toString(uri));
      }
    }
  }

  private void addDtoFields(final boolean hasHyperMediaTypes, PropertyGroup propertyGroup, final Code dto, final Collection<Code> sources)
      throws Exception {
    for (String name : propertyGroup.propertyNames()) {
      Property property = propertyGroup.property(name);
      addSemanticAnnotation(property, "  ", dto);
      dto.add("  public %s %s;", getType(property, hasHyperMediaTypes, sources), name);
    }
  }

  protected String getType(Property property, boolean hasHyperMediaTypes, Collection<Code> sources)
      throws Exception {
    String result = null;
    if (property instanceof PropertyGroup) {
      PropertyGroup propertyGroup = (PropertyGroup)property;
      String ref = propertyGroup.reference();
      if (ref.isEmpty()) {
        result = addDtosFor(propertyGroup, hasHyperMediaTypes, sources);
      } else {
        result = getDtoClass(ref);
      }
    }
    if (result == null) {
      String type = property.type();
      result = type.isEmpty() ? "String" : type;
    }
    if (property.repeats()) {
      return result + "[]";
    }
    return result;
  }

  private void generateSourcesForErrors(RadlCode radl, final Collection<Code> sources) throws Exception {
    Iterator<String> errors = radl.errors().iterator();
    if (!errors.hasNext()) {
      return;
    }
    sources.add(generateErrorDto());
    sources.add(generateIdentifiable());
    final JavaCode errorHandler = startErrorHandler();
    final Collection<String> errorHandlingMethods = new ArrayList<String>();
    do {
      String name = errors.next();
      String statusCode = radl.errorStatus(name);
      if (statusCode.isEmpty()) {
        statusCode = DEFAULT_STATUS_CODE;
      }
      String documentation = radl.errorDocumentation(name);
      JavaCode exceptionType = generateException(name, statusCode, documentation);
      sources.add(exceptionType);
      handleException(exceptionType, statusCode, errorHandlingMethods, errorHandler);
    } while (errors.hasNext());
    sources.add(endErrorHandler(errorHandler));
  }

  private Code generateErrorDto() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public class %s {", ERROR_DTO_TYPE);
    result.add("");
    result.add("  public String title;");
    result.add("  public String type;");
    result.add("");
    result.add("}");
    return result;
  }
  
  private Code generateIdentifiable() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", IDENTIFIABLE_TYPE);
    result.add("");
    result.add("  String getId();");
    result.add("");
    result.add("}");
    return result;
  }

  protected JavaCode generateException(String name, String statusCode, String documentation) {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("import %s;", apiType());
    result.add("");
    result.add("");
    String type = toExceptionTypeName(getErrorName(name));
    result.add("public class %s extends %s implements %s {", type, getBaseException(statusCode), IDENTIFIABLE_TYPE);
    result.add("");
    result.add("  public %s() {", type);
    result.add("    super(\"%s\");", getMessage(name, documentation));
    result.add("  }");
    result.add("");
    result.add("  public String getId() {");
    result.add("    return %s.%s;", API_TYPE, errorConstants.get(name).getName());
    result.add("  }");
    result.add("");
    result.add("}");
    return result;
  }

  private String getErrorName(String name) {
    URI uri;
    try {
      uri = new URI(name);
    } catch (URISyntaxException e) {
      return name;
    }
    if (uri.getScheme() == null || !uri.getScheme().startsWith("http")) {
      return name;
    }
    String path = uri.getPath();
    if (path.endsWith("/")) {
      path = path.substring(0,  path.length() - 1);
    }
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private String toExceptionTypeName(String name) {
    return Java.toIdentifier(name + "Exception");
  }

  private String getMessage(String name, String documentation) {
    if (documentation == null || documentation.trim().isEmpty()) {
      return errorConstants.get(name).getName();
    }
    return Java.toString(documentation.trim());
  }

  private String getBaseException(String statusCode) {
    if ("400".equals(statusCode)) {
      return IllegalArgumentException.class.getSimpleName();
    }
    if ("500".equals(statusCode)) {
      return IllegalStateException.class.getSimpleName();
    }
    return RuntimeException.class.getSimpleName();
  }

  private JavaCode startErrorHandler() {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("import org.springframework.http.HttpStatus;");
    result.add("import org.springframework.http.ResponseEntity;");
    result.add("import org.springframework.web.bind.annotation.ControllerAdvice;");
    result.add("import org.springframework.web.bind.annotation.ExceptionHandler;");
    result.add("");
    result.add("");
    result.add("@ControllerAdvice");
    result.add("public class CentralErrorHandler {");
    result.add("");
    return result;
  }
  
  private void handleException(JavaCode exceptionType, String statusCode, Collection<String> errorHandlingMethods,
      JavaCode errorHandler) {
    if (FRAMEWORK_HANDLED_STATUSES.contains(statusCode)) {
      return;
    }
    String handledType = handledExceptionType(exceptionType);
    String method = exceptionTypeToMethod(handledType);
    if (errorHandlingMethods.contains(method)) {
      return;
    }
    errorHandlingMethods.add(method);
    errorHandler.add("  @ExceptionHandler({ %s.class })", handledType);
    errorHandler.add("  public ResponseEntity<ErrorDto> %s(%s e) {", method, handledType);
    errorHandler.add("    return error(e, HttpStatus.%s);", HTTP_STATUSES.get(statusCode));
    errorHandler.add("  }");
    errorHandler.add("");
  }

  private String handledExceptionType(JavaCode exceptionType) {
    String result = exceptionType.superTypeName();
    if ("RuntimeException".equals(result)) {
      result = exceptionType.typeName();
    }
    return result;
  }

  private String exceptionTypeToMethod(String exceptionType) {
    StringBuilder result = new StringBuilder(exceptionType);
    result.setLength(result.length() - "Exception".length());
    result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
    return result.toString();
  }
  
  private Code endErrorHandler(JavaCode errorHandler) {
    errorHandler.add("  private ResponseEntity<ErrorDto> error(Exception e, HttpStatus statusCode) {");
    errorHandler.add("    ErrorDto error = new ErrorDto();");
    errorHandler.add("    error.type = ((%s)e).getId();", IDENTIFIABLE_TYPE);
    errorHandler.add("    error.title = e.getMessage();");
    errorHandler.add("    return new ResponseEntity<ErrorDto>(error, statusCode);");
    errorHandler.add("  }");
    errorHandler.add("");
    errorHandler.add("}");
    return errorHandler;
  }

  private void generateSourcesForResources(RadlCode radl, final boolean hasHyperMediaTypes,
      final Collection<Code> sources) throws Exception {
    Iterator<String> startTransitions = radl.stateTransitionNames("").iterator();
    String startTransition = startTransitions.hasNext() ? startTransitions.next() : null;
    for (String resource : radl.resourceNames()) {
      sources.add(generateController(radl, resource, hasHyperMediaTypes, startTransition));
      sources.add(generateControllerHelper(radl, hasHyperMediaTypes, resource));
    }
    sources.add(generateApi(radl));
    sources.add(generateUris());
  }

  private Code generateUris() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", URIS_TYPE);
    addUris(result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addUris(Code code) {
    addConstants(filter(uriConstants, CONSTANT_PREFIX_URL + BILLBOARD_URL, false), "Resource locations", code);
  }

  private Code generateApi(RadlCode radl) throws Exception {
    Code result = new JavaCode();
    addPackage(API_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", API_TYPE);
    addBillboardUri(result);
    addMediaTypes(result);
    addLinkRelations(result);
    addErrors(radl, result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addErrors(RadlCode radl, Code code) throws Exception {
    addErrorConstants(radl);
    addConstants(errorConstants, "Error conditions", code);
  }

  private void addErrorConstants(RadlCode radl) throws Exception {
    for (String value : radl.errors()) {
      String documentation = radl.errorDocumentation(value);
      errorConstants.put(value, ensureConstant("ERROR_", getErrorName(value), value, documentation, errorConstants));
    }
  }

  private void addBillboardUri(Code code) {
    addConstants(filter(uriConstants, CONSTANT_PREFIX_URL + BILLBOARD_URL, true), "Entry point", code);
  }

  private Map<String, Constant> filter(Map<String, Constant> values, String value, boolean include) {
    Map<String, Constant> result = new HashMap<String, Constant>();
    for (Entry<String, Constant> entry : values.entrySet()) {
      if (value.equals(entry.getValue().getName()) == include) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private void addLinkRelations(Code code) throws Exception {
    addConstants(linkRelationConstants, "Link relations", code);
  }

  private void addLinkRelationConstants(RadlCode radl)
      throws Exception {
    for (String value : radl.linkRelationNames()) {
      String[] segments = value.split("/");
      String name = segments[segments.length - 1];
      String documentation = radl.linkRelationDocumentation(value);
      ensureConstant("LINK_REL_", name, value, documentation, linkRelationConstants);
    }
  }

  private void addMediaTypes(Code code) {
    addConstants(mediaTypeConstants, "Media types", code);
    if (defaultMediaType != null) {
      code.add("  String %s = \"%s\";", getLocalMediaTypeConstant(defaultMediaType.name()), defaultMediaType.name());
      code.add("  String %s = %s;", DEFAULT_MEDIA_TYPE_CONSTANT, getLocalMediaTypeConstant(defaultMediaType.name()));
    }
  }

  private void addConstants(Map<String, Constant> constants, String heading, Code code) {
    if (!constants.isEmpty()) {
      code.add("");
      code.add("");
      code.add("  // %s", heading);
      code.add("");
      for (Entry<String, Constant> entry : constants.entrySet()) {
        Constant constant = entry.getValue();
        if (constant.getComments().length > 0) {
          code.add("  /**");
          for (String comment : constant.getComments()) {
            code.add("   * %s", comment);
          }
          code.add("   */");
        }
        code.add("  String %s = \"%s\";", constant.getName(), entry.getKey());
      }
    }
  }

  private void addPackage(String name, Code code) {
    code.add("/*");
    for (String line : header.split("\n")) {
      code.add(" * %s", line);
    }
    code.add(" */");
    code.add("package %s.%s;", packagePrefix, toPackage(name));
  }

  private String toPackage(String name) {
    StringBuilder result = new StringBuilder(name);
    int i = 0;
    while (i < result.length()) {
      char c = result.charAt(i);
      if (Character.isUpperCase(c)) {
        result.setCharAt(i, Character.toLowerCase(c));
      } else if (!Character.isJavaIdentifierPart(c)) {
        int j = i + 1;
        while (j < result.length() && !Character.isJavaIdentifierPart(result.charAt(j))) {
          j++;
        }
        result.delete(i, j);
      }
      i++;
    }
    if (result.charAt(result.length() - 1) == 's') {
      result.setLength(result.length() - 1);
    }
    return result.toString();
  }

  private String apiType() {
    return join(packagePrefix, API_PACKAGE, API_TYPE);
  }

  private String join(String... parts) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String part : parts) {
      result.append(prefix).append(part);
      prefix = ".";
    }
    return result.toString();
  }

  private String urisType() {
    return join(packagePrefix, IMPL_PACKAGE, URIS_TYPE);
  }

  private Code generateController(RadlCode radl, String resource, final boolean hasHyperMediaTypes, String startTransition)
      throws Exception {
    final JavaCode result = new JavaCode();
    String name = resource;
    addPackage(name, result);
    result.add("");
    String uri = radl.resourceLocation(resource);
    boolean addUris;
    String namePrefix;
    String constantName;
    String type;
    if (transitionsToStart(radl, startTransition, resource)) {
      namePrefix = CONSTANT_PREFIX_URL;
      constantName = BILLBOARD_URL;
      type = API_TYPE;
      addUris = false;
    } else {
      namePrefix = "";
      constantName = name;
      type = URIS_TYPE;
      addUris = true;
    }
    addControllerImports(radl, resource, addUris, result);
    result.add("@RestController");
    if (uri != null) {
      Constant constant = ensureConstant(namePrefix, constantName, uri, null, uriConstants);
      result.add(String.format("@RequestMapping(%s.%s)", type,  constant.getName()));
    }
    result.add("public class %s {", getControllerClassName(resource));
    result.add("");
    result.add("  @Autowired");
    result.add("  private %s %s;", getControllerHelperClassName(resource), CONTROLLER_HELPER_NAME);
    result.add("");
    for (String method : radl.methodNames(resource)) {
      addControllerMethod(radl, resource, method, hasHyperMediaTypes, result);
    }
    result.add("}");
    return result;
  }

  private boolean transitionsToStart(RadlCode radl, final String startTransition, String resource) throws Exception {
    for (String method : radl.methodNames(resource)) {
      for (String transition : radl.methodTransitions(resource, method)) {
        if (transition.equals(startTransition)) {
          return true;
        }
      }
    }
    return false;
  }

  private void addControllerMethod(RadlCode radl, String resource, String method, boolean hasHyperMediaTypes, JavaCode code) throws Exception {
    String consumes = getConsumes(radl, resource, method);
    String produces = getProduces(radl, resource, method);
    String argName = parameterName(consumes);
    code.add("  @RequestMapping(method = RequestMethod.%s%s%s)", method.toUpperCase(Locale.getDefault()),
        consumes, produces);
    String type = returnType(produces, radl, resource, method);
    addReturnTypeImport(type, code);
    String javaMethod = httpToJavaMethod(method);
    code.add("  public %s %s(%s) {", type, javaMethod, parameters(consumes, radl, resource, method, argName));
    if (NO_TYPE.equals(type)) {
      code.add("    %s.%s(%s);", CONTROLLER_HELPER_NAME, javaMethod, argName);
    } else {
      code.add("    %s result = %s.%s(%s);", type, CONTROLLER_HELPER_NAME, javaMethod, argName);
      if (hasHyperMediaTypes) {
        addLinks(radl, resource, method, code);
      }
      code.add("    return result;");
    }
    code.add("  }");
    code.add("");
  }

  private String httpToJavaMethod(String httpMethod) {
    return httpMethod.toLowerCase(Locale.getDefault());
  }

  private void addLinks(RadlCode radl, String resource, String method, JavaCode code) {
    for (String transition : radl.methodTransitions(resource, method)) {
      for (String state : radl.transitionEnds(transition)) {
        addLinks(radl, state, code);
      }
    }
  }

  private void addLinks(RadlCode radl, String state, JavaCode code) {
    for (String transition : radl.stateTransitionNames(state)) {
      ResourceMethod resourceMethod = radl.transitionMethod(transition);
      String controller = getControllerClassName(resourceMethod.getResource());
      String method = httpToJavaMethod(resourceMethod.getMethod());
      code.ensureImport(packagePrefix + '.' + toPackage(resourceMethod.getResource()), controller);
      code.ensureImport("org.springframework.hateoas.mvc", "ControllerLinkBuilder");
      for (String linkRelation : radl.transitionImplementations(transition)) {
        String linkConstant = API_TYPE + '.' + linkRelationConstants.get(linkRelation).getName();
        code.add("    if (helper.isLinkEnabled(%s)) {", linkConstant);
        code.add("      result.add(ControllerLinkBuilder");
        code.add("        .linkTo(ControllerLinkBuilder.methodOn(%s.class).%s())", controller, method);
        code.add("        .withRel(%s));", linkConstant);
        code.add("    }");
      }
    }
  }

  private void addReturnTypeImport(String type, JavaCode code) {
    if (type.endsWith(DTO_SUFFIX)) {
      String packageName = join(packagePrefix, toPackage(type.substring(0, type.length() - DTO_SUFFIX.length())));
      code.ensureImport(packageName, type);
    }
  }

  private String returnType(String produces, RadlCode radl, String resource, String method) throws Exception {
    final String noType = produces.isEmpty() ? NO_TYPE : UNKNOWN_TYPE;
    String result = noType;
    for (String transition : radl.methodTransitions(resource, method)) {
      String propertyGroup = getOutputPropertyGroup(radl, transition);
      if (propertyGroup.isEmpty()) {
        result = noType;
      } else {
        String dto = getDtoClass(propertyGroup);
        if (noType.equals(result)) {
          result = dto;
        } else if (!result.equals(dto)) {
          result = UNKNOWN_TYPE;
        }
      }
    }
    return result;
  }

  protected String getOutputPropertyGroup(RadlCode radl, String transition) throws Exception {
    String result = "";
    for (String state : radl.transitionEnds(transition)) {
      result = radl.statePropertyGroup(state);
      if (!result.isEmpty()) {
        return result;
      }
    }
    return "";
  }

  private String parameters(String consumes, RadlCode radl, String resource, String method, String argName) {
    return consumes.isEmpty() ? "" : parameterType(consumes, radl, resource, method) + ' ' + argName;
  }

  private String parameterType(String consumes, RadlCode radl, String resource, String method) {
    final String noType = consumes.isEmpty() ? NO_TYPE : UNKNOWN_TYPE;
    String result = noType;
    for (String transition : radl.methodTransitions(resource, method)) {
      String propertyGroup = radl.transitionPropertyGroup(transition);
      if (propertyGroup.isEmpty()) {
        result = noType;
      } else {
        String dto = getDtoClass(propertyGroup);
        if (noType.equals(result)) {
          result = dto;
        } else if (!result.equals(dto)) {
          result = UNKNOWN_TYPE;
        }
      }
    }
    return result;
  }

  private String parameterName(String consumes) {
    return consumes.isEmpty() ? "" : "input";
  }

  private String getMediaTypeConstant(String mediaType) {
    return API_TYPE + '.' + getLocalMediaTypeConstant(mediaType);
  }

  private String getLocalMediaTypeConstant(String mediaType) {
    String name = mediaType.startsWith(DEFAULT_MEDIA_TYPE) ? mediaType.substring(DEFAULT_MEDIA_TYPE.length())
        : mediaType;
    return ensureConstant(MEDIA_TYPE_CONSTANT_PREFIX, name, mediaType, null, mediaTypeConstants).getName();
  }

  private Constant ensureConstant(String namePrefix, String name, String value, String documentation,
      Map<String, Constant> constants) {
    Constant result = constants.get(value);
    if (result == null) {
      String contantName = namePrefix + toJava(name.replace('/', '_').toUpperCase(Locale.getDefault()));
      result = new Constant(contantName, documentation);
      constants.put(value, result);
    }
    return result;
  }

  private String toJava(String value) {
    StringBuilder result = new StringBuilder(value);
    int i = 0;
    boolean nonJava = false;
    while (i < result.length()) {
      if (Character.isJavaIdentifierPart(result.charAt(i))) {
        nonJava = false;
        i++;
      } else {
        if (nonJava) {
          result.delete(i, i + 1);
        } else {
          result.setCharAt(i, '_');
          nonJava = true;
          i++;
        }
      }
    }
    return result.toString();
  }

  private void addControllerImports(RadlCode radl, String resource, boolean addUris, Code controllerClass) throws Exception {
    controllerClass.add("import org.springframework.beans.factory.annotation.Autowired;");
    boolean hasMethod = radl.methodNames(resource).iterator().hasNext();
    if (hasMethod || !radl.resourceLocation(resource).isEmpty()) {
      controllerClass.add("import org.springframework.web.bind.annotation.RequestMapping;");
    }
    if (hasMethod) {
      controllerClass.add("import org.springframework.web.bind.annotation.RequestMethod;");
    }
    controllerClass.add("import org.springframework.web.bind.annotation.RestController;");
    controllerClass.add("");
    controllerClass.add("import %s;", apiType());
    if (addUris) {
      controllerClass.add("import %s;", urisType());
    }
    controllerClass.add("");
    controllerClass.add("");
  }

  private String getControllerClassName(String resource) {
    return getClassName(resource) + "Controller";
  }

  private String getClassName(String name) {
    return toJavaIdentifier(name);
  }

  public String toJavaIdentifier(String text) {
    StringBuilder result = new StringBuilder(text);
    while (!Character.isJavaIdentifierStart(result.charAt(0))) {
      result.delete(0, 1);
    }
    upcase(result, 0);
    int index = 1;
    while (index < result.length()) {
      char ch = result.charAt(index);
      if (Character.isUpperCase(ch)) {
        result.setCharAt(index, Character.toLowerCase(ch));
      } else if (!Character.isJavaIdentifierPart(ch)) {
        while (index < result.length() && !Character.isJavaIdentifierPart(result.charAt(index))) {
          result.delete(index, index + 1);
        }
        if (index < result.length()) {
          upcase(result, index);
        }
      }
      index++;
    }
    return result.toString();
  }

  private static void upcase(StringBuilder builder, int index) {
    builder.setCharAt(index, Character.toUpperCase(builder.charAt(index)));
  }

  private String getConsumes(RadlCode radl, String resource, String method) throws Exception {
    return getMediaTypes(radl.methodRequestRepresentations(resource, method), "consumes");
  }

  private String getMediaTypes(Iterable<String> mediaTypes, String prefix) throws Exception {
    Iterator<String> iterator = mediaTypes.iterator();
    if (!iterator.hasNext()) {
      return "";
    }
    String mediaType = iterator.next();
    if (!iterator.hasNext() && defaultMediaType != null
        && mediaType.equals(defaultMediaType.name())) {
      // Explicit use of default media type
      mediaType = API_TYPE + '.' + DEFAULT_MEDIA_TYPE_CONSTANT;
    } else {
      mediaType = getMediaTypeConstant(mediaType);
    }
    StringBuilder result = new StringBuilder();
    result.append(", ").append(prefix).append(" = { ").append(mediaType);
    while (iterator.hasNext()) {
      result.append(", ").append(getMediaTypeConstant(iterator.next()));
    }
    result.append(" }");
    return result.toString();
  }

  private String getProduces(RadlCode radl, String resource, String method) throws Exception {
    return getMediaTypes(radl.methodResponseRepresentations(resource, method), "produces");
  }

  private Code generateControllerHelper(RadlCode radl, boolean hasHyperMediaTypes, String resource) throws Exception {
    final JavaCode result = new JavaCode();
    addPackage(resource, result);
    result.add("");
    result.add("import org.springframework.stereotype.Service;");
    result.add("");
    result.add("");
    result.add("@Service");
    result.add("public class %s {", getControllerHelperClassName(resource));
    result.add("");
    for (String method : radl.methodNames(resource)) {
      addControllerHelperMethod(radl, resource, method, result);
    }
    if (hasHyperMediaTypes) {
      result.add("  public boolean isLinkEnabled(String linkRelation) {");
      addDummyReturnStatement("return true; ", result);
      result.add("  }");
      result.add("");
    }
    result.add("}");
    return result;
  }

  private String getControllerHelperClassName(String resource) {
    return getControllerClassName(resource) + "Helper";
  }

  private void addControllerHelperMethod(RadlCode radl, String resource, String method, JavaCode code) throws Exception {
    String consumes = getConsumes(radl, resource, method);
    String produces = getProduces(radl, resource, method);
    String argName = parameterName(consumes);
    String args = parameters(consumes, radl, resource, method, argName);
    String type = returnType(produces, radl, resource, method);
    addReturnTypeImport(type, code);
    String returnStatement = type.equals(NO_TYPE) ? "" : "return new " + type + "(); ";
    code.add("  public %s %s(%s) {", type, httpToJavaMethod(method), args);
    addDummyReturnStatement(returnStatement, code);
    code.add("  }");
    code.add("");
  }

  private void addDummyReturnStatement(String returnStatement, JavaCode code) {
    // Make sure the comment is not viewed as a to-do in *this* code base
    code.add("    %s// TO%s: Implement", returnStatement, "DO");
  }


  private static class Constant {

    private final String name;
    private final String comments;

    public Constant(String name, String comments) {
      this.name = name;
      this.comments = comments;
    }

    public String getName() {
      return name;
    }

    public String[] getComments() {
      return comments == null ? new String[0] : comments.split("\n");
    }

  }

}
