package com.qcadoo.mes.plugins.products.validation;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.qcadoo.mes.core.data.beans.Entity;
import com.qcadoo.mes.core.data.definition.FieldDefinition;
import com.qcadoo.mes.core.data.definition.FieldType;
import com.qcadoo.mes.core.data.definition.FieldTypeFactory;
import com.qcadoo.mes.core.data.internal.FieldTypeFactoryImpl;

public class ValidationUtilsRequiredFields {

    @Test
    public void shouldValidateWhenAllRequiredFieldsAreFilled() {
        // given
        FieldTypeFactory fieldTypeFactory = new FieldTypeFactoryImpl();
        Entity entity = new Entity();
        List<FieldDefinition> fields = new LinkedList<FieldDefinition>();
        fields.add(createFieldDefinition("testField1", fieldTypeFactory.integerType(), true));
        fields.add(createFieldDefinition("testField2", fieldTypeFactory.integerType(), true));
        fields.add(createFieldDefinition("testField3", fieldTypeFactory.integerType(), false));

        entity.setField("testField1", 1);
        entity.setField("testField2", 2);
        entity.setField("testField3", 3);

        // when
        ValidationResult result = ValidationUtils.validateRequiredFields(entity, fields);

        // then
        assertEquals(true, result.isValid());
    }

    private FieldDefinition createFieldDefinition(final String name, final FieldType type, final boolean required) {
        FieldDefinition fieldDefinition = new FieldDefinition(name);
        fieldDefinition.setType(type);
        fieldDefinition.setRequired(required);
        return fieldDefinition;
    }
}
