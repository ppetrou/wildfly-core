/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.controller;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
public interface AttributeMarshallers {

    static class PropertiesAttributeMarshaller extends AttributeMarshaller {
        private final String wrapperElement;
        private final String elementName;
        private final boolean wrapElement;

        public PropertiesAttributeMarshaller(String wrapperElement, String elementName, boolean wrapElement) {
            this.wrapperElement = wrapperElement;
            this.elementName = elementName == null ? ModelDescriptionConstants.PROPERTY : elementName;
            this.wrapElement = wrapElement;
        }

        public PropertiesAttributeMarshaller(String wrapperElement, boolean wrapElement) {
            this(wrapperElement, null, wrapElement);
        }

        public PropertiesAttributeMarshaller() {
            this(null, null, true);
        }

        @Override
        public boolean isMarshallable(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault) {
            return resourceModel.isDefined() && resourceModel.hasDefined(attribute.getName());
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {

            resourceModel = resourceModel.get(attribute.getName());
            if (!resourceModel.isDefined()) {
                // nothing to do
                return;
            }

            String wrapper =  wrapperElement == null ? attribute.getName() : wrapperElement;
            List<ModelNode> elementList = resourceModel.asList();
            if (elementList.isEmpty()) {
                if (wrapElement) {
                    writer.writeEmptyElement(wrapper);
                } else {
                    // This is a subtle programming error, where the xml schema doesn't
                    // prevent ambiguity between the 'null' and 'empty collection' cases,
                    // or a defined but empty ModelNode isn't valid but there's no
                    // AttributeDefinition validation preventing that being accepted.
                    // TODO We can look into possibly throwing an exception here, but
                    // for now to be conservative and avoid regressions I'm just sticking
                    // with existing behavior and marshalling nothing. I'll log a DEBUG
                    // though in the off chance it's helpful if this happens.
                    ControllerLogger.MGMT_OP_LOGGER.debugf("%s found ambigous empty value for unwrapped property %s",
                            getClass().getSimpleName(), attribute.getName());
                }
                // No elements to marshal, so we're done
                return;
            }

            if (wrapElement) {
                writer.writeStartElement(wrapper);
            }
            for (ModelNode property : elementList) {
                writer.writeEmptyElement(elementName);
                writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), property.asProperty().getName());
                writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName(), property.asProperty().getValue().asString());
            }
            if (wrapElement) {
                writer.writeEndElement();
            }
        }

        @Override
        public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            marshallAsElement(attribute, resourceModel, marshallDefault, writer);
        }

        @Override
        public boolean isMarshallableAsElement() {
            return true;
        }
    }


}
