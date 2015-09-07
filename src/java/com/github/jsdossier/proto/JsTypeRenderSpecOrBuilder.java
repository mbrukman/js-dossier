// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: dossier.proto

package com.github.jsdossier.proto;

public interface JsTypeRenderSpecOrBuilder extends
    // @@protoc_insertion_point(interface_extends:dossier.JsTypeRenderSpec)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>required .dossier.JsType type = 1;</code>
   *
   * <pre>
   * The type to generate documentation for.
   * </pre>
   */
  boolean hasType();
  /**
   * <code>required .dossier.JsType type = 1;</code>
   *
   * <pre>
   * The type to generate documentation for.
   * </pre>
   */
  com.github.jsdossier.proto.JsType getType();
  /**
   * <code>required .dossier.JsType type = 1;</code>
   *
   * <pre>
   * The type to generate documentation for.
   * </pre>
   */
  com.github.jsdossier.proto.JsTypeOrBuilder getTypeOrBuilder();

  /**
   * <code>required .dossier.Resources resources = 2;</code>
   *
   * <pre>
   * The resources to include.
   * </pre>
   */
  boolean hasResources();
  /**
   * <code>required .dossier.Resources resources = 2;</code>
   *
   * <pre>
   * The resources to include.
   * </pre>
   */
  com.github.jsdossier.proto.Resources getResources();
  /**
   * <code>required .dossier.Resources resources = 2;</code>
   *
   * <pre>
   * The resources to include.
   * </pre>
   */
  com.github.jsdossier.proto.ResourcesOrBuilder getResourcesOrBuilder();

  /**
   * <code>required .dossier.Index index = 3;</code>
   *
   * <pre>
   * Navigation index.
   * </pre>
   */
  boolean hasIndex();
  /**
   * <code>required .dossier.Index index = 3;</code>
   *
   * <pre>
   * Navigation index.
   * </pre>
   */
  com.github.jsdossier.proto.Index getIndex();
  /**
   * <code>required .dossier.Index index = 3;</code>
   *
   * <pre>
   * Navigation index.
   * </pre>
   */
  com.github.jsdossier.proto.IndexOrBuilder getIndexOrBuilder();
}
