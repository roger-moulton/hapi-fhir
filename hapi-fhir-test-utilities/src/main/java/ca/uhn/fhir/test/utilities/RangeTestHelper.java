package ca.uhn.fhir.test.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RangeTestHelper {

	public static final double THOUSANDTH = .001d;


	public static void checkInRange(double base, double value) {
		checkInRange(base, THOUSANDTH, value);
	}

	public static void checkInRange(double theBase, double theRange, double theValue) {
		double lowerBound = theBase - theRange;
		double upperBound = theBase + theRange;
		checkWithinBounds(lowerBound, upperBound, theValue);
	}

	public static void checkInRange(String theBase, String theValue) {
		// ease tests
		if (theBase == null && theValue == null) {
			return;
		}

		double value = Double.parseDouble(theValue);
		double base = Double.parseDouble(theBase);
		checkInRange(base, THOUSANDTH, value);
	}

	public static void checkInRange(String theBase, double theRange, String theValue) {
		// ease tests
		if (theBase == null && theValue == null) {
			return;
		}

		double value = Double.parseDouble(theValue);
		double base = Double.parseDouble(theBase);
		checkInRange(base, theRange, value);
	}

	public static void checkWithinBounds(double theLowerBound, double theUpperBound, double theValue) {
		assertThat(theValue, is(both(greaterThanOrEqualTo(theLowerBound)).and(lessThanOrEqualTo(theUpperBound))));
	}

	public static void checkWithinBounds(String theLowerBound, String theUpperBound, String theValue) {
		assertNotNull(theLowerBound, "theLowerBound");
		assertNotNull(theUpperBound, "theUpperBound");
		assertNotNull(theValue, "theValue");
		double lowerBound = Double.parseDouble(theLowerBound);
		double upperBound = Double.parseDouble(theUpperBound);
		double value = Double.parseDouble(theValue);
		checkWithinBounds(lowerBound, upperBound, value);
	}


}
