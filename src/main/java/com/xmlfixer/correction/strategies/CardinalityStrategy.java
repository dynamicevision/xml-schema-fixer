package com.xmlfixer.correction.strategies;

import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ErrorType;
import com.xmlfixer.validation.model.ValidationError;

import java.util.List;
import java.util.Map;

public class CardinalityStrategy implements CorrectionStrategy {
    @Override
    public String getStrategyName() {
        return "";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public List<ErrorType> getSupportedErrorTypes() {
        return List.of();
    }

    @Override
    public List<CorrectionAction> generateCorrections(Map<ErrorType, List<ValidationError>> errorsByType, SchemaElement schema) {
        return List.of();
    }
}
