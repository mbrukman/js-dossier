/*
 Copyright 2013-2016 Jason Leyba

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.github.jsdossier.soy;

import com.google.common.collect.ImmutableSet;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.types.SoyType;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Function for testing if a message has a field that is a chunk of sanitized HTML.
 */
@Singleton
final class IsSanitizedHtmlFunction extends AbstractSoyJavaFunction {

  private final DossierSoyTypeProvider typeProvider;

  @Inject
  IsSanitizedHtmlFunction(DossierSoyTypeProvider typeProvider) {
    this.typeProvider = typeProvider;
  }

  @Override
  public SoyValue computeForJava(List<SoyValue> args) {
    String typeName = getStringArgument(args, 0);
    String fieldName = getStringArgument(args, 1);

    SoyType type = typeProvider.getType(typeName, null);
    if (type instanceof ProtoMessageSoyType) {
      ProtoMessageSoyType messageType = (ProtoMessageSoyType) type;
      return BooleanData.forValue(messageType.isSanitizedHtml(fieldName));
    }
    return BooleanData.FALSE;
  }

  @Override
  public Set<Integer> getValidArgsSizes() {
    return ImmutableSet.of(2);
  }
}
