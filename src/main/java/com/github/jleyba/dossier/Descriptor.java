// Copyright 2013 Jason Leyba
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.github.jleyba.dossier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.FunctionType;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.ObjectType;
import com.google.javascript.rhino.jstype.UnionType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

public class Descriptor {

  private final String name;
  @Nullable private final Node srcNode;
  private final JSType type;
  @Nullable private final JsDoc info;

  private ImmutableList<ArgDescriptor> args;
  private ImmutableList<Descriptor> properties;
  private ImmutableSet<Descriptor> instanceProperties;

  private Optional<Descriptor> parent;
  private Optional<ModuleDescriptor> module = Optional.absent();

  Descriptor(String name, @Nullable Node srcNode, JSType type, @Nullable JSDocInfo info) {
    this.name = name;
    this.srcNode = srcNode;
    this.type = checkNotNull(type, "null type: %s", name);
    this.info = getJsDoc(info, type);
    this.parent = Optional.absent();
  }

  /**
   * Creates a new descriptor for an instance property.
   *
   * @param parent the descriptor for the object this property is defined on.
   * @param name the fully qualified name of this property.
   * @param srcNode node in the AST where the property was defined.
   * @param type this property's type.
   * @param info this property's JSDoc info.
   * @throws IllegalArgumentException if {@code parent} is not a constructor or interface
   *     descriptor.
   */
  Descriptor(Descriptor parent, String name, @Nullable Node srcNode, JSType type,
      @Nullable JSDocInfo info) {
    checkArgument(null != parent && (parent.isConstructor() || parent.isInterface()));
    this.name = name;
    this.srcNode = srcNode;
    this.type = checkNotNull(type, "null type: %s", name);
    this.info = getJsDoc(info, type);
    this.parent = Optional.of(parent);
  }

  /**
   * Returns whether the given {@code type} is a reference to the "Object" type.
   */
  public static boolean isTheObjectType(JSType type) {
    if (!type.isInstanceType()) {
      return false;
    }
    ObjectType obj = type.toObjectType();
    return obj.getConstructor().isNativeObjectType()
        && "Object".equals(obj.getConstructor().getReferenceName());
  }

  /**
   * Returns whether the given {@code type} is a reference to the "Function" type.
   */
  public static boolean isTheFunctionType(JSType type) {
    if (!type.isInstanceType()) {
      return false;
    }
    ObjectType obj = type.toObjectType();
    return obj.getConstructor().isNativeObjectType()
        && "Function".equals(obj.getConstructor().getReferenceName());
  }

  private static JsDoc getJsDoc(@Nullable JSDocInfo info, JSType type) {
    if (info == null && !isTheObjectType(type)) {
      // TODO: we should only do this for constructors, interfaces, and enums.
      // TODO: must preserve original source location.
      info = type.getJSDocInfo();
    }
    return info == null ? null : new JsDoc(info);
  }

