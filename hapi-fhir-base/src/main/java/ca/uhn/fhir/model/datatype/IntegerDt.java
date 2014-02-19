package ca.uhn.fhir.model.datatype;

import ca.uhn.fhir.model.api.BasePrimitiveDatatype;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.parser.DataFormatException;

@DatatypeDef(name="integer")
public class IntegerDt extends BasePrimitiveDatatype<Integer> {

	private Integer myValue;

	public Integer getValue() {
		return myValue;
	}

	public void setValue(Integer theValue) {
		myValue = theValue;
	}

	@Override
	public void setValueAsString(String theValue) throws DataFormatException {
		if (theValue == null) {
			myValue = null;
		}else {
			myValue = Integer.parseInt(theValue);
		}
	}

	@Override
	public String getValueAsString() {
		if (myValue==null) {
			return null;
		}
		return Integer.toString(myValue);
	}
	
}