  public static ImmutableList<Descriptor> sortByName(Iterable<Descriptor> descriptors) {
    return Ordering.from(new Comparator<Descriptor>() {
      @Override
      public int compare(Descriptor a, Descriptor b) {
        return a.getFullName().compareTo(b.getFullName());
      }
    }).immutableSortedCopy(descriptors);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Descriptor) {
      Descriptor that = (Descriptor) o;
      return this.name.equals(that.name)
          && Objects.equals(this.type, that.type);
    }
    return false;
  }

  public Optional<Descriptor> getParent() {
    return parent;
  }

  Optional<ModuleDescriptor> getModule() {
    return module;
  }

  void setModule(ModuleDescriptor module) {
    checkState(!this.module.isPresent());
    this.module = Optional.of(module);
  }

  /**
   * Returns the described type's "simple" name - that is, the last segment of its dot-separated
   * fully qualified name.
   */
  String getSimpleName() {
    int index = name.lastIndexOf('.');
    if (index != -1) {
      return name.substring(index + 1);
    }
    return name;
  }

  /**
   * Returns the described type's fully qualified name (using dot-notation).
   */
  String getFullName() {
    return name;
  }

  JSType getType() {
    return type;
  }

  @Nullable JsDoc getJsDoc() {
    return info;
  }

  @Nullable String getSource() {
    if (srcNode != null) {
      return srcNode.getSourceFileName();
    }
    if (info != null) {
      return info.getSource();
    }
    return null;
  }

  /**
   * Returns the line number this instance's described type is defined on in its source file;
   * will trivially return 0 if this line number cannot be determined.
   */
  int getLineNum() {
    if (srcNode != null) {
      return srcNode.getLineno();
    }
    if (info != null) {
      return info.getLineNum();
    }
    return 0;
  }

  ObjectType toObjectType() {
    checkState(isObject());
    ObjectType obj = ObjectType.cast(type);
    if (obj == null && type != null && type.isUnionType()) {
      for (JSType t : ((UnionType) type).getAlternates()) {
        obj = ObjectType.cast(t);
        if (obj != null) {
          break;
        }
      }
    }
    checkState(obj != null);
    return obj;
  }

  boolean isObject() {
    return type != null && type.isObject();
  }

  boolean isFunction() {
    return type != null && type.isFunctionType();
  }

  boolean isConstructor() {
    return info != null && info.isConstructor()
        || ((info == null || !info.isTypedef())
        && !isInterface()
        && !isTheFunctionType(type)
        && type != null && type.isConstructor());
  }

  boolean isInterface() {
    if (info != null) {
      return info.isInterface();
    }
    return type != null && type.isInterface();
  }

  boolean isEnum() {
    return (type != null && type.isEnumType())
        || (info != null && info.isEnum());
  }

  boolean isEmptyNamespace() {
    return !isConstructor()
        && !isInterface()
        && !isEnum()
        && getProperties().isEmpty();
  }

  boolean isDeprecated() {
    return (info != null && info.isDeprecated());
  }

  boolean isFinal() {
    return (info != null && info.isFinal());
  }

  boolean isDict() {
    return (info != null && info.isDict());
  }

  boolean isStruct() {
    return (info != null && info.isStruct());
  }

  boolean isConst() {
    return (info != null && info.isConst());
  }

  boolean isCompilerConstant() {
    return (info != null && info.isDefine());
  }

  boolean isTypedef() {
    return (info != null && info.isTypedef());
  }

  boolean isModuleExports() {
    return module.isPresent() && module.get().getDescriptor() == this;
  }

  JSDocInfo.Visibility getVisibility() {
    if (info != null) {
      return info.getVisibility();
    }
    return JSDocInfo.Visibility.PUBLIC;
  }

  @Nullable
  String getDeprecationReason() {
    checkState(isDeprecated(), "%s is not deprecated", getFullName());
    assert info != null;
    return info.getDeprecationReason();
  }

  /**
   * Returns all of the super types for this descriptor as a stack with this descriptor at the
   * bottom and the root ancestor at the top (Object is excluded as it is implied).
   *
   * <p>The returned stack will be empty if this descriptor is not for a class.
   */
  LinkedList<JSType> getAllTypes(DocRegistry registry) {
    LinkedList<JSType> stack = new LinkedList<>();
    if (!isConstructor()) {
      return stack;
    }
    for (JSType type = this.type; type != null; type = getBaseType(type, registry)) {
      stack.push(normalizeType(type));
    }
    return stack;
  }

  /**
   * Returns all of the interfaces that this type implements. The returned set will be empty if
   * this descriptor is not for a class.
   */
  Set<JSType> getImplementedInterfaces(DocRegistry registry) {
    Set<JSType> interfaces = new HashSet<>();
    if (!isConstructor()) {
      return interfaces;
    }

    LinkedList<JSType> allTypes = getAllTypes(registry);
    while (!allTypes.isEmpty()) {
      JSType type = allTypes.pop();
      if (type != null && type.getJSDocInfo() != null) {
        JSDocInfo info = type.getJSDocInfo();
        for (JSTypeExpression expr : info.getImplementedInterfaces()) {
          interfaces.add(registry.evaluate(expr));
        }
      }
    }

    return interfaces;
  }

  /**
   * Returns the interfaces extended by this type. If this descriptor is not for an interface,
   * the returned set will be empty.
   */
  Set<JSType> getExtendedInterfaces(DocRegistry registry) {
    if (!isInterface()) {
      return new HashSet<>();
    }
    return getExtendedInterfaces(info, registry);
  }

  /**
   * Returns the names of types this object can be assigned.  That is, if {@code x} is an
   * an instance of class {@code X}, which extends {@code Y} and implements {@code Z},
   * {@code x} may be assigned to {@code X}, {@code Y}, or {@code Z}.
   */
  List<JSType> getAssignableTypes(DocRegistry registry) {
    if (isConstructor()) {
      return getAllTypes(registry);
    } else if (isInterface()) {
      List<JSType> types = Lists.newLinkedList();
      types.add(normalizeType(type));
      types.addAll(getExtendedInterfaces(registry));
      return types;
    } else {
      return ImmutableList.of();
    }
  }

  /**
   * Returns the argument descriptors for this instance.
   *
   * @throws IllegalStateException If this instance does not describe a function.
   */
  List<ArgDescriptor> getArgs() {
    checkState(isConstructor() || isInterface() || isFunction(),
        "%s is not a function!", getFullName());

    if (null != args) {
      return args;
    }

    ObjectType obj = toObjectType();
    if (!(obj instanceof FunctionType)) {
      obj = checkNotNull(obj.getConstructor());
    }

    Node source = ((FunctionType) obj).getSource();
    if (source == null) {
      // If we don't have access to the function node, assume the JSDocInfo has the correct info.
      args = info != null ? info.getParameters() : ImmutableList.<ArgDescriptor>of();
    } else {
      args = ImmutableList.copyOf(getArgs(source, info));
    }

    return args;
  }

  private static List<ArgDescriptor> getArgs(Node source, @Nullable final JsDoc info) {
    // JSDocInfo does not guarantee parameter names will be returned in the order declared,
    // so we have to parse the function declaration.
    Node paramList = source  // function node
        .getFirstChild()     // name node
        .getNext();          // param list node
    checkState(paramList.isParamList());
    List<String> names = Lists.newArrayList();
    for (Node paramName = paramList.getFirstChild(); paramName != null; paramName = paramName.getNext()) {
      names.add(paramName.getString());
    }
    return Lists.transform(names, new Function<String, ArgDescriptor>() {
      @Override
      public ArgDescriptor apply(String name) {
        if (info != null) {
          try {
            return info.getParameter(name);
          } catch (IllegalArgumentException ignored) {
            // Do nothing; undocumented parameter.
          }
        }
        // Undocumented parameter.
        return new ArgDescriptor(name, null, "");
      }
    });
  }

  /**
   * Returns the list of (static) properties defined on this type.
   */
  ImmutableList<Descriptor> getProperties() {
    if (null != properties) {
      return properties;
    }

    if (!isObject()) {
      return this.properties = ImmutableList.of();
    }

    List<Descriptor> properties = new LinkedList<>();
    ObjectType obj = toObjectType();
    if (isConstructor() && obj.getConstructor() != null) {
      obj = obj.getConstructor();
    }

    for (String prop : obj.getOwnPropertyNames()) {
      if ("prototype".equals(prop)) {
        continue;  // Collected separately.
      }

      // Sometimes the JSCompiler picks up the builtin call and apply functions off of a
      // function object.  We should always skip these.
      if (type != null && type.isFunctionType()
          && ("apply".equals(prop) || "bind".equals(prop) || "call".equals(prop))) {
        continue;
      }

      JSDocInfo info = obj.getOwnPropertyJSDocInfo(prop);
      Node srcNode  = obj.getPropertyNode(prop);
      Descriptor property = new Descriptor(
          getFullName() + "." + prop, srcNode, obj.getPropertyType(prop), info);
      property.module = module;
      property.parent = Optional.of(this);

      properties.add(property);
    }

    this.properties = ImmutableList.copyOf(properties);
    return this.properties;
  }

  /**
   * Returns the instance properties defined on this type.
   */
  ImmutableSet<Descriptor> getInstanceProperties() {
    if (null == instanceProperties) {
      ImmutableSet.Builder<Descriptor> properties = ImmutableSet.builder();
      if (!isConstructor() && !isInterface()) {
        instanceProperties = properties.build();
        return instanceProperties;
      }

      ObjectType obj = toObjectType();
      if ((isConstructor() || isInterface()) && obj.getConstructor() != null) {
        obj = obj.getConstructor();
      }

      ObjectType instance = ((FunctionType) obj).getInstanceType();
      properties.addAll(getInstanceProperties(this, instance));

      ObjectType proto = ((FunctionType) obj).getPrototype();
      properties.addAll(getInstanceProperties(this, proto));

      instanceProperties = properties.build();
    }
    return instanceProperties;
  }

  private static List<Descriptor> getInstanceProperties(Descriptor parent, ObjectType obj) {
    List<Descriptor> properties = new LinkedList<>();
    for (String prop : obj.getOwnPropertyNames()) {
      if ("constructor".equals(prop) || "prototype".equals(prop)) {
        continue;
      }

      Node node = obj.getPropertyNode(prop);
      if (null == node) {
        continue;
      }

      JSDocInfo info = node.getJSDocInfo();
      if (null == info && null != node.getParent() && node.getParent().isAssign()) {
        info = node.getParent().getJSDocInfo();
      }

      String name = parent.getFullName() + ".prototype." + prop;
      properties.add(new Descriptor(
          parent, name, obj.getPropertyNode(prop), obj.getPropertyType(prop), info));
    }
    return properties;
  }

  boolean hasOwnInstanceProprety(String name) {
    if (!isConstructor() && !isInterface()) {
      return false;
    }
    ObjectType obj = toObjectType();
    if (isConstructor() && obj.getConstructor() != null) {
      obj = obj.getConstructor();
    }
    if (obj instanceof FunctionType) {
      FunctionType ctor = (FunctionType) obj;
      return ctor.getInstanceType().hasOwnProperty(name)
          || ctor.getPrototype().hasOwnProperty(name);
    }
    return false;
  }

  private static Set<JSType> getExtendedInterfaces(JsDoc info, DocRegistry registry) {
    Set<JSType> interfaces = new HashSet<>();
    for (JSTypeExpression expression : info.getExtendedInterfaces()) {
      JSType type = registry.evaluate(expression);
      if (interfaces.add(normalizeType(type))) {
        JSDocInfo docInfo = type.getJSDocInfo();
        if (docInfo != null) {
          interfaces.addAll(getExtendedInterfaces(new JsDoc(docInfo), registry));
        }
      }
    }

    return interfaces;
  }

  /**
   * Normalizes constructor type references to reference the constructed type, not the
   * constructor <i>function</i>.
   */
  private static JSType normalizeType(JSType type) {
    if (type.isConstructor()) {
      return type.toObjectType().getTypeOfThis();
    }
    return type;
  }

  @Nullable
  private static JSType getBaseType(JSType type, DocRegistry registry) {
    JSDocInfo info = type.getJSDocInfo();
    if (info == null) {
      return null;
    }

    JSTypeExpression baseType = info.getBaseType();
    if (baseType == null) {
      return null;
    }

    return registry.evaluate(baseType);
  }
}
